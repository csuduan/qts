package org.qts.trader.gateway.tora;

import com.tora.traderapi.CTORATstpTraderApi;
import lombok.extern.slf4j.Slf4j;
import org.qts.common.disruptor.FastQueue;
import org.qts.common.entity.Contract;
import org.qts.common.entity.LoginInfo;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.trade.CancelOrderReq;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.Position;
import org.qts.trader.gateway.TdGateway;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import com.tora.traderapi.*;

import static com.tora.traderapi.TORA_TE_RESUME_TYPE.TORA_TERT_RESTART;


@Slf4j
public class ToraTdGateway extends  CTORATstpTraderSpi implements TdGateway {

    static {
        try {
            System.loadLibrary("javatraderapi");
        } catch (Exception e) {
            log.error("加载库失败!", e);
        }
    }

    private String tdName;
    private AcctDetail acct;

    private LoginInfo loginInfo;
    private FastQueue fastQueue;
    private ScheduledExecutorService scheduler;


    private CTORATstpTraderApi tdApi;

    private boolean isRunning = false; //接口是否执行中
    private boolean isConnected = false; //接口是否连接(登录完才算连接)
    private String tradingDay;

    private AtomicInteger reqID = new AtomicInteger(0); // 操作请求编号

    private int frontID = 0; // 前置机编号
    private int sessionID = 0; // 会话编号

    private Map<String, Position> positionMap = new HashMap<>();
    private Map<String, Contract> contractMap = new HashMap<>();
    private Map<String, Order> orderMap = new HashMap<>();

    public ToraTdGateway(AcctDetail acctInfo){
        this.acct = acctInfo;
        this.scheduler = acctInfo.getScheduler();
        this.fastQueue = acctInfo.getFastQueue();
        this.loginInfo = new LoginInfo(acctInfo.getConf());
        this.tdName = loginInfo.getUserId();
        log.info("td init ...");
        this.connect();
    }
    @Override
    public boolean isConnected() {
        return this.isRunning && this.isConnected;
    }

    private void setConnected(boolean status){
        this.isConnected = status;
    }

    @Override
    public void connect() {
        if (isRunning) {
            return;
        }
        scheduler.submit(() -> {
            try {
                log.info("td start connect...");
                this.connectAsync();
                //10s后若连接失败，则自动关闭
                Thread.sleep(10000);
                if (!this.isConnected) {
                    log.error("td connect timeout ...");
                    this.close();
                }
            } catch (Exception ex) {
                log.error("td connect error", ex);
            }
        });
    }

    private void connectAsync() {
        Thread thread = new Thread(() -> {
            isRunning = true;
            log.warn("td thread start ...");
            String envTmpDir = System.getProperty("java.io.tmpdir");


            tdApi = CTORATstpTraderApi.CreateTstpTraderApi();
            tdApi.RegisterSpi(this);
            tdApi.SubscribePublicTopic(TORA_TERT_RESTART); //注册公有流
            tdApi.SubscribePrivateTopic(TORA_TERT_RESTART);//注册私有流
            tdApi.RegisterFront(loginInfo.getAddress());
            tdApi.Init();
            tdApi.Join();
            log.info("td thread exit...");
        });
        thread.setName("td");
        thread.start();
    }

    @Override
    public void close() {
        if (!this.isRunning)
            return;
        //需要在异步线程中释放，否则会出现crash
        new Thread(() -> {
            if (tdApi != null) {
                try {
                    this.tdApi.RegisterSpi(null);
                    this.tdApi.Release();
                } catch (Exception e) {
                    log.error("td release error！", e);
                }
                tdApi = null;
                this.setConnected(false);
                log.warn("td released");
            }
            isRunning = false;
        }).start();
    }

    @Override
    public boolean insertOrder(Order orderReq) {
        return false;
    }

    @Override
    public void cancelOrder(CancelOrderReq cancelOrderReq) {

    }

    
    @Override
    public Contract getContract(String symbol) {
        return null;
    }


