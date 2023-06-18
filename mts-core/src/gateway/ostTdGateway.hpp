//
// Created by 段晴 on 2022/2/15.
//
#pragma once


#include "ost/UTApi.h"
#include "gateway.h"
#include "lockFreeQueue.hpp"
#include "common/timer.hpp"
#include "define.h"
#include "semaphore.h"
#include "acct.h"

#include <dlfcn.h>
#include "common/timer.hpp"
#include "magic_enum.hpp"

class OstTdGateway : public CUTSpi, public TdGateway {

private:
    static inline int nRequestID =0 ;
    static inline map<TUTOrderStatusType, ORDER_STATUS> statusMap={
            {UT_OST_AllTraded,             ORDER_STATUS::ALLTRADED},
            {UT_OST_PartTradedQueueing,    ORDER_STATUS::QUEUEING},
            {UT_OST_PartTradedNotQueueing, ORDER_STATUS::NOTQUEUEING},
            {UT_OST_NoTradeQueueing,       ORDER_STATUS::QUEUEING},
            {UT_OST_NoTradeNotQueueing,    ORDER_STATUS::NOTQUEUEING},
            {UT_OST_Canceled,              ORDER_STATUS::CANCELLED},
            {UT_OST_Unknown,               ORDER_STATUS::UNKNOWN}
    };
    static inline map<TUTExchangeIDType,string> exgMap={
            {UT_EXG_SSE,"SSE"},
            {UT_EXG_SZSE,"SZE"},
            {UT_EXG_SHFE,"SHFE"},
            {UT_EXG_CFFEX,"CFFEX"},
            {UT_EXG_DCE,"DCE"},
            {UT_EXG_CZCE,"CZCE"},
            {UT_EXG_INE,"INE"},
            {UT_EXG_HKEX,"HK"}
    };
    static inline map<string,TUTExchangeIDType> reExgMap={
            {"SSE",UT_EXG_SSE},
            {"SZE",UT_EXG_SZSE},
            {"SHFE",UT_EXG_SHFE},
            {"CFFEX",UT_EXG_CFFEX},
            {"DCE",UT_EXG_DCE},
            {"CZCE",UT_EXG_CZCE},
            {"INE",UT_EXG_INE},
            {"HK",UT_EXG_HKEX}
    };

    static inline map<int, string> qryRetMsgMap ={
            {0,  "成功"},
            {-1, "网络连接失败"},
            {-2, "未处理请求超过许可数"},
            {-3, "每秒发送请求数超过许可数"}

    };
    //map<string,TUTExchangeIDType> exgReverseMap;

    string id;
    CUTApi *m_pUserApi;
    Acct *pAcct;
    LockFreeQueue<Event> *queue;
    int frontId = 0;// 前置机编号
    long long sessionId = 0;// 会话编号
    vector<Position> tmpPositons;
    vector<Trade> tmpTrades;
    //map<int, Order *> orderMap;
    Timer timer;

    Semaphore  semaphore={0};


    void Run(){
        const char *address = this->pAcct->acctConf->tdAddress.c_str();
        m_pUserApi->RegisterFront(const_cast<char *>(address));
        m_pUserApi->Init();
        logi("{} ctp connecting...", id);
        m_pUserApi->Join();
    }

public:
    OstTdGateway(Acct *acct) : pAcct(acct) {
        this->id = pAcct->id;
        this->queue = pAcct->tdQueue;

//        for(auto &[key,value]:exgMap){
//            exgReverseMap[value]=key;
//        }
    }

    ~OstTdGateway() {}

