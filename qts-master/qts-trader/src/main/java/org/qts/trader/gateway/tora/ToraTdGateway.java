package org.qts.trader.gateway.tora;

import com.tora.traderapi.CTORATstpTraderApi;
import lombok.extern.slf4j.Slf4j;
import org.qts.common.disruptor.FastQueue;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.entity.Contract;
import org.qts.common.entity.Enums;
import org.qts.common.entity.LoginInfo;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.trade.OrderCancelReq;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.Position;
import org.qts.common.entity.trade.Trade;
import org.qts.common.utils.SpringUtils;
import org.qts.trader.gateway.TdGateway;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import com.tora.traderapi.*;
import org.springframework.util.StringUtils;

import static com.tora.traderapi.TORA_TE_RESUME_TYPE.TORA_TERT_QUICK;


@Slf4j
public class ToraTdGateway extends  CTORATstpTraderSpi implements TdGateway {
    private static String basePath;
    static {
        try {
            //System.loadLibrary("thostmduserapi_wrap");
            basePath = SpringUtils.getContext().getEnvironment().getProperty("base.path");
            if(!StringUtils.hasLength(basePath))
                throw new RuntimeException("can not find apiPath");
            //System.loadLibrary("ctp/thosttraderapi_wrap");
            System.load(basePath+"/lib/tora/libjavatraderapi.so");
        } catch (Exception e) {
            log.error("加载库失败!", e);
        }
    }

    private AcctDetail acct;
    private LoginInfo loginInfo;
    private FastQueue fastQueue;
    private ScheduledExecutorService scheduler;

    private CTORATstpTraderApi tdApi;

    private boolean isRunning = false; //接口是否执行中
    private boolean isConnected = false; //接口是否连接(登录完才算连接)
    private String tradingDay;

