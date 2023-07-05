//
// Created by Administrator on 2020/7/2.
//
#include "ctp/ThostFtdcTraderApi.h"
#include "ctp/ThostFtdcUserApiStruct.h"
#include "define.h"
#include <dlfcn.h>

#ifndef TRADECORE_CTPTDAPI_H
#define TRADECORE_CTPTDAPI_H

#include "data.h"
#include "context.h"
#include "gateway.h"
#include "lockFreeQueue.hpp"
#include "common/timer.hpp"

class CtpTdGateway : public CThostFtdcTraderSpi, public TdGateway {
private:

    string id;
    CThostFtdcTraderApi *m_pUserApi;
    Acct * account;
    int frontId = 0;// 前置机编号
    int sessionId = 0;// 会话编号
    vector<Position> tmpPositons;
    vector<Trade> tmpTrades;
    Timer timer;
    LockFreeQueue<Event> *queue;

    string address;
    string brokerId;
    string appId;
    string authCode;


    void Run(){
        const char *address = this->address.c_str();
        m_pUserApi->RegisterFront(const_cast<char *>(address));
        m_pUserApi->Init();
        m_pUserApi->Join();
    }


    static inline int nRequestID=0;
    static inline map<TThostFtdcOrderStatusType, ORDER_STATUS> statusMap={
            {THOST_FTDC_OST_AllTraded,             ORDER_STATUS::ALLTRADED},
            {THOST_FTDC_OST_PartTradedQueueing,    ORDER_STATUS::QUEUEING},
            {THOST_FTDC_OST_PartTradedNotQueueing, ORDER_STATUS::NOTQUEUEING},
            {THOST_FTDC_OST_NoTradeQueueing,       ORDER_STATUS::QUEUEING},
            {THOST_FTDC_OST_NoTradeNotQueueing,    ORDER_STATUS::NOTQUEUEING},
            {THOST_FTDC_OST_Canceled,              ORDER_STATUS::CANCELLED},
            {THOST_FTDC_OST_Unknown,               ORDER_STATUS::UNKNOWN}
    };
    static inline map<int, string> qryRetMsgMap={
            {0,  "成功"},
            {-1, "网络连接失败"},
            {-2, "未处理请求超过许可数"},
            {-3, "每秒发送请求数超过许可数"}

    };

public:
    CtpTdGateway(Acct *account) : account(account) {
        //this->queue=account->tdQueue;
        this->id = account->id;

        vector<string> tmp;
        Util::split(account->acctConf->tdAddress,tmp,"|");
        this->address=tmp[0];
        this->brokerId=tmp[1];
        this->appId=tmp[2];
        this->authCode=tmp[3];

    }

    ~CtpTdGateway() {}

    ///连接
    int connect() override{
        void *handle = dlopen("lib/ctp/thosttraderapi_se.so", RTLD_LAZY);
        if(handle == nullptr){
            logi("load thosttraderapi.dll fail [{}] [{}]", errno, strerror(errno));
            return -1;
        }
        typedef CThostFtdcTraderApi *(*CreateFtdcTdApiFunc)(const char *);
        CreateFtdcTdApiFunc pfnCreateFtdcTdApiFunc = (CreateFtdcTdApiFunc) dlsym(handle,
                                                                                 "_ZN19CThostFtdcTraderApi19CreateFtdcTraderApiEPKc");
        if (pfnCreateFtdcTdApiFunc == nullptr) {
            logi("load thosttraderapi.dll fail [{}] [{}]", errno, strerror(errno));
            return -1;
        }
        m_pUserApi = pfnCreateFtdcTdApiFunc("./flow");
        m_pUserApi->RegisterSpi(this);
        m_pUserApi->SubscribePrivateTopic(THOST_TERT_QUICK);
        m_pUserApi->SubscribePublicTopic(THOST_TERT_QUICK);

        thread t(&CtpTdGateway::Run, this);
        t.detach();
        return 0;
    }

