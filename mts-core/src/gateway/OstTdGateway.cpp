//
// Created by 段晴 on 2022/2/15.
//
#include <dlfcn.h>
#include "OstTdGateway.h"
#include "Timer.hpp"
#include "magic_enum.hpp"

int OstTdGateway::nRequestID=0;
map<TUTOrderStatusType, ORDER_STATUS> OstTdGateway::statusMap = {
        {UT_OST_AllTraded,             ORDER_STATUS::ALLTRADED},
        {UT_OST_PartTradedQueueing,    ORDER_STATUS::QUEUEING},
        {UT_OST_PartTradedNotQueueing, ORDER_STATUS::NOTQUEUEING},
        {UT_OST_NoTradeQueueing,       ORDER_STATUS::QUEUEING},
        {UT_OST_NoTradeNotQueueing,    ORDER_STATUS::NOTQUEUEING},
        {UT_OST_Canceled,              ORDER_STATUS::CANCELLED},
        {UT_OST_Unknown,               ORDER_STATUS::ALLTRADED}
};
map<int, string> OstTdGateway::qryRetMsgMap = {
        {0,  "成功"},
        {-1, "网络连接失败"},
        {-2, "未处理请求超过许可数"},
        {-3, "每秒发送请求数超过许可数"}

};


int OstTdGateway::connect() {
//    void *handle = dlopen("../lib/ost/libutapi.so", RTLD_LAZY);
//    if (handle != nullptr) {
//        typedef CUTApi *(*CreateTdApiFunc)(const char *);
//        CreateTdApiFunc pfnCreateFtdcTdApiFunc = (CreateTdApiFunc) dlsym(handle,
//                                                                                 "_ZN6CUTApi9CreateApiEPKci");
//        if (pfnCreateFtdcTdApiFunc == nullptr) {
//            logi("load libutapi.so fail [{}] [{}]", errno, strerror(errno));
//            return -1;
//        }
//        m_pUserApi = pfnCreateFtdcTdApiFunc("./flow");
//        m_pUserApi->RegisterSpi(this);
//        m_pUserApi->SubscribePrivateTopic(UT_TERT_QUICK);
//
//    } else {
//        logi("load thosttraderapi.dll fail [{}] [{}]", errno, strerror(errno));
//        return -1;
//    }

    //创建api;将参数nCPUID设置为需要绑定的CPU,可开启极速模式
    //如果同一进程内创建多个api，参数pszFlowPath必须设置为不同的路径
    m_pUserApi = CUTApi::CreateApi();
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
    return 0;
}

