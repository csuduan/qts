//
// Created by Administrator on 2020/7/2.
//
#include "ctp/ThostFtdcTraderApi.h"
#include "ctp/ThostFtdcUserApiStruct.h"
#include "define.h"
#include <dlfcn.h>

#ifndef TRADECORE_CTPTDAPI_H
#define TRADECORE_CTPTDAPI_H

#include "Data.h"
#include "Gateway.h"
#include "LockFreeQueue.hpp"
#include "Timer.hpp"

class CtpTdGateway : public CThostFtdcTraderSpi, public TdGateway {
private:
    static int nRequestID;
    static map<TThostFtdcOrderStatusType, ORDER_STATUS> statusMap;
    static map<int, string> qryRetMsgMap;
    string id;
    CThostFtdcTraderApi *m_pUserApi;
    Acct *account;
    LoginInfo loginInfo;
    LockFreeQueue<Event> *queue;
    int frontId = 0;// 前置机编号
    int sessionId = 0;// 会话编号
    vector<Position> tmpPositons;
    vector<Trade> tmpTrades;
    Timer timer;

    void Run();

public:
    CtpTdGateway(Acct *account) : account(account) {
        this->loginInfo = account->loginInfo;
        this->id = account->id;
        this->queue = account->queue;
    }

    ~CtpTdGateway() {}

    ///连接
    int connect() override;

    ///断开
    int disconnect() override;

    ///插入报单
    bool insertOrder(Order *order) override;

    ///撤销报单
    void cancelOrder(Action *order) override;

    ///查询持仓
    void reqQryPosition();

    ///用户口令更新请求
    void reqUserPasswordUpdate();

    ///前置连接响应
    void OnFrontConnected() override;

    ///前置断开响应
    void OnFrontDisconnected(int nReason) override;

    ///客户端认证响应
    void OnRspAuthenticate(CThostFtdcRspAuthenticateField *pRspAuthenticateField, CThostFtdcRspInfoField *pRspInfo,
                           int nRequestID, bool bIsLast) override;

    ///登录请求响应
    void OnRspUserLogin(CThostFtdcRspUserLoginField *pRspUserLogin, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                        bool bIsLast) override;

    ///登出请求响应
    void OnRspUserLogout(CThostFtdcUserLogoutField *pUserLogout, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                         bool bIsLast) override;

    ///错误应答
    void OnRspError(CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override;

    ///报单通知
    void OnRtnOrder(CThostFtdcOrderField *pOrder) override;

    ///成交通知
    void OnRtnTrade(CThostFtdcTradeField *pTrade) override;

    ///报单录入错误回报
    void OnErrRtnOrderInsert(CThostFtdcInputOrderField *pInputOrder, CThostFtdcRspInfoField *pRspInfo) override;

    ///报单操作错误回报
    void OnErrRtnOrderAction(CThostFtdcOrderActionField *pOrderAction, CThostFtdcRspInfoField *pRspInfo) override;

    ///报单录入请求响应
    void OnRspOrderInsert(CThostFtdcInputOrderField *pInputOrder, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                          bool bIsLast) override;

    ///报单操作请求响应
    void OnRspOrderAction(CThostFtdcInputOrderActionField *pInputOrderAction, CThostFtdcRspInfoField *pRspInfo,
                          int nRequestID, bool bIsLast) override;

    ///请求查询报单响应
    void OnRspQryOrder(CThostFtdcOrderField *pOrder, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                       bool bIsLast) override;

    ///请求查询成交响应
    void OnRspQryTrade(CThostFtdcTradeField *pTrade, CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {};

    ///请求查询投资者持仓响应
    void OnRspQryInvestorPosition(CThostFtdcInvestorPositionField *pInvestorPosition, CThostFtdcRspInfoField *pRspInfo,
                                  int nRequestID, bool bIsLast) override;

    ///请求查询资金账户响应
    void OnRspQryTradingAccount(CThostFtdcTradingAccountField *pTradingAccount, CThostFtdcRspInfoField *pRspInfo,
                                int nRequestID, bool bIsLast) override;

    ///投资者结算结果确认响应
    void OnRspSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField *pSettlementInfoConfirm,
                                    CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override;

    ///用户口令更新请求响应
    void
    OnRspUserPasswordUpdate(CThostFtdcUserPasswordUpdateField *pUserPasswordUpdate, CThostFtdcRspInfoField *pRspInfo,
                            int nRequestID, bool bIsLast) {};

    ///请求查询合约手续费率响应
    void OnRspQryInstrumentCommissionRate(CThostFtdcInstrumentCommissionRateField *pInstrumentCommissionRate,
                                          CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override;

    ///请求查询合约响应
    void OnRspQryInstrument(CThostFtdcInstrumentField *pInstrument, CThostFtdcRspInfoField *pRspInfo, int nRequestID,
                            bool bIsLast) override;

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