    ///断开
    int disconnect() override {
        logw("CtpTdGateway discounnect ");
        try {
            if (m_pUserApi != nullptr) {
                connected = false;
                m_pUserApi->Release();
                m_pUserApi = nullptr;
            }
        } catch (exception ex) {
            loge("discounnect fail ,{}", ex.what());
        }
        return 0;
    }

    ///插入报单
    bool insertOrder(Order *order) override{
        CThostFtdcInputOrderField req = {0};
        strcpy(req.BrokerID, this->brokerId.c_str());
        strcpy(req.InvestorID, account->acctConf->user.c_str());
        strcpy(req.InstrumentID, order->symbol.c_str());
        strcpy(req.ExchangeID, order->exchange.c_str());
        strcpy(req.OrderRef, to_string(order->orderRef).c_str());
        req.RequestID = this->nRequestID++;
        req.Direction = order->direction == BUY ? THOST_FTDC_D_Buy : THOST_FTDC_D_Sell;//买/卖
        switch (order->offset) {
            //组合开平标志: 开仓/平仓
            case OPEN:
                req.CombOffsetFlag[0] = THOST_FTDC_OF_Open;
                break;
            case CLOSETD:
                req.CombOffsetFlag[0] = THOST_FTDC_OF_CloseToday;
                break;
            case CLOSEYD:
                req.CombOffsetFlag[0] = THOST_FTDC_OF_CloseYesterday;
                break;
            default:
                req.CombOffsetFlag[0] = THOST_FTDC_OF_Close;
                break;
        }
        req.CombHedgeFlag[0] = THOST_FTDC_HF_Speculation;//组合投机套保标志
        req.VolumeTotalOriginal = order->totalVolume;
        req.MinVolume = 1;
        req.TimeCondition = THOST_FTDC_TC_GFD;///当日有效
        req.VolumeCondition = THOST_FTDC_VC_AV;///任何数量
        req.ContingentCondition = THOST_FTDC_CC_Immediately;//触发条件: 立即
        req.StopPrice = 0;
        req.ForceCloseReason = THOST_FTDC_FCC_NotForceClose;//强平原因: 非强平
        req.UserForceClose = 0;
        req.IsAutoSuspend = 0;

        switch (order->orderType) {
            case FAK:
                req.TimeCondition = THOST_FTDC_TC_IOC;
                req.VolumeCondition = THOST_FTDC_VC_AV;
                break;
            case FOK:
                req.TimeCondition = THOST_FTDC_TC_IOC;
                req.VolumeCondition = THOST_FTDC_VC_CV;
                break;
            default:
                req.TimeCondition = THOST_FTDC_TC_GFD;///有效期类型: 当日有效
                req.VolumeCondition = THOST_FTDC_VC_AV;//成交量类型
                break;
        }
        // 限价单
        req.OrderPriceType = THOST_FTDC_OPT_LimitPrice;
        req.LimitPrice = order->price;
        int ret = this->m_pUserApi->ReqOrderInsert(&req, req.RequestID);
        logi("ReqOrder orderRef:{} symbol:{} offset:{} direction:{} price:{} volume:{}  ret={}",
             order->orderRef, order->symbol, order->offsetStr, order->directionStr, order->price, order->totalVolume,
             ret);
        if (ret != 0) {
            order->status = ORDER_STATUS::ERROR;
            order->statusMsg = "insert order fail";
        } else {
            account->orderMap[order->orderRef] = order;
        }
        return  ret==0;
    }

    ///撤销报单
    void cancelOrder(Action *order) override{
        if (!this->isConnected()) {
            loge("Not connected");
            return;
        }
        CThostFtdcInputOrderActionField req = {0};
        req.ActionFlag = THOST_FTDC_AF_Delete;

        strcpy(req.BrokerID, this->brokerId.c_str());
        strcpy(req.InvestorID, account->acctConf->user.c_str());
        //strcpy_s(req.UserID, g_chUserID);
        //strcpy(req.InstrumentID, order->symbol.c_str());


        //strcpy(req.OrderSysID, vector_OrderSysID.at(action_num - 1).c_str());
        //strcpy(req.ExchangeID, vector_ExchangeID.at(action_num - 1).c_str());
        req.FrontID = frontId;
        req.SessionID = sessionId;
        strcpy(req.OrderRef, to_string(order->orderRef).c_str());

        //account->orderMap[order->orderRef] = order;
        int ret = m_pUserApi->ReqOrderAction(&req, this->nRequestID++);
        logi("ReqOrderAction orderRef:{} ret={}", order->orderRef, ret);
    }