int OstTdGateway::disconnect() {
    logw("{} OstTdGateway discounnect ", id);
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

void OstTdGateway::insertOrder(Order *order) {

    CUTInputOrderField* pInputOrderField;
    memset(pInputOrderField, 0, sizeof(CUTInputOrderField));
    strcpy(pInputOrderField->InvestorID, account->loginInfo.userId.c_str());
    strcpy(pInputOrderField->InstrumentID, order->symbol.c_str());
    //pInputOrderField->ExchangeID = order->exchange; //todo
    //OrderRef必须设置,同一会话内必须递增,可以不连续
    pInputOrderField->OrderRef=atol(order->orderRef.c_str());
    pInputOrderField->OrderPriceType = UT_OPT_LimitPrice;

    //股票,基金，债券买:HedgeFlag = UT_HF_Speculation,Direction = UT_D_Buy,OffsetFlag = UT_OF_Open
    //股票, 基金，债券卖:HedgeFlag = UT_HF_Speculation, Direction = UT_D_Sell, OffsetFlag = UT_OF_Close
    //债券逆回购:HedgeFlag = UT_HF_Speculation, Direction = UT_D_Sell, OffsetFlag = UT_OF_Open
    //ETF申购:HedgeFlag = UT_HF_Redemption,Direction = UT_D_Buy,OffsetFlag = UT_OF_Open
    //ETF赎回:HedgeFlag = UT_HF_Redemption,Direction = UT_D_Sell,OffsetFlag = UT_OF_Close
    pInputOrderField->HedgeFlag = UT_HF_Speculation;
    pInputOrderField->Direction = UT_D_Buy;
    pInputOrderField->OffsetFlag = UT_OF_Open;
    pInputOrderField->LimitPrice = 3.5;
    pInputOrderField->VolumeTotalOriginal = 100;
    pInputOrderField->TimeCondition = UT_TC_GFD;
    pInputOrderField->VolumeCondition = UT_VC_AV;
    pInputOrderField->MinVolume = 0;
    pInputOrderField->ContingentCondition = UT_CC_Immediately;
    pInputOrderField->StopPrice = 0;
    pInputOrderField->IsAutoSuspend = 0;memset(pInputOrderField, 0, sizeof(CUTInputOrderField));
    int ret=m_pUserApi->ReqOrderInsert(pInputOrderField,0);
}

void OstTdGateway::cancelOrder(Order *order) {
    if (!this->isConnected()) {
        loge("{} not connected", this->id);
        return;
    }
    CUTInputOrderActionField req = {0};
    req.ActionFlag = UT_AF_Delete;

    req.FrontID = frontId;
    req.SessionID = sessionId;
    req.OrderRef= atol(order->orderRef.c_str());
    this->orderMap[order->orderRef] = order;
    int ret = m_pUserApi->ReqOrderAction(&req, this->nRequestID++);
    logi("{} ReqOrderAction orderRef:{} ret={}", this->id, order->orderRef, ret);
}


void OstTdGateway::reqQryPosition() {

}

void OstTdGateway::reqUserPasswordUpdate() {

}

void OstTdGateway::Run() {
    const char *address = this->loginInfo.tdAddress.c_str();
    m_pUserApi->RegisterFront(const_cast<char *>(address));
    m_pUserApi->Init();
    logi("{} ctp connecting...", id);
    m_pUserApi->Join();
}

void OstTdGateway::OnFrontConnected() {
    fmtlog::setThreadName("TdGateway");
    logi("OnFrontConnected!");

    //登录请求
    CUTReqLoginField reqLoginField;
    memset(&reqLoginField, 0, sizeof(reqLoginField));
    strcpy(reqLoginField.UserID, account->loginInfo.userId.c_str());
    strcpy(reqLoginField.Password, account->loginInfo.password.c_str());
    //strcpy(reqLoginField.UserProductInfo, "xxx");
    int ret = m_pUserApi->ReqLogin(&reqLoginField, this->nRequestID++);
    logi("ReqLogin! ret={}", ret);

}

void OstTdGateway::OnFrontDisconnected(int nReason) {
    loge("OnFrontDisconnected:{}", nReason);
}

void OstTdGateway::OnRspError(CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    loge("OnRspError: Error! [{}] [{}]", pRspInfo->ErrorID, pRspInfo->ErrorMsg);

}

///登录请求响应
void
OstTdGateway::OnRspLogin(CUTRspLoginField *pRspUserLogin, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {

    logi("{} OnRspLogin", id);
    if (pRspInfo->ErrorID == 0) {
        this->sessionId = pRspUserLogin->SessionID;
        this->frontId = pRspUserLogin->FrontID;
        this->connected = true;
        this->tradingDay = pRspUserLogin->TradingDay;
        logi("{} 交易接口登录成功,交易日={}", id, tradingDay);

        timer.delay(500, [this]() {
            //查询账户信息
            CUTQryTradingAccountField QryTradingAccount={0};
            int ret = m_pUserApi->ReqQryTradingAccount(&QryTradingAccount, this->nRequestID++);
            logi("ReqQryTradingAccount  ret={}", ret);
        });


    } else {
        loge("{} 交易接口登录成功失败! ErrorID:{},ErrorMsg:{}", id, pRspInfo->ErrorID, pRspInfo->ErrorMsg);
        this->connected = false;
    }
}

///请求查询合约响应
void OstTdGateway::OnRspQryInstrument(CUTInstrumentField *pInstrument, CUTRspInfoField *pRspInfo, int nRequestID,
                                      bool bIsLast) {

    if (pInstrument != NULL) {
        //ProductID:股票--ASTOCK,基金--ETF,债券--BOND,指数--INDEX
        //printf("Instrument:[%s] [%d] [%s]\n", pInstrument->InstrumentID, pInstrument->ExchangeID, pInstrument->InstrumentName);
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
            contract->posDateType = pInstrument->PositionDateType;
            contract->underlyingSymbol = pInstrument->UnderlyingInstrID;//针对商品期权
        }
    }

    if (bIsLast) {
        logi("{} OnRspQryInstrument Finish,cont:{}", id, account->contractMap.size());

        timer.delay(1000, [this]() {
            //查询持仓
            CUTQryInvestorPositionField a={0};
            int ret = m_pUserApi->ReqQryInvestorPosition(&a, this->nRequestID++);
            logi("{} ReqQryInvestorPosition ret={},{}", id, ret, qryRetMsgMap[ret]);
        });
    }
}

///请求查询资金响应
void
OstTdGateway::OnRspQryTradingAccount(CUTTradingAccountField *pTradingAccount, CUTRspInfoField *pRspInfo, int nRequestID,
                                     bool bIsLast) {
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
        account->balance = pTradingAccount->Balance;
        account->closeProfit = pTradingAccount->CloseProfit;
        account->positionProfit = pTradingAccount->PositionProfit;
        account->commission = pTradingAccount->Commission;
        account->deposit = pTradingAccount->Deposit;
        account->available = pTradingAccount->Available;
        account->margin = pTradingAccount->CurrMargin;


        timer.delay(1000, [this]() {
            //查询合约
            CUTQryInstrumentField QryInstrument ={0};
            int ret = m_pUserApi->ReqQryInstrument(&QryInstrument, this->nRequestID++);
            logi("{} ReqQryInstrument ret={},{}", id, ret, qryRetMsgMap[ret]);
        });
    }

}

