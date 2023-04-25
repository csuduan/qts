//
// Created by Administrator on 2020/7/2.
//

#include <thread>
#include <cstring>
#include "CtpTdGateway.h"
#include <signal.h>
#include <queue>
#include  <functional>
#include "magic_enum.hpp"
#include <iconv.h>


int CtpTdGateway::nRequestID = 0;
map<TThostFtdcOrderStatusType, ORDER_STATUS> CtpTdGateway::statusMap = {
        {THOST_FTDC_OST_AllTraded,             ORDER_STATUS::ALLTRADED},
        {THOST_FTDC_OST_PartTradedQueueing,    ORDER_STATUS::QUEUEING},
        {THOST_FTDC_OST_PartTradedNotQueueing, ORDER_STATUS::NOTQUEUEING},
        {THOST_FTDC_OST_NoTradeQueueing,       ORDER_STATUS::QUEUEING},
        {THOST_FTDC_OST_NoTradeNotQueueing,    ORDER_STATUS::NOTQUEUEING},
        {THOST_FTDC_OST_Canceled,              ORDER_STATUS::CANCELLED},
        {THOST_FTDC_OST_Unknown,               ORDER_STATUS::UNKNOWN}
};
map<int, string> CtpTdGateway::qryRetMsgMap = {
        {0,  "成功"},
        {-1, "网络连接失败"},
        {-2, "未处理请求超过许可数"},
        {-3, "每秒发送请求数超过许可数"}

};


int CtpTdGateway::connect() {
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


int CtpTdGateway::disconnect() {
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

void CtpTdGateway::Run() {
    const char *address = this->loginInfo.tdAddress.c_str();
    m_pUserApi->RegisterFront(const_cast<char *>(address));
    m_pUserApi->Init();
    m_pUserApi->Join();
}

void CtpTdGateway::reqQryPosition() {
    if (!this->isConnected()) {
        loge("Not connected");
        return;
    }
    tmpPositons.clear();
    CThostFtdcQryInvestorPositionField cThostFtdcQryInvestorPositionField;
    strcpy(cThostFtdcQryInvestorPositionField.BrokerID, loginInfo.brokerId.c_str());
    strcpy(cThostFtdcQryInvestorPositionField.InvestorID, loginInfo.userId.c_str());
    int ret = this->m_pUserApi->ReqQryInvestorPosition(&cThostFtdcQryInvestorPositionField, this->nRequestID++);
    logi("查询持仓,ret={}",  ret);

}

bool CtpTdGateway::insertOrder(Order *order) {
    CThostFtdcInputOrderField req = {0};
    strcpy(req.BrokerID, loginInfo.brokerId.c_str());
    strcpy(req.InvestorID, loginInfo.userId.c_str());
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
         order->orderRef, order->symbol, order->offset_s, order->direction_s, order->price, order->totalVolume,
         ret);
    if (ret != 0) {
        order->status = ORDER_STATUS::ERROR;
        order->statusMsg = "insert order fail";
    } else {
        account->orderMap[order->orderRef] = order;
    }
    return  ret==0;
}


void CtpTdGateway::cancelOrder(Action *order) {
    if (!this->isConnected()) {
        loge("Not connected");
        return;
    }
    CThostFtdcInputOrderActionField req = {0};
    req.ActionFlag = THOST_FTDC_AF_Delete;

    strcpy(req.BrokerID, loginInfo.brokerId.c_str());
    strcpy(req.InvestorID, loginInfo.userId.c_str());
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

///用户口令更新请求
void CtpTdGateway::reqUserPasswordUpdate() {
}

void CtpTdGateway::OnFrontConnected() {
    fmtlog::setThreadName("TdGateway");
    logi("OnFrontConnected");

    //认证通过后再登录
    CThostFtdcReqAuthenticateField req = {0};
    strcpy(req.BrokerID, loginInfo.brokerId.c_str());
    strcpy(req.UserID, loginInfo.userId.c_str());
    strcpy(req.AuthCode, loginInfo.authCode.c_str());
    strcpy(req.AppID, loginInfo.appId.c_str());
    int ret = m_pUserApi->ReqAuthenticate(&req, this->nRequestID++);
    logi("ReqAuthenticate ret={}", ret);

}

void CtpTdGateway::OnFrontDisconnected(int nReason) {
    logi("OnFrontDisconnected");
}

void
CtpTdGateway::OnRspAuthenticate(CThostFtdcRspAuthenticateField *pRspAuthenticateField, CThostFtdcRspInfoField *pRspInfo,
                                int nRequestID, bool bIsLast) {
    logi("OnRspAuthenticate ");
    if (pRspInfo->ErrorID == 0) {
        CThostFtdcReqUserLoginField reqUserLogin = {0};
        strcpy(reqUserLogin.BrokerID, loginInfo.brokerId.c_str());
        strcpy(reqUserLogin.UserID, loginInfo.userId.c_str());
        strcpy(reqUserLogin.Password, loginInfo.password.c_str());
        // 发出登陆请求
        int ret = m_pUserApi->ReqUserLogin(&reqUserLogin, nRequestID++);
        logi("ReqUserLogin ret={}", ret);
    } else {
        loge("认证失败 {}", pRspInfo->ErrorMsg);
    }


}

void CtpTdGateway::OnRspUserLogin(CThostFtdcRspUserLoginField *pRspUserLogin, CThostFtdcRspInfoField *pRspInfo,
                                  int nRequestID, bool bIsLast) {
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
            strcpy(req.BrokerID, loginInfo.brokerId.c_str());
            strcpy(req.InvestorID, loginInfo.userId.c_str());
            int ret = m_pUserApi->ReqSettlementInfoConfirm(&req, this->nRequestID++);
            logi("ReqSettlementInfoConfirm ret={},{}", ret, qryRetMsgMap[ret]);
        });


    } else {
        loge("交易接口登录成功失败! ErrorID:{},ErrorMsg:{}",  pRspInfo->ErrorID, pRspInfo->ErrorMsg);
        this->connected = false;
    }


}

