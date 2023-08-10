package org.qts.trader.gateway.ctp;


import ctp.thostmduserapi.*;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.qts.common.disruptor.FastEventService;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.entity.MdInfo;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.trade.Tick;
import org.qts.trader.gateway.MdGateway;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Slf4j
public class CtpMdGateway extends CThostFtdcMdSpi  implements MdGateway {
    static {
        try {
            System.loadLibrary("thostmduserapi_wrap");
            log.info("加载库文件成功!");
        } catch (Exception e) {
            log.error("加载库失败!", e);
        }
    }
    private boolean status = false;
    private String tradingDay;
    private String mdName;
    private MdInfo mdInfo;
    protected FastEventService fastEventService;

    private CThostFtdcMdApi mdApi = null;
    private HashSet<String> subscribedSymbols = new HashSet<>();
    private HashMap<String,Integer> preTickVolumeMap = new HashMap<>();


    public CtpMdGateway(FastEventService fastEventEngineService, AcctInfo acctInfo) {
        this.fastEventService = fastEventEngineService;
        this.mdInfo = new MdInfo(acctInfo.getAcctConf());
        this.mdName=mdInfo.getId();
        log.info("行情接口"+this.mdInfo.getId() + "开始初始化");
        this.connect();
    }



    public CThostFtdcMdApi getMdApi(){
        return this.mdApi;
    }

    private void setStatus(boolean status){
        this.status = status;
    }
    @Override
    @Synchronized
    public void connect() {
        if (this.status) {
            return;
        }
        new Thread(()->{
            log.warn("行情接口实例初始化");
            String envTmpDir = System.getProperty("java.io.tmpdir");
            String tempFilePath = envTmpDir + File.separator + "qts" + File.separator + "md_"
                    + this.mdInfo.getId();
            File tempFile = new File(tempFilePath);
            if (!tempFile.getParentFile().exists()) {
                try {
                    FileUtils.forceMkdir(tempFile);
                    log.info("{} 创建临时文件夹" , mdName, tempFile.getParentFile().getAbsolutePath());
                } catch (IOException e) {
                    log.error("{} 创建临时文件夹失败", mdName, tempFile.getParentFile().getAbsolutePath());
                }
            }
            log.info("{} 使用临时文件夹", mdName, tempFile.getParentFile().getAbsolutePath());


            mdApi = CThostFtdcMdApi.CreateFtdcMdApi(tempFile.getAbsolutePath());
            mdApi.RegisterSpi(this);
            mdApi.RegisterFront(mdInfo.getMdAddress());
            mdApi.Init();
        }).run();

    }

    @Override
    @Synchronized
    public void close() {
        //仅释放接口
        if (mdApi != null) {
            try {
                log.info("行情接口释放启动！");
                mdApi.RegisterSpi(null);
                mdApi.Release();
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("行情接口释放发生异常！", e);
            }
            mdApi = null;
            status = false;
            log.warn("{} 行情接口实例关闭并释放", mdName);
        }
    }



    @Override
    public void subscribe(String symbol) {
        this.subscribedSymbols.add(symbol);
        if (isConnected()) {
            String[] symbolArray = new String[1];
            symbolArray[0] = symbol;
            mdApi.SubscribeMarketData(symbolArray, 1);
        } else {
            log.warn(mdName + "无法订阅行情,行情服务器尚未连接成功");
        }
    }

    @Override
    public boolean isConnected() {
        return this.status;
    }




    @Override
    public List<String> getSubscribedSymbols() {
        return this.subscribedSymbols.stream().toList();
    }


    /**
     * MdSpi
     */
    public void OnFrontConnected() {
        log.info("行情接口前置机已连接");
        // 修改前置机连接状态为true
        CThostFtdcReqUserLoginField userLoginField = new CThostFtdcReqUserLoginField();
        mdApi.ReqUserLogin(userLoginField, 0);
    }

    // 前置机断开回报
    public void OnFrontDisconnected(int nReason) {
        log.info( "行情接口前置机已断开,Reason:" + nReason);
        this.setStatus(false);
    }

