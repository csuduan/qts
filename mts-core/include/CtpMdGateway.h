//
// Created by Administrator on 2020/7/2.
//
#include "ctp/ThostFtdcMdApi.h"
#include "ctp/ThostFtdcUserApiStruct.h"
#include "Logger.h"
#include <functional>
#include "Data.h"
#include <dlfcn.h>
#include "EngineContext.h"
#include "Delegate.h"

#ifndef TRADECORE_CTPMDAPI_H
#define TRADECORE_CTPMDAPI_H

#include "Data.h"
#include "Gateway.h"
#include "LockFreeQueue.h"

class CtpMdGateway: public CThostFtdcMdSpi, public MdGateway
{
public:
    //std::function<void(TickData *)> tickDataCallBack = nullptr;
    //std::list<std::function<void(TickData)>> tickDataCallBack;
    CtpMdGateway(MdInfo mdInfo,LockFreeQueue<Event>* queue): mdInfo(mdInfo),queue(queue) {}
    ~CtpMdGateway() {}
    void ReqUserLogin();
    void OnFrontConnected() override;
    void OnFrontDisconnected(int nReason) override;
    void OnRspUserLogin(CThostFtdcRspUserLoginField* pRspUserLogin,CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast) override;
    void OnRspSubMarketData(CThostFtdcSpecificInstrumentField* pSpecificInstrument, CThostFtdcRspInfoField* pRspInfo, int nRequestID, bool bIsLast) override;
    void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField* pDepthMarketData) override;


    void subscribe(vector<string> &contracts);
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
    MdInfo mdInfo;
    LockFreeQueue<Event>* queue;
    void Run();
};


#endif //TRADECORE_CTPMDAPI_H