void
CtpTdGateway::OnRspUserLogout(CThostFtdcUserLogoutField *pUserLogout, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                              bool bIsLast) {
    logw("OnRspUserLogout");
    this->connected = false;
}

void CtpTdGateway::OnRspError(CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    loge("OnRspError");
}

void CtpTdGateway::OnRtnOrder(CThostFtdcOrderField *pOrder) {
    int orderRef=atoi(pOrder->OrderRef);
    if (!account->orderMap.contains(orderRef))
        return;
    long tsc=Context::get().tn.rdtsc();
    Order *order = account->orderMap[orderRef];
    //order->tradedVolume = order.TradedVolume;
    order->orderSysId = pOrder->OrderSysID;
    order->tradedVolume = pOrder->VolumeTraded;
    order->status = statusMap[pOrder->OrderStatus];
    order->status_s= enum_string(order->status);
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
    this->queue->push(Event{EvType::ORDER, tsc,order});
    logi("{} OnRtnOrder {} {} traded:{}/{} status:{} msg:{}", id, order->orderRef, order->symbol, order->tradedVolume,
         order->totalVolume, order->status_s, order->statusMsg);
}

void CtpTdGateway::OnRtnTrade(CThostFtdcTradeField *pTrade) {
    int orderRef=atoi(pTrade->OrderRef);
    if (!account->orderMap.contains(orderRef))
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
    trade->direction_s = enum_string(order->direction);
    trade->offset = order->offset;
    trade->offset_s= enum_string(order->offset);
    trade->volume = pTrade->Volume;
    trade->price = pTrade->Price;
    trade->exchange = pTrade->ExchangeID;
    trade->updateTsc=tsc;

    this->queue->push(Event{EvType::TRADE, tsc,trade});
    logi("{} OnRtnTrade {} {} {} {} traded:{}", id, trade->orderRef, trade->symbol, magic_enum::enum_name(trade->offset),
         magic_enum::enum_name(trade->direction), pTrade->Volume);


}


void
CtpTdGateway::OnRspOrderInsert(CThostFtdcInputOrderField *pInputOrder, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                               bool bIsLast) {
    //CTP检测失败时触发
    int orderRef=atoi(pInputOrder->OrderRef);
    loge("OnRspOrderInsert Error! orderRef:{} {}", pInputOrder->OrderRef,pRspInfo->ErrorMsg);
    if (account->orderMap.contains(orderRef)) {
        Order *order = account->orderMap[orderRef];
        order->status = ORDER_STATUS::ERROR;
        order->statusMsg = pRspInfo->ErrorMsg;
        Event event{EvType::ORDER,0, order};
        this->queue->push(event);
    }
}