    @Override
    public void qryContract() {
        if (this.isConnected()) {
            log.warn("接口未连接,无法查询账户");
            return;
        }
        CTORATstpQrySecurityField qry_security_field = new CTORATstpQrySecurityField();
        qry_security_field.setExchangeID(traderapi.getTORA_TSTP_EXD_SSE());
        //qry_security_field.setSecurityID("600000");
        int ret = tdApi.ReqQrySecurity(qry_security_field, reqID.incrementAndGet());
        if (ret != 0)
            log.error("ReqQrySecurity fail, ret[{}]", ret);

    }
    @Override
    public void qryPosition(){
        if (this.isConnected()) {
            log.warn("接口未连接,无法查询账户");
            return;
        }
        CTORATstpQryPositionField qry_position_field = new CTORATstpQryPositionField();
        qry_position_field.setInvestorID(loginInfo.getUserId());
        int ret = tdApi.ReqQryPosition(qry_position_field, reqID.incrementAndGet());
        if( ret !=0)
            log.error("ReqQryPosition fail, ret[{}]", ret);

    }
    @Override
    public void qryTradingAccount() {
        if (this.isConnected()) {
            log.warn("接口未连接,无法查询账户");
            return;
        }
        CTORATstpQryTradingAccountField qry_investor_field = new CTORATstpQryTradingAccountField();
        qry_investor_field.setInvestorID(loginInfo.getUserId());
        int ret = tdApi.ReqQryTradingAccount(qry_investor_field, reqID.incrementAndGet());
        if (ret != 0)
            log.error("ReqQryTradingAccount fail, ret[{}]", ret);
    }

    private void login() {
        if (tdApi == null) {
            log.warn("{} 交易接口实例已经释放", tdName);
            return;
        }


        CTORATstpReqUserLoginField req_user_login_field = new CTORATstpReqUserLoginField();
        req_user_login_field.setLogInAccount(loginInfo.getUserId());
        req_user_login_field.setLogInAccountType(traderapi.getTORA_TSTP_LACT_UserID());
        req_user_login_field.setPassword(loginInfo.getPassword());
        req_user_login_field.setUserProductInfo("qts");

        int ret = tdApi.ReqUserLogin(req_user_login_field, reqID.incrementAndGet());
        if(ret != 0)
            log.error("ReqUserLogin fail, ret[{}]",ret);
    }

    public void OnFrontConnected()
    {
        log.info("OnFrontConnected");
        this.login();
    }

    public void OnFrontDisconnected(int nReason)
    {
        log.warn("OnFrontDisconnected, reason[{}]",nReason);
        this.close();
    }

    public void OnRspUserLogin(CTORATstpRspUserLoginField pRspUserLogin, CTORATstpRspInfoField pRspInfo, int nRequestID)
    {
        if (pRspInfo.getErrorID() == 0) {
            log.info("{} 交易接口登录成功! TradingDay:{},SessionID:{},BrokerID:{},UserID:{}", tdName,
                    pRspUserLogin.getTradingDay(), pRspUserLogin.getSessionID(), "",
                    pRspUserLogin.getUserID());
            sessionID = pRspUserLogin.getSessionID();
            frontID = pRspUserLogin.getFrontID();
            // 修改登录状态为true
            this.setConnected(true);
            tradingDay = pRspUserLogin.getTradingDay();
            log.info("{}交易接口获取到的交易日为{}", tdName, tradingDay);

            String maxOrderRef = String.valueOf(pRspUserLogin.getMaxOrderRef());
            log.info("maxOrderRef:{},OrderInsertCommFlux:{},OrderActionCommFlux:{}",
                    maxOrderRef,pRspUserLogin.getOrderInsertCommFlux(),pRspUserLogin.getOrderActionCommFlux());

            //发起其他查询
            int delay=1250;
            this.scheduler.schedule(()->{

            },delay, TimeUnit.MILLISECONDS);

            this.scheduler.schedule(()->{
                this.qryContract();
            },delay*2, TimeUnit.MILLISECONDS);

            this.scheduler.schedule(()->{
                this.qryPosition();
            },delay*3, TimeUnit.MILLISECONDS);

        } else {
            log.error("{}交易接口登录回报错误! ErrorID:{},ErrorMsg:{}", tdName, pRspInfo.getErrorID(),
                    pRspInfo.getErrorMsg());
        }
    }

    public void OnRspQrySecurity(CTORATstpSecurityField pSecurity, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        if (pSecurity != null)
        {
            System.out.printf("OnRspQrySecurity[%d]: SecurityID[%s] SecurityName[%s] UpperLimitPrice[%.3f] LowerLimitPrice[%.3f]\n",
                    nRequestID, pSecurity.getSecurityID(), pSecurity.getSecurityName(),
                    pSecurity.getUpperLimitPrice(), pSecurity.getLowerLimitPrice());
        }

        if (bIsLast)
        {
            System.out.printf("��ѯ��Լ��Ϣ����!\n");
        }
    }

