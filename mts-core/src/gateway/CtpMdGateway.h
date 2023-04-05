//
// Created by Administrator on 2020/7/2.
//
#include "ctp/ThostFtdcMdApi.h"
#include "ctp/ThostFtdcUserApiStruct.h"
#include <functional>
#include <dlfcn.h>
#include "Delegate.h"
#include "define.h"

#ifndef TRADECORE_CTPMDAPI_H
#define TRADECORE_CTPMDAPI_H

#include "Data.h"
#include "Gateway.h"
#include "LockFreeQueue.hpp"

class CtpMdGateway: public CThostFtdcMdSpi, public MdGateway
{
public:
    //std::function<void(TickData *)> tickDataCallBack = nullptr;
    //std::list<std::function<void(TickData)>> tickDataCallBack;
    CtpMdGateway(Account* account): account(account){
        this->queue=&account->eventQueue;
    }
    ~CtpMdGateway() {}
    void ReqUserLogin();
    void OnFrontConnected() override;
    void OnFrontDisconnected(int nReason) override;
    void OnRspUserLogin(CThostFtdcRspUserLoginField* pRspUserLogin,CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast) override;
    void OnRspSubMarketData(CThostFtdcSpecificInstrumentField* pSpecificInstrument, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast) override;
    void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField* pDepthMarketData) override;
    ///错误应答
    void OnRspError(CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override;



    void subscribe(set<string> &contracts);
    void unSubscribe(string contract);
    void reSubscribe();
    int  connect();
    void disconnect();
    CMultiDelegate<void, Tick*> OnTickData;



private:
    static int nRequestID;
    // 指向CThostFtdcMduserApi实例的指针
    CThostFtdcMdApi* m_pUserApi;
    bool  isConnected;
    string tradingDay;
    std::set<string> contracts;
    LockFreeQueue<Event>* queue;
    Account * account;
    void Run();
};


#endif //TRADECORE_CTPMDAPI_H