    int connect() override {
        void *handle = dlopen("lib/ost/libutapi.so", RTLD_LAZY);
        if(handle == nullptr){
            logi("load libutapi.so fail [{}] [{}]", errno, strerror(errno));
            return -1;
        }

        typedef CUTApi *(*CreateTdApiFunc)(const char *,int);
        CreateTdApiFunc pfnCreateFtdcTdApiFunc = (CreateTdApiFunc) dlsym(handle,
                                                                         "_ZN6CUTApi9CreateApiEPKci");
        if (pfnCreateFtdcTdApiFunc == nullptr) {
            logi("load libutapi.so fail [{}] [{}]", errno, strerror(errno));
            return -1;
        }
        m_pUserApi = pfnCreateFtdcTdApiFunc("./flow", pAcct->cpuNumTd);
        //创建api;将参数nCPUID设置为需要绑定的CPU,可开启极速模式
        //如果同一进程内创建多个api，参数pszFlowPath必须设置为不同的路径
        //m_pUserApi = CUTApi::CreateApi();
        //创建并注册spi
        m_pUserApi->RegisterSpi(this);
        //订阅私有流;这个函数也可以在登录成功后的任何地方调用
        m_pUserApi->SubscribePrivateTopic(UT_TERT_QUICK);
        //暂时没有公有流
        //api->SubscribePublicTopic(UT_TERT_QUICK);

        //thread t(&OstTdGateway::Run, this);
        thread t([this](){
            this->Run();
        });
        t.detach();

        thread t1([this](){
            logi("start init  accout");

            this->semaphore.wait();
            //查询账户信息
            CUTQryTradingAccountField QryTradingAccount={0};
            int ret = this->m_pUserApi->ReqQryTradingAccount(&QryTradingAccount, this->nRequestID++);
            logi("ReqQryTradingAccount  ret={}", ret);

            this->semaphore.wait();
            std::this_thread::sleep_for(std::chrono::milliseconds(1000));
            //查询合约
            CUTQryInstrumentField QryInstrument ={0};
            ret = this->m_pUserApi->ReqQryInstrument(&QryInstrument, this->nRequestID++);
            logi("{} ReqQryInstrument ret={},{}", id, ret, qryRetMsgMap[ret]);


            this->semaphore.wait();
            std::this_thread::sleep_for(std::chrono::milliseconds(1000));
            //查询持仓
            CUTQryInvestorPositionField a={0};
            ret = this->m_pUserApi->ReqQryInvestorPosition(&a, this->nRequestID++);
            logi("{} ReqQryInvestorPosition ret={},{}", id, ret, qryRetMsgMap[ret]);

            this->semaphore.wait();
            std::this_thread::sleep_for(std::chrono::milliseconds(1000));
            //查询成交
            CUTQryTradeField QryTrade={0};
            ret = this->m_pUserApi->ReqQryTrade(&QryTrade, +this->nRequestID++);
            logi("ReqQryTrade ret={},{}",ret,qryRetMsgMap[ret]);

            this->semaphore.wait();
            std::this_thread::sleep_for(std::chrono::milliseconds(1000));
            //查询报单
            CUTQryOrderField QryOrder={0};
            ret =this->m_pUserApi->ReqQryOrder(&QryOrder, this->nRequestID++);
            logi("ReqQryOrder ret={},{}",ret,qryRetMsgMap[ret]);

            semaphore.wait();
            logi("======账户[{}]准备就绪=======",this->id);
        });
        t1.detach();

        return 0;
    }

    int disconnect() override  {
        if(this->connected== false)
            return 0;
        logw("{} OstTdGateway discounnect ", id);
        this->connected= false;
        this->pAcct->acctInfo->tdStatus= false;
        try {
            if (m_pUserApi != nullptr) {
                connected = false;
                m_pUserApi->Release();
                m_pUserApi = nullptr;
            }
        } catch (exception ex) {
            loge("{} discounnect fail ,{}", id, ex.what());
        }
        return 0;
    }

    bool insertOrder(Order *order) override {
        long tsc=Context::get().tn.rdtsc();

        CUTInputOrderField pInputOrderField={0};
        strcpy(pInputOrderField.InvestorID, pAcct->acctConf->user.c_str());
        strcpy(pInputOrderField.InstrumentID, order->symbol.c_str());
        pInputOrderField.ExchangeID = OstTdGateway::reExgMap[order->exchange];
        //OrderRef必须设置,同一会话内必须递增,可以不连续
        pInputOrderField.OrderRef=order->orderRef;
        pInputOrderField.OrderPriceType = UT_OPT_LimitPrice;

        //股票,基金，债券买:HedgeFlag = UT_HF_Speculation,Direction = UT_D_Buy,OffsetFlag = UT_OF_Open
        //股票, 基金，债券卖:HedgeFlag = UT_HF_Speculation, Direction = UT_D_Sell, OffsetFlag = UT_OF_Close
        //债券逆回购:HedgeFlag = UT_HF_Speculation, Direction = UT_D_Sell, OffsetFlag = UT_OF_Open
        //ETF申购:HedgeFlag = UT_HF_Redemption,Direction = UT_D_Buy,OffsetFlag = UT_OF_Open
        //ETF赎回:HedgeFlag = UT_HF_Redemption,Direction = UT_D_Sell,OffsetFlag = UT_OF_Close
        pInputOrderField.HedgeFlag = UT_HF_Speculation;
        pInputOrderField.Direction = order->direction== TRADE_DIRECTION::BUY?UT_D_Buy:UT_D_Sell;
        pInputOrderField.OffsetFlag = order->offset == OFFSET::OPEN? UT_OF_Open:UT_OF_Close;
        pInputOrderField.LimitPrice = order->price;
        pInputOrderField.VolumeTotalOriginal = order->totalVolume;
        pInputOrderField.TimeCondition = UT_TC_GFD;
        pInputOrderField.VolumeCondition = UT_VC_AV;
        pInputOrderField.MinVolume = 0;
        pInputOrderField.ContingentCondition = UT_CC_Immediately;
        pInputOrderField.StopPrice = 0;
        pInputOrderField.IsAutoSuspend = 0;

        int ret = this->m_pUserApi->ReqOrderInsert(&pInputOrderField, this->nRequestID++);
        if (ret != 0) {
            order->status = ORDER_STATUS::ERROR;
            order->statusMsg = "insert order fail";
        } else {
            pAcct->orderMap[pInputOrderField.OrderRef] = order;
        }
        this->queue->push(Event{EvType::ORDER, tsc,order});
        logi("InsertOrder orderRef:{} {} {} {} price:{} volume:{}  ret={}",
             order->orderRef, order->symbol, order->offset_s, order->direction_s, order->price, order->totalVolume,ret);
        return ret==0;
    }