void CtpTdGateway::OnErrRtnOrderInsert(CThostFtdcInputOrderField *pInputOrder, CThostFtdcRspInfoField *pRspInfo) {
    int orderRef=atoi(pInputOrder->OrderRef);
    loge("OnErrRtnOrderInsert Error! orderRef:{} {}", pInputOrder->OrderRef,pRspInfo->ErrorMsg);
    if (account->orderMap.contains(orderRef)) {
        Order *order = account->orderMap[orderRef];
        order->status = ORDER_STATUS::ERROR;
        order->statusMsg = pRspInfo->ErrorMsg;
        Event event{EvType::ORDER,0, order};
        this->queue->push(event);
    }
}

void CtpTdGateway::OnErrRtnOrderAction(CThostFtdcOrderActionField *pOrderAction, CThostFtdcRspInfoField *pRspInfo) {
    //CTP检测失败时触发
    loge("OnErrRtnOrderAction");
}

void
CtpTdGateway::OnRspQryTradingAccount(CThostFtdcTradingAccountField *pTradingAccount, CThostFtdcRspInfoField *pRspInfo,
                                     int nRequestID, bool bIsLast) {
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
        account->balance = pTradingAccount->Balance;
        account->closeProfit = pTradingAccount->CloseProfit;
        account->positionProfit = pTradingAccount->PositionProfit;
        account->commission = pTradingAccount->Commission;
        account->deposit = pTradingAccount->Deposit;
        account->available = pTradingAccount->Available;
        account->margin = pTradingAccount->CurrMargin;

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

void CtpTdGateway::OnRspQryInvestorPosition(CThostFtdcInvestorPositionField *pInvestorPosition,
                                            CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {

    if(pInvestorPosition!=NULL){
        //持仓更新
        string symbol = pInvestorPosition->InstrumentID;
        POS_DIRECTION direction =
                pInvestorPosition->PosiDirection == THOST_FTDC_PD_Long ? POS_DIRECTION::LONG : POS_DIRECTION::SHORT;
        logi("{} OnRspQryInvestorPosition {} {} pos:{}", id, symbol, magic_enum::enum_name(direction),
             pInvestorPosition->Position);
        string key = symbol + "-" + to_string(direction);
        if (!account->accoPositionMap.contains(key)) {
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

void CtpTdGateway::OnRspQryOrder(CThostFtdcOrderField *pOrder, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                                 bool bIsLast) {
    //todo 可用来查询挂单
}

void CtpTdGateway::OnRspQryInstrumentCommissionRate(CThostFtdcInstrumentCommissionRateField *pInstrumentCommissionRate,
                                                    CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    //todo 查询手续费率
}

void
CtpTdGateway::OnRspOrderAction(CThostFtdcInputOrderActionField *pInputOrderAction, CThostFtdcRspInfoField *pRspInfo,
                               int nRequestID, bool bIsLast) {
    loge("OnRspOrderAction {}",pRspInfo->ErrorMsg);
}

void CtpTdGateway::OnRspQryInstrument(CThostFtdcInstrumentField *pInstrument, CThostFtdcRspInfoField *pRspInfo,
                                      int nRequestID, bool bIsLast) {

    if(pInstrument!=NULL){
        if (strlen(pInstrument->InstrumentID) <= 10) {
            //过滤掉组合合约
            Contract *contract;
            if (account->contractMap.contains(pInstrument->InstrumentID))
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
            strcpy(a.BrokerID, loginInfo.brokerId.c_str());
            strcpy(a.InvestorID, loginInfo.userId.c_str());
            int ret = m_pUserApi->ReqQryInvestorPosition(&a, this->nRequestID++);
            logi("{} ReqQryInvestorPosition ret={},{}", id, ret, qryRetMsgMap[ret]);
        });
    }

}

void CtpTdGateway::OnRspSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField *pSettlementInfoConfirm,
                                              CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    logi("{} OnRspSettlementInfoConfirm", id);
    if (bIsLast) {
        timer.delay(500, [this]() {
            //查询资金账户
            CThostFtdcQryTradingAccountField a = {0};
            strcpy(a.BrokerID, loginInfo.brokerId.c_str());
            strcpy(a.InvestorID, loginInfo.userId.c_str());
            //strcpy(a.CurrencyID, "CNY");
            int ret = m_pUserApi->ReqQryTradingAccount(&a, this->nRequestID++);
            logi("{} ReqQryTradingAccount ret={},{}", id, ret, qryRetMsgMap[ret]);
        });
    }
}