    // 登录回报
    public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo,
                               int nRequestID, boolean bIsLast) {
        if (pRspInfo.getErrorID() == 0) {
            log.info("{}OnRspUserLogin! TradingDay:{},SessionID:{},BrokerID:{},UserID:{}", mdName,
                    pRspUserLogin.getTradingDay(), pRspUserLogin.getSessionID(), pRspUserLogin.getBrokerID(),
                    pRspUserLogin.getUserID());
            // 修改登录状态为true
            this.setStatus(true);
            this.tradingDay = pRspUserLogin.getTradingDay();
            log.info("{}行情接口获取到的交易日为{}", mdName, tradingDay);
            // 重新订阅之前的合约
            if (!this.getSubscribedSymbols().isEmpty()) {
                String[] subscribedSymbolsArray = this.getSubscribedSymbols()
                        .toArray(new String[this.getSubscribedSymbols().size()]);
                this.mdApi.SubscribeMarketData(subscribedSymbolsArray, subscribedSymbolsArray.length + 1);
            }
        } else {
            log.warn("{}行情接口登录回报错误! ErrorID:{},ErrorMsg:{}", mdName, pRspInfo.getErrorID(),
                    pRspInfo.getErrorMsg());
        }

    }

    // 心跳警告
    public void OnHeartBeatWarning(int nTimeLapse) {
        log.warn(mdName + "行情接口心跳警告 nTimeLapse:" + nTimeLapse);
    }

    // 登出回报
    public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID,
                                boolean bIsLast) {
        if (pRspInfo.getErrorID() != 0) {
            log.info("{}OnRspUserLogout!ErrorID:{},ErrorMsg:{}", mdName, pRspInfo.getErrorID(),
                    pRspInfo.getErrorMsg());
        } else {
            log.info("{}OnRspUserLogout!BrokerID:{},UserID:{}", mdName, pUserLogout.getBrokerID(),
                    pUserLogout.getUserID());

        }
        this.status = false;
    }

    // 错误回报
    public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        log.info("{}行情接口错误回报!ErrorID:{},ErrorMsg:{},RequestID:{},isLast{}", mdName, pRspInfo.getErrorID(),
                pRspInfo.getErrorMsg(), nRequestID, bIsLast);
    }

    // 订阅合约回报
    public void OnRspSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument,
                                   CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        if (pRspInfo.getErrorID() == 0) {
            log.info("{} OnRspSubMarketData! 订阅合约成功:{}", mdName,pSpecificInstrument.getInstrumentID());
        } else {
            log.error("{} OnRspSubMarketData! 订阅合约失败,ErrorID:{} ErrorMsg:{}", mdName, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    // 退订合约回报
    public void OnRspUnSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument,
                                     CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        if (pRspInfo.getErrorID() == 0) {
            log.info("{} OnRspSubMarketData! 退订合约成功:{}", mdName,pSpecificInstrument.getInstrumentID());
        } else {
            log.error("{} OnRspSubMarketData! 退订合约失败,ErrorID:{} ErrorMsg:{}", mdName, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }

    /**
     * 合约行情推送
     * @param pDepthMarketData
     */
    public void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField pDepthMarketData) {
        if (pDepthMarketData != null) {

            String symbol = pDepthMarketData.getInstrumentID();

//			if (!contractExchangeMap.containsKey(symbol)) {
//				log.warn("{} 收到合约 {}行情,但尚未获取到交易所信息,丢弃",loginInfo.getAccoutId(),symbol);
//				return;
//			}

            // 上期所 郑商所正常,大商所错误
            // TODO 大商所时间修正
            Long updateTime = Long.valueOf(pDepthMarketData.getUpdateTime().replaceAll(":", ""));
            Long updateMillisec = (long) pDepthMarketData.getUpdateMillisec();
            double datetIime=updateTime+updateMillisec/1000.0;
            //LocalDateTime.parse(pDepthMarketData.getActionDay())


            Tick tickData=new Tick();

            tickData.setSymbol(symbol);
            tickData.setExchange(pDepthMarketData.getExchangeID());
            tickData.setTradingDay(tradingDay);
            tickData.setActionDay(pDepthMarketData.getActionDay());
            tickData.setActionTime(datetIime);
            tickData.setStatus(0);
            tickData.setLastPrice(pDepthMarketData.getLastPrice());
            tickData.setVolume(pDepthMarketData.getVolume());

            Integer lastVolume = 0;
            if(preTickVolumeMap.containsKey(symbol)) {
                lastVolume = tickData.getVolume() - preTickVolumeMap.get(symbol);
            }else {
                lastVolume = tickData.getVolume();
            }
            tickData.setLastVolume(lastVolume);
            preTickVolumeMap.put(symbol, tickData.getVolume());
            tickData.setOpenInterest(pDepthMarketData.getOpenInterest());
            tickData.setPreOpenInterest((long)pDepthMarketData.getPreOpenInterest());
            tickData.setPreClosePrice(pDepthMarketData.getPreClosePrice());
            tickData.setPreSettlePrice(pDepthMarketData.getPreSettlementPrice());
            tickData.setOpenPrice(pDepthMarketData.getOpenPrice());
            tickData.setHighPrice(pDepthMarketData.getHighestPrice());
            tickData.setLowPrice(pDepthMarketData.getLowestPrice());
            tickData.setUpperLimit(pDepthMarketData.getUpperLimitPrice());
            tickData.setLowerLimit(pDepthMarketData.getLowerLimitPrice());
            tickData.setBidPrice1(pDepthMarketData.getBidPrice1());
            tickData.setBidPrice2(pDepthMarketData.getBidPrice2());
            tickData.setBidPrice3(pDepthMarketData.getBidPrice3());
            tickData.setBidPrice4(pDepthMarketData.getBidPrice4());
            tickData.setBidPrice5(pDepthMarketData.getBidPrice5());
            tickData.setAskPrice1(pDepthMarketData.getAskPrice1());
            tickData.setAskPrice2(pDepthMarketData.getAskPrice2());
            tickData.setAskPrice3(pDepthMarketData.getAskPrice3());
            tickData.setAskPrice4(pDepthMarketData.getAskPrice4());
            tickData.setAskPrice5(pDepthMarketData.getAskPrice5());
            tickData.setBidVolume1(pDepthMarketData.getBidVolume1());
            tickData.setBidVolume2(pDepthMarketData.getBidVolume2());
            tickData.setBidVolume3(pDepthMarketData.getBidVolume3());
            tickData.setBidVolume4(pDepthMarketData.getBidVolume4());
            tickData.setBidVolume5(pDepthMarketData.getBidVolume5());
            tickData.setAskVolume1(pDepthMarketData.getAskVolume1());
            tickData.setAskVolume2(pDepthMarketData.getAskVolume2());
            tickData.setAskVolume3(pDepthMarketData.getAskVolume3());
            tickData.setAskVolume4(pDepthMarketData.getAskVolume4());
            tickData.setAskVolume5(pDepthMarketData.getAskVolume5());

            log.info("tick:{}",tickData);
            //todo 对象池
            tickData.setTimeStampRecv(System.nanoTime());
            fastEventService.emitEvent(FastEvent.EVENT_TICK,tickData);

        } else {
            log.warn("{}OnRtnDepthMarketData! 收到行情信息为空", mdName);
        }
    }

    // 订阅期权询价
    public void OnRspSubForQuoteRsp(CThostFtdcSpecificInstrumentField pSpecificInstrument,
                                    CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        log.info("{}OnRspSubForQuoteRsp!", mdName);
    }

    // 退订期权询价
    public void OnRspUnSubForQuoteRsp(CThostFtdcSpecificInstrumentField pSpecificInstrument,
                                      CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        log.info("{}OnRspUnSubForQuoteRsp!", mdName);
    }

    // 期权询价推送
    public void OnRtnForQuoteRsp(CThostFtdcForQuoteRspField pForQuoteRsp) {
        log.info("{}OnRspUnSubForQuoteRsp!", mdName);
    }

}