    void cancelOrder(Action *action) override {
        //ETF申赎不可撤单
        if (!this->isConnected()) {
            loge("td not connected");
            return;
        }
        CUTInputOrderActionField req = {0};
        req.ActionFlag = UT_AF_Delete;
        req.OrderRef= action->orderRef;
        if(action->frontId>0){
            req.FrontID=action->frontId;
            req.SessionID=action->sessionId;
        }else{
            req.FrontID = frontId;
            req.SessionID = sessionId;
        }

        int ret = m_pUserApi->ReqOrderAction(&req, this->nRequestID++);
        logi("ReqOrderAction orderRef:{} ret={}",  action->orderRef, ret);
    }


    ///当客户端与交易后台建立起通信连接时（还未登录前），该方法被调用。
    void OnFrontConnected() override {
        fmtlog::setThreadName("TdGateway");
        logi("OnFrontConnected!");

        //登录请求
        CUTReqLoginField reqLoginField;
        memset(&reqLoginField, 0, sizeof(reqLoginField));
        strcpy(reqLoginField.UserID, pAcct->acctConf->user.c_str());
        strcpy(reqLoginField.Password, pAcct->acctConf->pwd.c_str());
        //strcpy(reqLoginField.UserProductInfo, "xxx");
        int ret = m_pUserApi->ReqLogin(&reqLoginField, this->nRequestID++);
        logi("ReqLogin! ret={}", ret);


    }

    ///当客户端与交易后台通信连接断开时，该方法被调用。当发生这个情况后，API会自动重新连接，客户端可不做处理。
    void OnFrontDisconnected(int nReason) override {
        loge("OnFrontDisconnected:{}", nReason);
    }

