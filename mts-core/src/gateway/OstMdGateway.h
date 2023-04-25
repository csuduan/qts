//
// Created by Administrator on 2020/7/2.
//
#include <functional>
#include <dlfcn.h>
#include "Delegate.h"
#include "define.h"

#ifndef TRADECORE_OSTMDAPI_H
#define TRADECORE_OSTMDAPI_H

#include "Data.h"
#include "Gateway.h"
#include "LockFreeQueue.hpp"
#include "ost/UTMdApi.h"
#include <thread>
#include <netdb.h>
#include "Timer.hpp"
#include "Shm.hpp"


class OstMdGateway: public CUTMDSpi, public MdGateway
{
public:
    OstMdGateway(Quote* quote): MdGateway(quote){
    }
    ~OstMdGateway() {}
    void OnFrontConnected() override;
    void OnFrontDisconnected(int nReason) override;
    void OnRspLogin(CUTRspLoginField *pRspLogin, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override;
    void OnRspError(CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override;
    void OnRspSubDepthMarketData(CUTSubInstrumentField *pSubInstrument, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override;
    void OnRtnDepthMarketData(CUTDepthMarketDataField *pDepthMarketData) override;


    void subscribe(set<string> &contracts);
    void reSubscribe();
    int  connect();
    //void init();
    void disconnect();
    CMultiDelegate<void, Tick*> OnTickData;

private:
    static int nRequestID;
    static map<TUTExchangeIDType,string> exgMap;
    static map<string,TUTExchangeIDType> reExgMap;

    CUTMDApi* m_pUserApi;
    bool  isConnected;
    string tradingDay;
    Timer timer;
    void Run();
};
#endif //TRADECORE_OSTMDAPI_H
