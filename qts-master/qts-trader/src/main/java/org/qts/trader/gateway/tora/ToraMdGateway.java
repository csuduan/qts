package org.qts.trader.gateway.tora;

import com.tora.xmdapi.*;
import lombok.extern.slf4j.Slf4j;
import org.qts.common.disruptor.FastQueue;
import org.qts.common.entity.MdInfo;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.utils.SpringUtils;
import org.qts.trader.gateway.MdGateway;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class ToraMdGateway extends CTORATstpXMdSpi implements MdGateway {
    private static String basePath;
    static {
        try {
            //System.loadLibrary("thostmduserapi_wrap");
            basePath = SpringUtils.getContext().getEnvironment().getProperty("base.path");
            if(!StringUtils.hasLength(basePath))
                throw new RuntimeException("can not find apiPath");
            //System.loadLibrary("ctp/thosttraderapi_wrap");
            System.load(basePath+"/lib/tora/libjavaxmdapi.so");
        } catch (Exception e) {
            log.error("加载库失败!", e);
        }
    }

    private AcctDetail acct;
    private boolean isRunning = false;
    private boolean isConnected = false;
    private String tradingDay;
    private String mdName;
    private MdInfo mdInfo;
    protected FastQueue fastQueue;

    private CTORATstpXMdApi mdApi = null;
    private HashMap<String, Integer> preTickVolumeMap = new HashMap<>();
    private ScheduledExecutorService scheduler;


    int m_request_id;

    public ToraMdGateway(AcctDetail acctInfo) {
        this.acct = acctInfo;
        this.scheduler = acctInfo.getScheduler();
        this.fastQueue = acctInfo.getFastQueue();
        this.mdInfo = new MdInfo(acctInfo.getConf());
        this.mdName = mdInfo.getId();
        log.info("md init ...");
        this.connect();
    }

    private void setConnected(boolean connected) {
        this.isConnected = connected;
        this.acct.setMdStatus( connected);
        log.info("set isConnected to : {}",connected);
    }

    @Override
    public void subscribe(List<String> symbols) {
        if(CollectionUtils.isEmpty(symbols))
            return;
        if(!isConnected()){
            log.warn(mdName + "无法订阅行情,行情服务器尚未连接成功");
            return;
        }

        int ret = mdApi.SubscribeMarketData(symbols.toArray(new String[0]), xmdapi.getTORA_TSTP_EXD_SZSE());
        if(ret!=0){
            log.error("SubscribeMarketData fail ,ret={}",ret);
        }
    }

    @Override
    public synchronized void connect() {
        if (this.isRunning) {
            return;
        }

        scheduler.submit(() -> {
            try {
                log.info("Md start connect...");
                this.connectAsync();
                //10s后若连接失败，则自动关闭
                Thread.sleep(10000);
                if (!this.isConnected) {
                    log.error("md connect timeout ...");
                    this.close();
                }
            } catch (Exception ex) {
                log.error("md connect error", ex);
            }
        });
    }

    private void connectAsync() {
        Thread thread = new Thread(() -> {
            isRunning = true;
            log.warn("md thread start ...");
            mdApi = CTORATstpXMdApi.CreateTstpXMdApi();
            mdApi.RegisterSpi(this);
            mdApi.RegisterFront(mdInfo.getMdAddress());
            mdApi.RegisterDeriveServer("tcp://10.100.69.2:7401");
            mdApi.Init();
            mdApi.Join();
            log.info("md thread exit...");

        });
        thread.setName("md");
        thread.start();
    }

    @Override
    public void close() {
        if (!this.isRunning)
            return;
        new Thread(() -> {
            if (mdApi != null) {
                try {
                    mdApi.RegisterSpi(null);
                    this.mdApi.Release();
                } catch (Exception e) {
                    log.error("md release error！", e);
                }
                mdApi = null;
                this.setConnected(false);
                log.warn("md released");
            }
            isRunning = false;
        }).start();
    }

    @Override
    public boolean isConnected() {
        return this.isRunning && this.isConnected;
    }


    public void OnFrontConnected()
    {
        log.info("OnFrontConnected");
        CTORATstpReqUserLoginField req_user_login_field = new CTORATstpReqUserLoginField();
        int ret = mdApi.ReqUserLogin(req_user_login_field, ++m_request_id);
        if (ret != 0)
        {
            log.error("ReqUserLogin fail, ret:{}",ret);
        }
    }

    public void OnFrontDisconnected(int nReason)
    {
        log.error("OnFrontDisconnected, reason[{}]",nReason);
        this.setConnected(false);
    }

    public void OnRspUserLogin(CTORATstpRspUserLoginField pRspUserLoginField, CTORATstpRspInfoField pRspInfo, int nRequestID)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            this.setConnected(true);
            log.info("OnRspUserLogin");
            //重新订阅行情
            this.subscribe(this.acct.getSubList().stream().toList());
        }
        else
        {
            log.error("OnRspUserLogin fail, error_id[{}], error_msg[{}]", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspSubMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() != 0)
        {
            log.error("OnRspSubMarketData fail, error_id[{}] error_msg[{}]", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspUnSubMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
    {
    }

    public void OnRspSubSimplifyMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() != 0)
        {
            log.error("OnRspSubSimplifyMarketData fail, error_id[{}] error_msg[{}]", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }


    public void OnRspSubSecurityStatus(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspSubSecurityStatus success!\n");
        }
        else
        {
            System.out.printf("OnRspSubSecurityStatus fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspSubMarketStatus(CTORATstpSpecificMarketField pSpecificMarketField, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspSubMarketStatus success!\n");
        }
        else
        {
            System.out.printf("OnRspSubMarketStatus fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspUnSubMarketStatus(CTORATstpSpecificMarketField pSpecificMarketField, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspUnSubMarketStatus success!\n");
        }
        else
        {
            System.out.printf("OnRspUnSubMarketStatus fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspUnSubSecurityStatus(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspUnSubSecurityStatus success!\n");
        }
        else
        {
            System.out.printf("OnRspUnSubSecurityStatus fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspInquiryMarketDataMirror(CTORATstpMarketDataField pMarketDataField, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspInquiryMarketDataMirror success!\n");
        }
        else
        {
            System.out.printf("OnRspInquiryMarketDataMirror fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspInquiryPHMarketDataMirror(CTORATstpPHMarketDataField pPHMarketDataField, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspInquiryPHMarketDataMirror success!\n");
        }
        else
        {
            System.out.printf("OnRspInquiryPHMarketDataMirror fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspInquirySpecialMarketDataMirror(CTORATstpSpecialMarketDataField pMarketDataField, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspInquirySpecialMarketDataMirror success!\n");
        }
        else
        {
            System.out.printf("OnRspInquirySpecialMarketDataMirror fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspSubPHMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspSubPHMarketData success!\n");
        }
        else
        {
            System.out.printf("OnRspSubPHMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspSubSpecialMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspSubSpecialMarketData success!\n");
        }
        else
        {
            System.out.printf("OnRspSubSpecialMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspUnSubPHMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspUnSubPHMarketData success!\n");
        }
        else
        {
            System.out.printf("OnRspUnSubPHMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspUnSubSpecialMarketData(CTORATstpSpecificSecurityField pSpecificSecurityField, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspUnSubSpecialMarketData success!\n");
        }
        else
        {
            System.out.printf("OnRspUnSubSpecialMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }


    public void OnRspSubRapidMarketData(CTORATstpSpecificSecurityField pSpecificSecurity, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspSubRapidMarketData success!\n");
        }
        else
        {
            System.out.printf("OnRspSubRapidMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspUnSubRapidMarketData(CTORATstpSpecificSecurityField pSpecificSecurity, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspUnSubRapidMarketData success!\n");
        }
        else
        {
            System.out.printf("OnRspUnSubRapidMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspSubFundsFlowMarketData(CTORATstpSpecificSecurityField pSpecificSecurity, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspSubFundsFlowMarketData success!\n");
        }
        else
        {
            System.out.printf("OnRspSubFundsFlowMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspUnSubFundsFlowMarketData(CTORATstpSpecificSecurityField pSpecificSecurity, CTORATstpRspInfoField pRspInfo)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspUnSubFundsFlowMarketData success!\n");
        }
        else
        {
            System.out.printf("OnRspUnSubFundsFlowMarketData fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    public void OnRspUserLogout(CTORATstpUserLogoutField pUserLogoutField, CTORATstpRspInfoField pRspInfo, int nRequestID)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspUserLogout success!\n");
        }
        else
        {
            System.out.printf("OnRspUserLogout fail, error_id[%d] error_msg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }


    public void OnRtnMarketData(CTORATstpMarketDataField pMarketDataField)
    {
        log.info("OnRtnMarketData: ExchangeID[{}] SecurityID[{}] SecurityName[{}] UpperLimitPrice[{}] LowerLimitPrice[{}] LastPrice[{}] AskPrice1[{}] AskVolume1[{}] BidPrice1[{}] BidVolume1[{}] UpdateTime[{}]",
                pMarketDataField.getExchangeID(), pMarketDataField.getSecurityID(), pMarketDataField.getSecurityName(),
                pMarketDataField.getUpperLimitPrice(), pMarketDataField.getLowerLimitPrice(), pMarketDataField.getLastPrice(),
                pMarketDataField.getAskPrice1(), pMarketDataField.getAskVolume1(),
                pMarketDataField.getBidPrice1(), pMarketDataField.getBidVolume1(),
                pMarketDataField.getUpdateTime());
    }

    public void OnRtnSimplifyMarketData(CTORATstpSimplifyMarketDataField pSimplifyMarketDataField)
    {
        log.info("OnRtnSimplifyMarketData: ExchangeID[{}] SecurityID[{}] SecurityName[{}] UpperLimitPrice[{}] LowerLimitPrice[{}] LastPrice[{}] AskPrice1[{}] BidPrice1[{}] UpdateTime[{}]",
                pSimplifyMarketDataField.getExchangeID(), pSimplifyMarketDataField.getSecurityID(), pSimplifyMarketDataField.getSecurityName(),
                pSimplifyMarketDataField.getUpperLimitPrice(), pSimplifyMarketDataField.getLowerLimitPrice(), pSimplifyMarketDataField.getLastPrice(),
                pSimplifyMarketDataField.getAskPrice1(),
                pSimplifyMarketDataField.getBidPrice1(),
                pSimplifyMarketDataField.getUpdateTime());
    }

    public void OnRtnSecurityStatus(CTORATstpSecurityStatusField pSecurityStatusField)
    {
        System.out.printf("OnRtnSecurityStatus: ExchangeID[%c] SecurityID[%s] IsSupportMarginBuy[%d] IsSupportShortSell[%d]\n",
                pSecurityStatusField.getExchangeID(), pSecurityStatusField.getSecurityID(), pSecurityStatusField.getIsSupportMarginBuy(),
                pSecurityStatusField.getIsSupportShortSell());
    }

    public void OnRtnMarketStatus(CTORATstpMarketStatusField pMarketStatusField)
    {
        System.out.printf("OnRtnMarketStatus: MarketID[%c] MarketStatus[%c]\n",
                pMarketStatusField.getMarketID(), pMarketStatusField.getMarketStatus());
    }

    public void OnRtnPHMarketData(CTORATstpPHMarketDataField pPHMarketDataField)
    {
        System.out.printf("OnRtnPHMarketData: ExchangeID[%c] SecurityID[%s] SecurityName[%s] UpperLimitPrice[%.3f] UpdateTime[%s]\n",
                pPHMarketDataField.getExchangeID(), pPHMarketDataField.getSecurityID(), pPHMarketDataField.getSecurityName(),
                pPHMarketDataField.getUpperLimitPrice(),
                pPHMarketDataField.getUpdateTime());
    }

    public void OnRtnSpecialMarketData(CTORATstpSpecialMarketDataField pSpecialMarketDataField)
    {
        System.out.printf("OnRtnSpecialMarketData: ExchangeID[%c] SecurityID[%s] SecurityName[%s] MovingAvgPrice[%.3f] UpdateTime[%s]\n",
                pSpecialMarketDataField.getExchangeID(), pSpecialMarketDataField.getSecurityID(), pSpecialMarketDataField.getSecurityName(),
                pSpecialMarketDataField.getMovingAvgPrice(),
                pSpecialMarketDataField.getUpdateTime());
    }

    public void OnRtnRapidMarketData(CTORATstpRapidMarketDataField pRapidMarketDataField)
    {
        System.out.printf("OnRtnRapidMarketData: [ExchangeID:%c][SecurityID:%s][PreClosePrice:$%.3f|OpenPrice:%.3f][NumTrades:$%d|TotalVolumeTrade:%d][TotalValueTrade:%.3f][HighestPrice:%.3f][LowestPrice:%.3f][LastPrice:%.3f][UpperLimitPrice:%.3f][LowerLimitPrice:%.3f]\n",
                pRapidMarketDataField.getExchangeID(),
                pRapidMarketDataField.getSecurityID(),
                pRapidMarketDataField.getPreClosePrice(),
                pRapidMarketDataField.getOpenPrice(),
                pRapidMarketDataField.getNumTrades(),
                pRapidMarketDataField.getTotalVolumeTrade(),
                pRapidMarketDataField.getTotalValueTrade(),
                pRapidMarketDataField.getHighestPrice(),
                pRapidMarketDataField.getLowestPrice(),
                pRapidMarketDataField.getLastPrice(),
                pRapidMarketDataField.getUpperLimitPrice(),
                pRapidMarketDataField.getLowerLimitPrice());
    }
}