    ///错误应答
    void OnRspError(CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override {
        loge("OnRspError: Error! [{}] [{}]", pRspInfo->ErrorID, utf8(pRspInfo->ErrorMsg));

    }

    ///登录请求响应
    void OnRspLogin(CUTRspLoginField *pRspUserLogin, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override {

        logi("{} OnRspLogin", id);
        if (pRspInfo->ErrorID == 0) {
            this->sessionId = pRspUserLogin->SessionID;
            this->frontId = pRspUserLogin->FrontID;
            this->connected = true;
            this->tradingDay = to_string(pRspUserLogin->TradingDay);
            logi("{} 交易接口登录成功,交易日={},frontId={},sessionId={}", id, tradingDay,frontId,sessionId);
            semaphore.signal();
            this->pAcct->acctInfo->tdStatus= true;
        } else {
            loge("{} 交易接口登录成功失败! ErrorID:{},ErrorMsg:{}", id, pRspInfo->ErrorID, utf8(pRspInfo->ErrorMsg));
            this->connected = false;
            this->pAcct->acctInfo->tdStatus= false;
        }
        this->queue->push(Event{EvType::STATUS,0});
    }

    ///请求查询合约响应
    void OnRspQryInstrument(CUTInstrumentField *pInstrument, CUTRspInfoField *pRspInfo, int nRequestID,
                            bool bIsLast) override{

        if (pInstrument != NULL) {
            //ProductID:股票--ASTOCK,基金--ETF,债券--BOND,指数--INDEX
            //printf("Instrument:[%s] [%d] [%s]\n", pInstrument->InstrumentID, pInstrument->ExchangeID, pInstrument->InstrumentName);
            if (strlen(pInstrument->InstrumentID) <= 10) {
                //过滤掉组合合约
                Contract *contract;
                if (pAcct->contractMap.count(pInstrument->InstrumentID) > 0)
                    contract = pAcct->contractMap[pInstrument->InstrumentID];
                else {
                    contract = new Contract;
                    pAcct->contractMap[pInstrument->InstrumentID] = contract;
                }
                contract->symbol = pInstrument->InstrumentID;
                contract->exchange = exgMap[pInstrument->ExchangeID];
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
            logi("{} OnRspQryInstrument Finish,cont:{}", id, pAcct->contractMap.size());
            this->queue->push(Event{EvType::CONTRACT});
            semaphore.signal();

        }
    }

    ///请求查询资金响应
    void OnRspQryTradingAccount(CUTTradingAccountField *pTradingAccount, CUTRspInfoField *pRspInfo, int nRequestID,
                                bool bIsLast) override{
        logi("{} OnRspQryTradingAccount", id);
        if (bIsLast) {
            logi("{} 资金信息： 静态:{}\t动态:{}\t平仓:{}\t持仓:{}\t手续费:{}\t入金:{}\t可用:{}\t保证金:{}",
                 id, pTradingAccount->PreBalance,
                 pTradingAccount->Balance,
                 pTradingAccount->CloseProfit,
                 pTradingAccount->PositionProfit,
                 pTradingAccount->Commission,
                 pTradingAccount->Deposit,
                 pTradingAccount->Available,
                 pTradingAccount->CurrMargin);
            pAcct->acctInfo->balance = pTradingAccount->Balance;
            pAcct->acctInfo->closeProfit = pTradingAccount->CloseProfit;
            pAcct->acctInfo->positionProfit = pTradingAccount->PositionProfit;
            pAcct->acctInfo->commission = pTradingAccount->Commission;
            pAcct->acctInfo->deposit = pTradingAccount->Deposit;
            pAcct->acctInfo->available = pTradingAccount->Available;
            pAcct->acctInfo->margin = pTradingAccount->CurrMargin;

            semaphore.signal();

        }

    }

    ///请求查询持仓响应
    void
    OnRspQryInvestorPosition(CUTInvestorPositionField *pInvestorPosition, CUTRspInfoField *pRspInfo, int nRequestID,
                             bool bIsLast) override{

        if (pInvestorPosition != NULL){
            string symbol = pInvestorPosition->InstrumentID;
            POS_DIRECTION direction =
                    pInvestorPosition->PosiDirection == UT_PD_Long ? POS_DIRECTION::LONG : POS_DIRECTION::SHORT;
            logi("{} OnRspQryInvestorPosition {} {} pos:{},tdPos:{}", id, symbol, enum_string(direction),
                 pInvestorPosition->Position,pInvestorPosition->TodayPosition);

            Position *position = pAcct->getPosition(symbol, direction);
            position->pos += pInvestorPosition->Position;
            position->tdPos += pInvestorPosition->TodayPosition;
            position->ydPos = position->pos - position->tdPos;


//        if (0 == strcmp(pInvestorPosition->InstrumentID, "SHRQ88") ||
//            0 == strcmp(pInvestorPosition->InstrumentID, "SZRQ88")) {
//            printf("InvestorPosition（历史逆回购标准券,不可平):[%s] [%s] [%d]\n", pInvestorPosition->InvestorID,
//                   pInvestorPosition->InstrumentID, pInvestorPosition->Position);
//        } else if (pInvestorPosition->PosiDirection == UT_PD_Short) {
//            printf("InvestorPosition（今日逆回购持仓,不可平):[%s] [%s] [%d]\n", pInvestorPosition->InvestorID,
//                   pInvestorPosition->InstrumentID, pInvestorPosition->Position);
//        } else if (pInvestorPosition->PosiDirection == UT_PD_Long) {
//            printf("InvestorPosition（非逆回购持仓）:[%s] [%s] [%d]\n", pInvestorPosition->InvestorID,
//                   pInvestorPosition->InstrumentID, pInvestorPosition->Position);
//        }
        }


        if (bIsLast) {
            //更新到账户中
            logi("OnRspQryInvestorPosition Finish! count:{}", pAcct->accoPositionMap.size());
            semaphore.signal();
        }


    }

    ///请求查询报单响应
    void OnRspQryOrder(CUTOrderField *pOrder, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override{
        if (bIsLast) {
            if (pRspInfo && pRspInfo->ErrorID) {
                loge("OnRspQryOrder: Error! [{}] [{}]", pRspInfo->ErrorID, utf8(pRspInfo->ErrorMsg));
            } else {
                logi("OnRspQryOrder: OK");
            }
            semaphore.signal();
        }
    }

    ///请求查询成交响应
    void OnRspQryTrade(CUTTradeField *pTrade, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override{
        if (bIsLast) {
            if (pRspInfo && pRspInfo->ErrorID) {
                loge("OnRspQryTrade: Error! [{}] [{}]", pRspInfo->ErrorID, utf8(pRspInfo->ErrorMsg));
            } else {
                logi("OnRspQryTrade: OK");
            }
            semaphore.signal();
        }
    }

    ///报单录入请求错误时的响应;正确时不会产生该响应,而是回调OnRtnOrder
    void
    OnRspOrderInsert(CUTInputOrderField *pInputOrder, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override{
        //检测失败时触发
        long tsc=Context::get().tn.rdtsc();
        if (pAcct->orderMap.count(pInputOrder->OrderRef) > 0) {
            Order *order = pAcct->orderMap[pInputOrder->OrderRef];
            order->status = ORDER_STATUS::ERROR;
            order->statusMsg = utf8(pRspInfo->ErrorMsg);
            order->updateTsc = tsc;
            this->queue->push(Event{EvType::ORDER, tsc,order});
        }
        loge("OnRspOrderInsert\t Error! orderRef:{}  {}",pInputOrder->OrderRef, utf8(pRspInfo->ErrorMsg));

    }

    ///报单通知
    void OnRtnOrder(CUTOrderField *pOrder) override{
        if (!pAcct->orderMap.count(pOrder->OrderRef) > 0)
            return;
        long tsc=Context::get().tn.rdtsc();
        Order *order = pAcct->orderMap[pOrder->OrderRef];
        //order->tradedVolume = order.TradedVolume;
        order->orderSysId = pOrder->OrderSysID;
        order->tradedVolume = pOrder->VolumeTraded;
        order->status = statusMap[pOrder->OrderStatus];
        order->updateTsc=tsc;
        //order->statusMsg = Util::g2u(pOrder->StatusMsg);
        //order->updateTime = pOrder->UpdateTime;
        this->queue->push(Event{EvType::ORDER, tsc,order});
        //错单识别
        logi("OnRtnOrder\t{} {} {} {}  traded:{}/{} status:{} msg:{}", order->orderRef, order->symbol, order->offset_s,order->direction_s,order->tradedVolume,
             order->totalVolume, magic_enum::enum_name(order->status), order->statusMsg);
    }

    ///报单操作错误，被UT打回时的响应;正确时不会产生该响应,而是回调OnRtnOrder
    void OnRspOrderAction(CUTInputOrderActionField *pInputOrderAction, CUTRspInfoField *pRspInfo, int nRequestID,
                          bool bIsLast) override{
        loge("OnRspOrderAction\t Error! [{}] [{}] [{}]\n", pInputOrderAction->OrderRef,pRspInfo->ErrorID, utf8(pRspInfo->ErrorMsg));
    }

    ///报单操作错误，被交易所打回时的回报
    void OnErrRtnOrderAction(CUTOrderActionField *pOrderAction) override{
        loge("OnErrRtnOrderAction\t [{}] [{}]", pOrderAction->OrderRef, pOrderAction->ExchangeErrorID);
    }


    ///成交通知
    void OnRtnTrade(CUTTradeField *pTrade) override{
        if (!pAcct->orderMap.count(pTrade->OrderRef) > 0)
            return;
        long tsc=Context::get().tn.rdtsc();
        Order *order = pAcct->orderMap[pTrade->OrderRef];
        Trade *trade = new Trade();
        trade->orderRef = order->orderRef;
        trade->tradeId = pTrade->TradeID;
        trade->tradingDay = pTrade->TradingDay;
        trade->tradeDate = pTrade->TradeDate;
        trade->tradeTime = pTrade->TradeTime;
        trade->symbol = pTrade->InstrumentID;
        trade->direction = order->direction;
        trade->offset = order->offset;
        trade->volume = pTrade->Volume;
        trade->price = pTrade->Price;
        trade->exchange = pTrade->ExchangeID;
        trade->updateTsc=tsc;
        order->realTradedVolume+=trade->volume;

        this->queue->push(Event{EvType::TRADE, tsc,trade});

        logi("OnRtnTrade\t{} {} {} {} traded:{} price:{}", trade->orderRef, trade->symbol, enum_string(trade->offset),
             enum_string(trade->direction), pTrade->Volume,pTrade->Price);
    }

};