    private AtomicInteger reqID = new AtomicInteger(0); // 操作请求编号
    private AtomicInteger orderRef = new AtomicInteger(LocalTime.now().toSecondOfDay());

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
        log.info("td init ...");
        this.connect();
    }
    @Override
    public boolean isConnected() {
        return this.isRunning && this.isConnected;
    }

    private void setConnected(boolean status){
        this.isConnected = status;
        this.acct.setTdStatus( status);
        log.info("set isConnected to :{}",this.isConnected);
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

            tdApi = CTORATstpTraderApi.CreateTstpTraderApi();
            tdApi.RegisterSpi(this);
            tdApi.SubscribePublicTopic(TORA_TERT_QUICK); //注册公有流
            tdApi.SubscribePrivateTopic(TORA_TERT_QUICK);//注册私有流
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
        if(!isConnected()){
            log.warn("接口未连接,无法报单");
            orderReq.setStatus(Enums.ORDER_STATUS.ERROR);
            orderReq.setStatusMsg("接口未连接");
            return false;
        }
        CTORATstpInputOrderField input_order_field = new CTORATstpInputOrderField();
        input_order_field.setExchangeID(ToraMapper.exchangeMap.get(orderReq.getExchange()));
        input_order_field.setSecurityID(orderReq.getSymbol());
        input_order_field.setDirection(orderReq.getDirection() == Enums.TRADE_DIRECTION.BUY?traderapi.getTORA_TSTP_D_Buy():traderapi.getTORA_TSTP_D_Sell());
        input_order_field.setVolumeTotalOriginal(orderReq.getTotalVolume());
        input_order_field.setLimitPrice(orderReq.getPrice());
        if(orderReq.getPriceType()== Enums.PRICE_TYPE.LIMIT ){
            input_order_field.setOrderPriceType(traderapi.getTORA_TSTP_OPT_LimitPrice());
        }else {
            input_order_field.setOrderPriceType(traderapi.getTORA_TSTP_OPT_BestPrice());
        }

        input_order_field.setTimeCondition(traderapi.getTORA_TSTP_TC_GFD());
        input_order_field.setVolumeCondition(traderapi.getTORA_TSTP_VC_AV());

        int ret = tdApi.ReqOrderInsert(input_order_field, reqID.incrementAndGet());
        if (ret != 0)
        {
            log.error("ReqOrderInsert fail, ret[{}]", ret);
            orderReq.setStatus(Enums.ORDER_STATUS.ERROR);
            orderReq.setStatusMsg("插入报单失败");
            return false;
        }
        return true;
    }

    @Override
    public void cancelOrder(OrderCancelReq cancelOrderReq) {
        if(!isConnected()){
            log.warn("接口未连接,无法撤单");
            return;
        }
        CTORATstpInputOrderActionField input_order_action_field = new CTORATstpInputOrderActionField();
        input_order_action_field.setExchangeID(ToraMapper.exchangeMap.get(cancelOrderReq.getExchange()));
        input_order_action_field.setActionFlag(traderapi.getTORA_TSTP_AF_Delete());
        if(StringUtils.hasLength(cancelOrderReq.getOrderSysID()))
            input_order_action_field.setOrderSysID(cancelOrderReq.getOrderSysID());
        else {
            input_order_action_field.setFrontID(cancelOrderReq.getFrontId());
            input_order_action_field.setSessionID(cancelOrderReq.getSessionId());
            input_order_action_field.setOrderRef(Integer.valueOf(cancelOrderReq.getOrderRef()));
        }

        int ret = tdApi.ReqOrderAction(input_order_action_field, reqID.incrementAndGet());
        if (ret != 0)
        {
            log.error("ReqOrderAction fail, ret[{}]", ret);
        }
    }

    
    @Override
    public Contract getContract(String symbol) {
        return null;
    }


    @Override
    public void qryContract() {
        if (!this.isConnected()) {
            log.warn("接口未连接,无法查询合约");
            return;
        }
        //TODO
//        CTORATstpQrySecurityField qry_security_field = new CTORATstpQrySecurityField();
//        qry_security_field.setExchangeID(traderapi.getTORA_TSTP_EXD_SSE());
//        //qry_security_field.setSecurityID("600000");
//        int ret = tdApi.ReqQrySecurity(qry_security_field, reqID.incrementAndGet());
//        if (ret != 0)
//            log.error("ReqQrySecurity fail, ret[{}]", ret);

    }
    @Override
    public void qryPosition(){
        if (!this.isConnected()) {
            log.warn("接口未连接,无法查询持仓");
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
        if (!this.isConnected()) {
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
            log.warn("td is released!");
            return;
        }


        CTORATstpReqUserLoginField req_user_login_field = new CTORATstpReqUserLoginField();
        req_user_login_field.setLogInAccount(loginInfo.getUserId());
        req_user_login_field.setLogInAccountType(traderapi.getTORA_TSTP_LACT_AccountID());
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

            this.sessionID = pRspUserLogin.getSessionID();
            this.frontID = pRspUserLogin.getFrontID();
            // 修改登录状态为true
            this.setConnected(true);
            this.tradingDay = pRspUserLogin.getTradingDay();
            String maxOrderRef = String.valueOf(pRspUserLogin.getMaxOrderRef());
            log.info("OnRspUserLogin! TradingDay:{},SessionID:{},FrontID:{},UserID:{},maxOrderRef:{},OrderInsertCommFlux:{},OrderActionCommFlux:{}",
                    this.tradingDay, this.sessionID, this.frontID, pRspUserLogin.getUserID(),maxOrderRef,pRspUserLogin.getOrderInsertCommFlux(),pRspUserLogin.getOrderActionCommFlux());


            //发起其他查询
            int delay=1250;
            this.scheduler.schedule(()->{
                this.qryTradingAccount();
            },delay, TimeUnit.MILLISECONDS);

            this.scheduler.schedule(()->{
                this.qryContract();
            },delay*2, TimeUnit.MILLISECONDS);

            this.scheduler.schedule(()->{
                this.qryPosition();
            },delay*3, TimeUnit.MILLISECONDS);

        } else {
            log.error("OnRspUserLogin error! ErrorID:{},ErrorMsg:{}", pRspInfo.getErrorID(),
                    pRspInfo.getErrorMsg());
        }
    }

    public void OnRspQrySecurity(CTORATstpSecurityField pSecurity, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        if (pSecurity != null)
        {
            log.info("OnRspQrySecurity[{}]: SecurityID[{}] SecurityName[{}] UpperLimitPrice[{}] LowerLimitPrice[{}]",
                    nRequestID, pSecurity.getSecurityID(), pSecurity.getSecurityName(),
                    pSecurity.getUpperLimitPrice(), pSecurity.getLowerLimitPrice());
        }

        if (bIsLast)
        {
            log.info("OnRspUserLogin finish");
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
            AcctInfo acctInfo = acct.getAcctInfo();
            acctInfo.setAvailable(pTradingAccount.getUsefulMoney());
            acctInfo.setPreBalance(pTradingAccount.getPreDeposit());
            acctInfo.setBalance(pTradingAccount.getDeposit()+pTradingAccount.getDeposit()-pTradingAccount.getWithdraw());
        }
    }

    public void OnRspQryPosition(CTORATstpPositionField pPosition, CTORATstpRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        if (pPosition != null)
        {

            String exchange = ToraMapper.exchangeMapReverse.get(pPosition.getExchangeID());
            Position position=new Position(pPosition.getSecurityID(),exchange, Enums.POS_DIRECTION.NET);
            position.setSymbolName(pPosition.getSecurityName());
            position.setYdPos(pPosition.getHistoryPos());
            position.setYdFrozen(pPosition.getHistoryPosFrozen());
            position.setTdPos(pPosition.getTodayBSPos()+pPosition.getTodayPRPos());
            position.setTdFrozen(pPosition.getTodayBSPosFrozen()+pPosition.getTodayPRPosFrozen());
            position.setAvgPrice(pPosition.getTotalPosCost());
            position.setCommission(pPosition.getTodayCommission());
            position.setTotalPos(pPosition.getCurrentPosition());

            this.positionMap.put(position.getId(), position);
            log.info("OnRspQryPosition:",position);

            if(bIsLast){
                log.info("OnRspQryPosition finish!");
            }

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
        if(pOrder==null && !this.orderMap.containsKey(pOrder.getOrderRef()))
            return;
        //更新报单
        Order lastOrder = this.orderMap.get(pOrder.getOrderRef());
        lastOrder.setLastTradedVolume(lastOrder.getTradedVolume());
        lastOrder.setOrderSysID(pOrder.getOrderSysID());
        lastOrder.setStatus(ToraMapper.statusMapReverse.getOrDefault(pOrder.getOrderStatus(), Enums.ORDER_STATUS.UNKNOWN));
        lastOrder.setStatusMsg(pOrder.getStatusMsg());
        lastOrder.setTradedVolume(pOrder.getVolumeTraded());
        this.fastQueue.emitEvent(FastEvent.EV_ORDER,lastOrder);

        //更新持仓(报单前需要确保仓位存在)
        int curTradedVolume = lastOrder.getTradedVolume()- lastOrder.getLastTradedVolume();
        if(curTradedVolume>0 && lastOrder.getDirection() == Enums.TRADE_DIRECTION.BUY ){
            Position pos = this.positionMap.get(lastOrder.getSymbol()+"-"+ Enums.POS_DIRECTION.NET);
            if(pos!=null){
                pos.setTdPos(pos.getTdPos()+curTradedVolume);
                this.fastQueue.emitEvent(FastEvent.EV_POSITION,pos);
            }else{
                log.error("order[{}] 找不到对应的持仓",lastOrder.getOrderRef());
            }
        }
        if(curTradedVolume>0 && lastOrder.getDirection() == Enums.TRADE_DIRECTION.SELL ){
            Position pos = this.positionMap.get(lastOrder.getSymbol()+"-"+ Enums.POS_DIRECTION.NET);
            if(pos!=null){
                //优先平昨(部分券运行平今)
                int left = pos.getYdPos()-curTradedVolume;
                pos.setYdPos(left>=0?left:0);
                if(left<0)
                    pos.setTdPos(pos.getTdPos()+left);
                this.fastQueue.emitEvent(FastEvent.EV_POSITION,pos);
            }else{
                log.error("order[{}] 找不到对应的持仓",lastOrder.getOrderRef());
            }
        }

        log.info("OnRtnOrder: orderRef:{} orderSysId:{}  traded:{}/{}  status:{} msg:{}",
                lastOrder.getOrderRef(),lastOrder.getOrderSysID(),lastOrder.getTotalVolume(),lastOrder.getTotalVolume(),lastOrder.getStatus(),lastOrder.getStatusMsg());
    }

    public void OnRtnTrade(CTORATstpTradeField pTrade)
    {
        if(pTrade==null)
            return;
        Trade trade =new Trade();
        trade.setTradeID(pTrade.getTradeID());
        trade.setTradingDay(pTrade.getTradingDay());
        trade.setOffset(Enums.OFFSET.NONE);
        trade.setDirection(ToraMapper.directionMapReverse.get(pTrade.getDirection()));
        trade.setSymbol(pTrade.getSecurityID());
        trade.setExchange(ToraMapper.exchangeMapReverse.get(pTrade.getExchangeID()));
        trade.setPrice(pTrade.getPrice());
        trade.setVolume(pTrade.getVolume());
        trade.setTradeDate(pTrade.getTradeDate());
        trade.setTradeTime(pTrade.getTradeTime());
        trade.setOrderRef(pTrade.getOrderRef()+"");
        trade.setOrderSysID(pTrade.getOrderSysID());
        this.acct.getTradeList().put(trade.getTradeID(),trade);
        this.fastQueue.emitEvent(FastEvent.EV_TRADE,trade);
        log.info("OnRtnTrade:{}",trade);

    }

    public void OnRspOrderAction(CTORATstpInputOrderActionField pInputOrderActionField, CTORATstpRspInfoField pRspInfo, int nRequestID)
    {
        if (pRspInfo.getErrorID() == 0)
        {
            log.info("OnRspOrderAction: OK! [{}]\n", nRequestID);
        }
        else
        {
            log.error("OnRspOrderAction: Error! [{}] [{}] [{}]", nRequestID, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        }
    }
}