///请求查询持仓响应
void OstTdGateway::OnRspQryInvestorPosition(CUTInvestorPositionField *pInvestorPosition, CUTRspInfoField *pRspInfo,
                                            int nRequestID, bool bIsLast) {

    if (pInvestorPosition != NULL){
        //todo 补充持仓更新
        string symbol = pInvestorPosition->InstrumentID;
        POS_DIRECTION direction =
                pInvestorPosition->PosiDirection == UT_PD_Long ? POS_DIRECTION::LONG : POS_DIRECTION::SHORT;
        logi("{} OnRspQryInvestorPosition {} {} pos:{}", id, symbol, magic_enum::enum_name(direction),
             pInvestorPosition->Position);
        string key = symbol + "-" + to_string(direction);
        if (!account->accoPositionMap.contains(key)) {
            AcctPosition *position = new AcctPosition(pInvestorPosition->InstrumentID, direction);
            account->accoPositionMap[key] = position;
        }
        AcctPosition *position = account->accoPositionMap[key];
        position->pos += pInvestorPosition->Position;
        position->tdPos += pInvestorPosition->TodayPosition;
        position->ydPos = position->pos - position->tdPos;


        if (0 == strcmp(pInvestorPosition->InstrumentID, "SHRQ88") ||
            0 == strcmp(pInvestorPosition->InstrumentID, "SZRQ88")) {
            printf("InvestorPosition（历史逆回购标准券,不可平):[%s] [%s] [%d]\n", pInvestorPosition->InvestorID,
                   pInvestorPosition->InstrumentID, pInvestorPosition->Position);
        } else if (pInvestorPosition->PosiDirection == UT_PD_Short) {
            printf("InvestorPosition（今日逆回购持仓,不可平):[%s] [%s] [%d]\n", pInvestorPosition->InvestorID,
                   pInvestorPosition->InstrumentID, pInvestorPosition->Position);
        } else if (pInvestorPosition->PosiDirection == UT_PD_Long) {
            printf("InvestorPosition（非逆回购持仓）:[%s] [%s] [%d]\n", pInvestorPosition->InvestorID,
                   pInvestorPosition->InstrumentID, pInvestorPosition->Position);
        }
    }


    if (bIsLast) {
        logi("{} OnRspQryInvestorPosition Finish!", id);

        //查询持仓结束，查询报单
//        CUTQryOrderField QryOrder;
//        memset(&QryOrder, 0, sizeof(CUTQryOrderField));
//        if (0 != m_pUserApi->ReqQryOrder(&QryOrder, this->nRequestID++)) {
//            printf("ReqQryOrder: Error!\n");
//        }
    }


}

///请求查询报单响应
void OstTdGateway::OnRspQryOrder(CUTOrderField *pOrder, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    if (pOrder != NULL) {
        printf("Order:[%s] [%s] [%d]\n", pOrder->InvestorID, pOrder->InstrumentID, pOrder->VolumeTotalOriginal);
    }

    if (bIsLast) {
        if (pRspInfo && pRspInfo->ErrorID) {
            printf("OnRspQryOrder: Error! [%d] [%s]\n", pRspInfo->ErrorID, pRspInfo->ErrorMsg);
        } else {
            printf("OnRspQryOrder: OK\n");
        }
        //查询报单结束，查询成交
        CUTQryTradeField QryTrade;
        memset(&QryTrade, 0, sizeof(CUTQryTradeField));
        if (0 != m_pUserApi->ReqQryTrade(&QryTrade, +this->nRequestID++)) {
            printf("ReqQryTrade: Error!\n");
        }
    }
}

