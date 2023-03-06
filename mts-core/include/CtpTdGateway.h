//
// Created by Administrator on 2020/7/2.
//
#include "ctp/ThostFtdcTraderApi.h"
#include "ctp/ThostFtdcUserApiStruct.h"
#include "Logger.h"
#include <dlfcn.h>

#ifndef TRADECORE_CTPTDAPI_H
#define TRADECORE_CTPTDAPI_H

#include "Data.h"
#include "Gateway.h"
#include "LockFreeQueue.h"

class CtpTdGateway : public CThostFtdcTraderSpi, public TdGateway {
private:
    static int nRequestID;
    CThostFtdcTraderApi* m_pUserApi;
    LoginInfo loginInfo;
    LockFreeQueue<Event>* queue;
    void Run();
public:
    CtpTdGateway(LoginInfo loginInfo,LockFreeQueue<Event>*):loginInfo(loginInfo),queue(queue) {}
    ~CtpTdGateway() {}
    int connect();
    //客户端认证
    void ReqAuthenticate();
    void ReqUserLogin();
    void ReqUserLogout();
    ///请求确认结算单
    void ReqSettlementInfoConfirm();
    ///用户口令更新请求
    void ReqUserPasswordUpdate();

    void OnFrontConnected() override;
    void OnFrontDisconnected(int nReason) override;
    ///客户端认证响应
    void OnRspAuthenticate(CThostFtdcRspAuthenticateField* pRspAuthenticateField, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast) override;
    ///登录请求响应
    void OnRspUserLogin(CThostFtdcRspUserLoginField* pRspUserLogin, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast) override;
    ///登出请求响应
    void OnRspUserLogout(CThostFtdcUserLogoutField* pUserLogout, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast) override;
    /*
     ///用户口令更新请求响应
     virtual void OnRspUserPasswordUpdate(CThostFtdcUserPasswordUpdateField* pUserPasswordUpdate, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast);
     ///报单录入请求响应
     virtual void OnRspOrderInsert(CThostFtdcInputOrderField* pInputOrder, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast);
     ///报单操作请求响应
     virtual void OnRspOrderAction(CThostFtdcInputOrderActionField* pInputOrderAction, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast);
     ///请求查询产品响应
     virtual void OnRspQryProduct(CThostFtdcProductField* pProduct, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast);
     ///请求查询合约手续费率响应
     virtual void OnRspQryInstrumentCommissionRate(CThostFtdcInstrumentCommissionRateField* pInstrumentCommissionRate, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast);
     ///请求查询交易所响应
     virtual void OnRspQryExchange(CThostFtdcExchangeField* pExchange, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast);
     ///请求查询合约响应
     virtual void OnRspQryInstrument(CThostFtdcInstrumentField* pInstrument, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast);
     ///请求查询行情响应
     virtual void OnRspQryDepthMarketData(CThostFtdcDepthMarketDataField* pDepthMarketData, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast);
     ///请求查询投资者结算结果响应
     virtual void OnRspQrySettlementInfo(CThostFtdcSettlementInfoField* pSettlementInfo, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast);
     ///请求查询投资者持仓明细响应
     virtual void OnRspQryInvestorPositionDetail(CThostFtdcInvestorPositionDetailField* pInvestorPositionDetail, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast);
     ///请求查询报单手续费响应
     virtual void OnRspQryInstrumentOrderCommRate(CThostFtdcInstrumentOrderCommRateField* pInstrumentOrderCommRate, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast);
     ///错误应答
     virtual void OnRspError(CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast);
     ///报单通知
     virtual void OnRtnOrder(CThostFtdcOrderField* pOrder);
     ///成交通知
     virtual void OnRtnTrade(CThostFtdcTradeField* pTrade);
     ///报单录入错误回报
     virtual void OnErrRtnOrderInsert(CThostFtdcInputOrderField* pInputOrder, CThostFtdcRspInfoField* pRspInfo);
     ///报单操作错误回报
     virtual void OnErrRtnOrderAction(CThostFtdcOrderActionField* pOrderAction, CThostFtdcRspInfoField* pRspInfo);
     ///合约交易状态通知
     virtual void OnRtnInstrumentStatus(CThostFtdcInstrumentStatusField* pInstrumentStatus);*/
};


#endif //TRADECORE_CTPTDAPI_H