    public void OnRspQryInvestor(CTORATstpInvestorField pInvestor, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        log.info("OnRspQryInvestor");
    }

    public void OnRspQryShareholderAccount(CTORATstpShareholderAccountField pShareholderAccount, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        log.info("OnRspQryShareholderAccount");
    }

    public void OnRspQryTradingAccount(CTORATstpTradingAccountField pTradingAccount, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        if (pTradingAccount != null)
        {
            log.info("OnRspQryTradingAccount[{}]: DepartmentID[{}] InvestorID[{}] AccountID[{}] CurrencyID[{}] UsefulMoney[{}] WithdrawQuota[{}]",
                    nRequestID, pTradingAccount.getDepartmentID(), pTradingAccount.getInvestorID(),
                    pTradingAccount.getAccountID(), pTradingAccount.getCurrencyID(), pTradingAccount.getUsefulMoney(),
                    pTradingAccount.getWithdraw());
        }
    }

    public void OnRspQryPosition(CTORATstpPositionField pPosition, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        if (pPosition != null)
        {
            System.out.printf("OnRspQryPosition[%d]: InvestorID[%s] SecurityID[%s] HistoryPos[%d] TodayBSPos[%d] TodayPRPos[%d]\n",
                    nRequestID, pPosition.getInvestorID(), pPosition.getSecurityID(),
                    pPosition.getHistoryPos(), pPosition.getTodayBSPos(), pPosition.getTodayPRPos());
        }
    }

    public void OnRspQryOrder(CTORATstpOrderField pOrder, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        if (pOrder != null)
        {
            log.info("OnRspQryOrder[{}]: SecurityID[{}] OrderLocalID[{}] OrderRef[{}] OrderSysID[{}] VolumeTraded[{}] OrderStatus[{}] OrderSubmitStatus[{}] StatusMsg[{}]",
                    nRequestID, pOrder.getSecurityID(), pOrder.getOrderLocalID(), pOrder.getOrderRef(),
                    pOrder.getOrderSysID(), pOrder.getVolumeTraded(), pOrder.getOrderStatus(),
                    pOrder.getOrderSubmitStatus(), pOrder.getStatusMsg());
        }
    }

    public void OnRspOrderInsert(CTORATstpInputOrderField pInputOrderField, CTORATstpRspInfoField pRspInfo, int nRequestID)
    {
        if (pRspInfo.getErrorID() != 0)
        {
            log.error("OnRspOrderInsert: Error! [%d] [{}] [{}]", nRequestID, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }

    }

    public void OnRtnOrder(CTORATstpOrderField pOrder)
    {
        System.out.printf("OnRtnOrder: InvestorID[%s] SecurityID[%s] OrderRef[%s] OrderLocalID[%s] LimitPrice[%.2f] VolumeTotalOriginal[%d] OrderSysID[%s] OrderStatus[%c]\n",
                pOrder.getInvestorID(), pOrder.getSecurityID(), pOrder.getOrderRef(), pOrder.getOrderLocalID(),
                pOrder.getLimitPrice(), pOrder.getVolumeTotalOriginal(), pOrder.getOrderSysID(),
                pOrder.getOrderStatus());
    }

    public void OnRtnTrade(CTORATstpTradeField pTrade)
    {
        System.out.printf("OnRtnTrade: TradeID[%s] InvestorID[%s] SecurityID[%s] OrderRef[%s] OrderLocalID[%s] Price[%.2f] Volume[%d]\n",
                pTrade.getTradeID(), pTrade.getInvestorID(), pTrade.getSecurityID(),
                pTrade.getOrderRef(), pTrade.getOrderLocalID(), pTrade.getPrice(), pTrade.getVolume());
    }

    public void OnRspOrderAction(CTORATstpInputOrderActionField pInputOrderActionField, CTORATstpRspInfoField pRspInfo, int nRequestID)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            System.out.printf("OnRspOrderAction: OK! [%d]\n", nRequestID);
        }
        else
        {
            System.out.printf("OnRspOrderAction: Error! [%d] [%d] [%s]\n", nRequestID, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }
}