///请求查询成交响应
void OstTdGateway::OnRspQryTrade(CUTTradeField *pTrade, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    if (pTrade != NULL) {
        printf("Trade:[%s] [%s] [%d]\n", pTrade->InvestorID, pTrade->InstrumentID, pTrade->Volume);
    }

    if (bIsLast) {
        if (pRspInfo && pRspInfo->ErrorID) {
            printf("OnRspQryTrade: Error! [%d] [%s]\n", pRspInfo->ErrorID, pRspInfo->ErrorMsg);
        } else {
            printf("OnRspQryTrade: OK\n");
        }

        //查询可申购ETF信息(每手数量,现金替代比例等)
        CUTQryETFInfoField QryETFInfo;
        memset(&QryETFInfo, 0, sizeof(CUTQryETFInfoField));
        if (0 != m_pUserApi->ReqQryETFInfo(&QryETFInfo, this->nRequestID++)) {
            printf("ReqQryETFInfo: Error!\n");
        }
        //查询可申购ETF对应的成分股
        CUTQryETFComponentField QryETFComponent;
        memset(&QryETFComponent, 0, sizeof(CUTQryETFComponentField));
        if (0 != m_pUserApi->ReqQryETFComponent(&QryETFComponent, this->nRequestID++)) {
            printf("ReqQryETFComponent: Error!\n");
        }
    }
}


///报单录入请求错误时的响应;正确时不会产生该响应,而是回调OnRtnOrder
void OstTdGateway::OnRspOrderInsert(CUTInputOrderField *pInputOrder, CUTRspInfoField *pRspInfo, int nRequestID,
                                    bool bIsLast) {
    printf("OnRspOrderInsert: Error! [%d] [%d] [%s] [%s] [%s] [%d]\n", nRequestID, pRspInfo->ErrorID,
           pRspInfo->ErrorMsg, pInputOrder->InvestorID, pInputOrder->InstrumentID, pInputOrder->OrderRef);
}

///报单通知
void OstTdGateway::OnRtnOrder(CUTOrderField *pOrder) {
    printf("OnRtnOrder:[%s] [%s] [%d]\n", pOrder->InvestorID, pOrder->InstrumentID, pOrder->OrderRef);
    //发一笔撤单;ETF申赎不可撤单
    /*if (pOrder->OrderStatus == UT_OST_Unknown)
    {
        CUTInputOrderActionField inputOrderActionField;
        gen_order_action(&inputOrderActionField, pOrder);
        m_api->ReqOrderAction(&inputOrderActionField, g_reqid);
    }*/
}

///报单操作错误，被UT打回时的响应;正确时不会产生该响应,而是回调OnRtnOrder
void
OstTdGateway::OnRspOrderAction(CUTInputOrderActionField *pInputOrderAction, CUTRspInfoField *pRspInfo, int nRequestID,
                               bool bIsLast) {
    printf("OnRspOrderAction: Error! [%d] [%d] [%s] [%d]\n", nRequestID, pRspInfo->ErrorID, pRspInfo->ErrorMsg,
           pInputOrderAction->OrderRef);
}

///报单操作错误，被交易所打回时的回报
void OstTdGateway::OnErrRtnOrderAction(CUTOrderActionField *pOrderAction) {
    printf("OnErrRtnOrderAction: [%s] [%d]\n", pOrderAction->OrderRef, pOrderAction->ExchangeErrorID);
}

///成交通知
void OstTdGateway::OnRtnTrade(CUTTradeField *pTrade) {
    if (pTrade->HedgeFlag != UT_HF_Redemption) {
        //非ETF成交
        printf("OnRtnTrade:%s,%s,%s\n", pTrade->InvestorID, pTrade->InstrumentID, "非ETF申赎成交");
    } else {
        //ETF成交
        printf("OnRtnTrade:%s,%s,%.2f,%s\n", pTrade->InvestorID, pTrade->InstrumentID, pTrade->Price,
               (pTrade->TradeType == UT_TRDT_ETFComponent ? "ETF申赎成分股成交" : pTrade->TradeType == UT_TRDT_ETFMoney
                                                                           ? "ETF申赎资金成交" : "ETF申赎成交"));
    }
}



