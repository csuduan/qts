//
// Created by 段晴 on 2022/2/15.
//

#ifndef MTS_CORE_OSTTDGATEWAY_H
#define MTS_CORE_OSTTDGATEWAY_H


#include "ost/UTApi.h"
#include "Gateway.h"
#include "LockFreeQueue.hpp"
#include "Timer.hpp"
#include "define.h"

class OstTdGateway : public CUTSpi, public TdGateway {

private:
    static int nRequestID;
    static map<TUTOrderStatusType, ORDER_STATUS> statusMap;
    static map<TUTExchangeIDType,string> exgMap;
    static map<string,TUTExchangeIDType> reExgMap;

    static map<int, string> qryRetMsgMap;
    //map<string,TUTExchangeIDType> exgReverseMap;

    string id;
    CUTApi *m_pUserApi;
    Account *account;
    LoginInfo loginInfo;
    LockFreeQueue<Event> *queue;
    int frontId = 0;// 前置机编号
    long long sessionId = 0;// 会话编号
    vector<Position> tmpPositons;
    vector<Trade> tmpTrades;
    //map<int, Order *> orderMap;
    Timer timer;

    void Run();

public:
    OstTdGateway(Account *account) : account(account) {
        this->loginInfo = account->loginInfo;
        this->id = account->id;
        this->queue = account->queue;

//        for(auto &[key,value]:exgMap){
//            exgReverseMap[value]=key;
//        }
    }

    ~OstTdGateway() {}

    int connect() override;

    int disconnect() override;

    bool insertOrder(Order *order) override;

    void cancelOrder(Action *order) override;

    void reqQryPosition();

    ///用户口令更新请求
    void reqUserPasswordUpdate();

    ///当客户端与交易后台建立起通信连接时（还未登录前），该方法被调用。
    void OnFrontConnected() override;

    ///当客户端与交易后台通信连接断开时，该方法被调用。当发生这个情况后，API会自动重新连接，客户端可不做处理。
    void OnFrontDisconnected(int nReason) override;

    ///错误应答
    void OnRspError(CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override;

    ///登录请求响应
    void OnRspLogin(CUTRspLoginField *pRspUserLogin, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override;

    ///请求查询合约响应
    void OnRspQryInstrument(CUTInstrumentField *pInstrument, CUTRspInfoField *pRspInfo, int nRequestID,
                            bool bIsLast) override;

    ///请求查询资金响应
    void OnRspQryTradingAccount(CUTTradingAccountField *pTradingAccount, CUTRspInfoField *pRspInfo, int nRequestID,
                                bool bIsLast) override;

    ///请求查询持仓响应
    void
    OnRspQryInvestorPosition(CUTInvestorPositionField *pInvestorPosition, CUTRspInfoField *pRspInfo, int nRequestID,
                             bool bIsLast) override;

    ///请求查询报单响应
    void OnRspQryOrder(CUTOrderField *pOrder, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override;

    ///请求查询成交响应
    void OnRspQryTrade(CUTTradeField *pTrade, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override;

    ///报单录入请求错误时的响应;正确时不会产生该响应,而是回调OnRtnOrder
    void
    OnRspOrderInsert(CUTInputOrderField *pInputOrder, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override;

    ///报单通知
    void OnRtnOrder(CUTOrderField *pOrder) override;

    ///报单操作错误，被UT打回时的响应;正确时不会产生该响应,而是回调OnRtnOrder
    void OnRspOrderAction(CUTInputOrderActionField *pInputOrderAction, CUTRspInfoField *pRspInfo, int nRequestID,
                          bool bIsLast) override;

    ///报单操作错误，被交易所打回时的回报
    void OnErrRtnOrderAction(CUTOrderActionField *pOrderAction) override;

    ///成交通知
    void OnRtnTrade(CUTTradeField *pTrade) override;

};


#endif //MTS_CORE_OSTTDGATEWAY_H