    ///查询持仓
    void reqQryPosition(){
        if (!this->isConnected()) {
            loge("Not connected");
            return;
        }
        tmpPositons.clear();
        CThostFtdcQryInvestorPositionField cThostFtdcQryInvestorPositionField;
        strcpy(cThostFtdcQryInvestorPositionField.BrokerID, this->brokerId.c_str());
        strcpy(cThostFtdcQryInvestorPositionField.InvestorID, account->acctConf->user.c_str());
        int ret = this->m_pUserApi->ReqQryInvestorPosition(&cThostFtdcQryInvestorPositionField, this->nRequestID++);
        logi("查询持仓,ret={}",  ret);

    }

    ///用户口令更新请求
    void reqUserPasswordUpdate(){
    }

    ///前置连接响应
    void OnFrontConnected() override{
        fmtlog::setThreadName("TdGateway");
        logi("OnFrontConnected");

        //认证通过后再登录
        CThostFtdcReqAuthenticateField req = {0};
        strcpy(req.BrokerID, this->brokerId.c_str());
        strcpy(req.UserID, account->acctConf->user.c_str());
        strcpy(req.AuthCode, this->authCode.c_str());
        strcpy(req.AppID, this->appId.c_str());
        int ret = m_pUserApi->ReqAuthenticate(&req, this->nRequestID++);
        logi("ReqAuthenticate ret={}", ret);

    }

    ///前置断开响应
    void OnFrontDisconnected(int nReason) override{
        logi("OnFrontDisconnected");
    }

    ///客户端认证响应
    void OnRspAuthenticate(CThostFtdcRspAuthenticateField *pRspAuthenticateField, CThostFtdcRspInfoField *pRspInfo,
                           int nRequestID, bool bIsLast) override{
        logi("OnRspAuthenticate ");
        if (pRspInfo->ErrorID == 0) {
            CThostFtdcReqUserLoginField reqUserLogin = {0};
            strcpy(reqUserLogin.BrokerID, this->brokerId.c_str());
            strcpy(reqUserLogin.UserID, account->acctConf->user.c_str());
            strcpy(reqUserLogin.Password, account->acctConf->pwd.c_str());
            // 发出登陆请求
            int ret = m_pUserApi->ReqUserLogin(&reqUserLogin, nRequestID++);
            logi("ReqUserLogin ret={}", ret);
        } else {
            loge("认证失败 {}", pRspInfo->ErrorMsg);
        }


    }

    ///登录请求响应
    void OnRspUserLogin(CThostFtdcRspUserLoginField *pRspUserLogin, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                        bool bIsLast) override{
        logi("OnRspUserLogin");
        if (pRspInfo->ErrorID == 0) {
            this->sessionId = pRspUserLogin->SessionID;
            this->frontId = pRspUserLogin->FrontID;
            this->connected = true;
            this->tradingDay = pRspUserLogin->TradingDay;
            logi("交易接口登录成功,交易日={}", tradingDay);

            timer.delay(500, [this]() {
                //确认结算单
                CThostFtdcSettlementInfoConfirmField req = {0};
                strcpy(req.BrokerID, this->brokerId.c_str());
                strcpy(req.InvestorID, account->acctConf->user.c_str());
                int ret = m_pUserApi->ReqSettlementInfoConfirm(&req, this->nRequestID++);
                logi("ReqSettlementInfoConfirm ret={},{}", ret, qryRetMsgMap[ret]);
            });


        } else {
            loge("交易接口登录成功失败! ErrorID:{},ErrorMsg:{}",  pRspInfo->ErrorID, pRspInfo->ErrorMsg);
            this->connected = false;
        }


    }

    ///登出请求响应
    void OnRspUserLogout(CThostFtdcUserLogoutField *pUserLogout, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                         bool bIsLast) override{
        logw("OnRspUserLogout");
        this->connected = false;
    }

    ///错误应答
    void OnRspError(CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override {
        loge("OnRspError");
    }

    ///报单通知
    void OnRtnOrder(CThostFtdcOrderField *pOrder) override{
        int orderRef=atoi(pOrder->OrderRef);
        if (account->orderMap.count(orderRef)==0)
            return;
        long tsc=Context::get().tn.rdtsc();
        Order *order = account->orderMap[orderRef];
        //order->tradedVolume = order.TradedVolume;
        order->orderSysId = pOrder->OrderSysID;
        order->tradedVolume = pOrder->VolumeTraded;
        order->status = statusMap[pOrder->OrderStatus];
        order->statusStr= enum_string(order->status);
        order->statusMsg = pOrder->StatusMsg;
        order->updateTime = pOrder->UpdateTime;
        order->updateTsc=tsc;
        //错单识别
        if (order->statusMsg.find("拒绝") != string::npos ||
            order->statusMsg.find("禁止") != string::npos ||
            order->statusMsg.find("不足") != string::npos ||
            order->statusMsg.find("错误") != string::npos ||
            order->statusMsg.find("未连接") != string::npos ||
            order->statusMsg.find("暂停") != string::npos ||
            order->statusMsg.find("闭市") != string::npos ||
            order->statusMsg.find("最小单位的倍数") != string::npos) {
            order->status = ORDER_STATUS::ERROR;
        }
        this->account->onOrder(order);
        logi("{} OnRtnOrder {} {} traded:{}/{} status:{} msg:{}", id, order->orderRef, order->symbol, order->tradedVolume,
             order->totalVolume, order->statusStr, order->statusMsg);
    }

    ///成交通知
    void OnRtnTrade(CThostFtdcTradeField *pTrade) override{
        int orderRef=atoi(pTrade->OrderRef);
        if (!account->orderMap.count(orderRef)>0)
            return;

        long tsc=Context::get().tn.rdtsc();
        Order *order = account->orderMap[orderRef];
        Trade *trade = new Trade();
        trade->orderRef = order->orderRef;
        trade->tradeId = pTrade->TradeID;
        trade->tradingDay = pTrade->TradingDay;
        trade->tradeDate = pTrade->TradeDate;
        trade->tradeTime = pTrade->TradeTime;
        trade->symbol = pTrade->InstrumentID;
        trade->direction = order->direction;
        //trade->direction_s = enum_string(order->direction_enum);
        trade->tradeType = order->offset;
        //trade->offset= enum_string(order->offset_enum);
        trade->tradedVolume = pTrade->Volume;
        trade->tradedPrice = pTrade->Price;
        trade->exchange = pTrade->ExchangeID;
        trade->updateTsc=tsc;

        this->account->onTrade(trade);
        logi("{} OnRtnTrade {} {} {} {} traded:{}", id, trade->orderRef, trade->symbol, magic_enum::enum_name(trade->tradeType),
             magic_enum::enum_name(trade->direction), pTrade->Volume);


    }

    ///报单录入错误回报
    void OnErrRtnOrderInsert(CThostFtdcInputOrderField *pInputOrder, CThostFtdcRspInfoField *pRspInfo) override{
        int orderRef=atoi(pInputOrder->OrderRef);
        loge("OnErrRtnOrderInsert Error! orderRef:{} {}", pInputOrder->OrderRef,pRspInfo->ErrorMsg);
        if (account->orderMap.count(orderRef)>0) {
            Order *order = account->orderMap[orderRef];
            order->status = ORDER_STATUS::ERROR;
            order->statusMsg = pRspInfo->ErrorMsg;
            this->account->onOrder(order);
        }
    }

    ///报单操作错误回报
    void OnErrRtnOrderAction(CThostFtdcOrderActionField *pOrderAction, CThostFtdcRspInfoField *pRspInfo) override{
        //CTP检测失败时触发
        loge("OnErrRtnOrderAction");
    }

    ///报单录入请求响应
    void OnRspOrderInsert(CThostFtdcInputOrderField *pInputOrder, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                          bool bIsLast) override{
        //CTP检测失败时触发
        int orderRef=atoi(pInputOrder->OrderRef);
        loge("OnRspOrderInsert Error! orderRef:{} {}", pInputOrder->OrderRef,pRspInfo->ErrorMsg);
        if (account->orderMap.count(orderRef)>0) {
            Order *order = account->orderMap[orderRef];
            order->status = ORDER_STATUS::ERROR;
            order->statusMsg = pRspInfo->ErrorMsg;
            this->account->onOrder(order);

        }
    }

    ///报单操作请求响应
    void OnRspOrderAction(CThostFtdcInputOrderActionField *pInputOrderAction, CThostFtdcRspInfoField *pRspInfo,
                          int nRequestID, bool bIsLast) override {
        loge("OnRspOrderAction {}",pRspInfo->ErrorMsg);
    }

    ///请求查询报单响应
    void OnRspQryOrder(CThostFtdcOrderField *pOrder, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                       bool bIsLast) override{
        //todo 可用来查询挂单
    }

    ///请求查询成交响应
    void OnRspQryTrade(CThostFtdcTradeField *pTrade, CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {};

    ///请求查询投资者持仓响应
    void OnRspQryInvestorPosition(CThostFtdcInvestorPositionField *pInvestorPosition, CThostFtdcRspInfoField *pRspInfo,
                                  int nRequestID, bool bIsLast) override{

        if(pInvestorPosition!=NULL){
            //持仓更新
            string symbol = pInvestorPosition->InstrumentID;
            POS_DIRECTION direction =
                    pInvestorPosition->PosiDirection == THOST_FTDC_PD_Long ? POS_DIRECTION::LONG : POS_DIRECTION::SHORT;
            logi("{} OnRspQryInvestorPosition {} {} pos:{}", id, symbol, magic_enum::enum_name(direction),
                 pInvestorPosition->Position);
            string key = symbol + "-" + to_string(direction);
            if (!account->accoPositionMap.count(key)>0) {
                Position *position = new Position(pInvestorPosition->InstrumentID, direction);
                account->accoPositionMap[key] = position;
            }
            Position *position = account->accoPositionMap[key];
            position->pos += pInvestorPosition->Position;
            position->tdPos += pInvestorPosition->TodayPosition;
            position->ydPos = position->pos - position->tdPos;
        }

        if (bIsLast) {
            logi("OnRspQryInvestorPosition Finish!");
        }
    }

    ///请求查询资金账户响应
    void OnRspQryTradingAccount(CThostFtdcTradingAccountField *pTradingAccount, CThostFtdcRspInfoField *pRspInfo,
                                int nRequestID, bool bIsLast) override{
        if(pTradingAccount !=NULL){
            logi("{} 资金信息： 静态:{}\t动态:{}\t平仓:{}\t持仓:{}\t手续费:{}\t入金:{}\t可用:{}\t保证金:{}",
                 id, pTradingAccount->PreBalance,
                 pTradingAccount->Balance,
                 pTradingAccount->CloseProfit,
                 pTradingAccount->PositionProfit,
                 pTradingAccount->Commission,
                 pTradingAccount->Deposit,
                 pTradingAccount->Available,
                 pTradingAccount->CurrMargin);
            account->acctInfo->balance = pTradingAccount->Balance;
            account->acctInfo->closeProfit = pTradingAccount->CloseProfit;
            account->acctInfo->positionProfit = pTradingAccount->PositionProfit;
            account->acctInfo->commission = pTradingAccount->Commission;
            account->acctInfo->deposit = pTradingAccount->Deposit;
            account->acctInfo->available = pTradingAccount->Available;
            account->acctInfo->margin = pTradingAccount->CurrMargin;

        }
        if (bIsLast) {
            logi("OnRspQryTradingAccount Finished!");

            timer.delay(1000, [this]() {
                //查询合约
                CThostFtdcQryInstrumentField req = {0};
                int ret = m_pUserApi->ReqQryInstrument(&req, this->nRequestID++);
                logi("{} ReqQryInstrument ret={},{}", id, ret, qryRetMsgMap[ret]);
            });
        }
    }

    ///投资者结算结果确认响应
    void OnRspSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField *pSettlementInfoConfirm,
                                    CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override{
        logi("{} OnRspSettlementInfoConfirm", id);
        if (bIsLast) {
            timer.delay(500, [this]() {
                //查询资金账户
                CThostFtdcQryTradingAccountField a = {0};
                strcpy(a.BrokerID, this->brokerId.c_str());
                strcpy(a.InvestorID, account->acctConf->user.c_str());
                //strcpy(a.CurrencyID, "CNY");
                int ret = m_pUserApi->ReqQryTradingAccount(&a, this->nRequestID++);
                logi("{} ReqQryTradingAccount ret={},{}", id, ret, qryRetMsgMap[ret]);
            });
        }
    }

    ///用户口令更新请求响应
    void
    OnRspUserPasswordUpdate(CThostFtdcUserPasswordUpdateField *pUserPasswordUpdate, CThostFtdcRspInfoField *pRspInfo,
                            int nRequestID, bool bIsLast) {};

    ///请求查询合约手续费率响应
    void OnRspQryInstrumentCommissionRate(CThostFtdcInstrumentCommissionRateField *pInstrumentCommissionRate,
                                          CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override{
        //todo 查询手续费率
    }

    ///请求查询合约响应
    void OnRspQryInstrument(CThostFtdcInstrumentField *pInstrument, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                            bool bIsLast) override{

        if(pInstrument!=NULL){
            if (strlen(pInstrument->InstrumentID) <= 10) {
                //过滤掉组合合约
                Contract *contract;
                if (account->contractMap.count(pInstrument->InstrumentID)>0)
                    contract = account->contractMap[pInstrument->InstrumentID];
                else {
                    contract = new Contract;
                    account->contractMap[pInstrument->InstrumentID] = contract;
                }
                contract->symbol = pInstrument->InstrumentID;
                contract->exchange = pInstrument->ExchangeID;
                contract->name = pInstrument->InstrumentName;
                contract->multiple = pInstrument->VolumeMultiple;
                contract->priceTick = pInstrument->PriceTick;
                contract->strikePrice = pInstrument->StrikePrice;
                contract->type = pInstrument->ProductClass;
                contract->expiryDate = pInstrument->ExpireDate;
                //contract->posDateType = pInstrument->PositionDateType;
                contract->underlyingSymbol = pInstrument->UnderlyingInstrID;//针对商品期权
            }
        }

        if (bIsLast) {
            logi("{} OnRspQryInstrument Finish,cont:{}", id, account->contractMap.size());
            this->queue->push(Event{EvType::CONTRACT});

            timer.delay(1000, [this]() {
                //查询持仓
                CThostFtdcQryInvestorPositionField a = {0};
                strcpy(a.BrokerID, this->brokerId.c_str());
                strcpy(a.InvestorID, account->acctConf->user.c_str());
                int ret = m_pUserApi->ReqQryInvestorPosition(&a, this->nRequestID++);
                logi("{} ReqQryInvestorPosition ret={},{}", id, ret, qryRetMsgMap[ret]);
            });
        }

    }

    ///请求查询行情响应
    void OnRspQryDepthMarketData(CThostFtdcDepthMarketDataField *pDepthMarketData, CThostFtdcRspInfoField *pRspInfo,
                                 int nRequestID, bool bIsLast) {};

    ///请求查询报单手续费响应
    void OnRspQryInstrumentOrderCommRate(CThostFtdcInstrumentOrderCommRateField *pInstrumentOrderCommRate,
                                         CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {};

    ///合约交易状态通知
    void OnRtnInstrumentStatus(CThostFtdcInstrumentStatusField *pInstrumentStatus) {};
};


#endif //TRADECORE_CTPTDAPI_H
