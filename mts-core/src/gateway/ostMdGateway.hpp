//
// Created by Administrator on 2020/7/2.
//
#include <functional>
#include <dlfcn.h>
#include "delegate.h"
#include "define.h"

#ifndef TRADECORE_OSTMDAPI_HPP
#define TRADECORE_OSTMDAPI_HPP

#include "data.h"
#include "gateway.h"
#include "lockFreeQueue.hpp"
#include "ost/UTMdApi.h"
#include <thread>
#include <netdb.h>
#include "common/timer.hpp"
#include "shm.hpp"
#include "common/util.hpp"
#include "semaphore.h"
#include "trade/acct.h"


class OstMdGateway: public CUTMDSpi, public MdGateway
{
public:
    OstMdGateway(QuoteInfo* quotaInfo): MdGateway(quotaInfo){
        this->name=quotaInfo->id;
    }
    ~OstMdGateway() {}
    void OnFrontConnected() override{
        fmtlog::setThreadName("MdGateway");
        logi("Md[{}] OnFrontConnected",this->id);
        CUTReqLoginField reqLoginField = {0};
        strcpy(reqLoginField.UserID, quotaInfo->user.c_str());
        strcpy(reqLoginField.Password, quotaInfo->pwd.c_str());
        int ret = m_pUserApi->ReqLogin(&reqLoginField, this->nRequestID++);
        logi("Md[{}] ReqLogin ret:{}", this->id,ret);

    }
    void OnFrontDisconnected(int nReason) override{
        logi("Md[{}] OnFrontDisconnected n=",this->id,nReason);
        this->setStatus(false);
    }
    void OnRspLogin(CUTRspLoginField *pRspLogin, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override{
        if (bIsLast && pRspInfo->ErrorID == 0) {
            this->tradingDay = pRspLogin->TradingDay;
            logi("{}  行情接口连接成功,交易日 = {}",name, this->tradingDay);
            this->setStatus(true);
            std::this_thread::sleep_for(std::chrono::milliseconds(1000));
            //重新订阅
            timer.delay(1000, [this]() {
                this->reSubscribe();
            });

        } else {
            loge("{} 行情接口连接失败, ErrorMsg={}",name, pRspInfo->ErrorMsg);
            this->setStatus(false);
        }

    }
    void OnRspError(CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override{
        loge("{} OnRspError {}", name,pRspInfo->ErrorMsg);
    }

    void OnRspSubDepthMarketData(CUTSubInstrumentField *pSubInstrument, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) override{
        if (pRspInfo->ErrorID == 0) {
            logi("{} OnRspSubMarketData {}", name,pSubInstrument->InstrumentID);
        } else
            loge("{} OnRspSubMarketData fail {} {}", name,pSubInstrument->InstrumentID, Util::g2u(pRspInfo->ErrorMsg));

    }
    void OnRtnDepthMarketData(CUTDepthMarketDataField *pDepthMarketData) override{
        long tsc=Context::get().tn.rdtsc();
        float updateTime=(float)pDepthMarketData->UpdateTime/1000;

        //todo tick从对象池获取
        Tick *tickData = new Tick();
        tickData->tradingDay = to_string(pDepthMarketData->TradingDay);
        tickData->symbol = pDepthMarketData->InstrumentID;
        tickData->exchange = pDepthMarketData->ExchangeID;
        tickData->lastPrice = pDepthMarketData->LastPrice;
        tickData->preSettlePrice = pDepthMarketData->PreSettlementPrice;
        tickData->openPrice = pDepthMarketData->OpenPrice;
        tickData->volume = pDepthMarketData->Volume;
        tickData->updateTime = updateTime;
        tickData->bidPrice1 = pDepthMarketData->BidPrice1;
        tickData->bidPrice2 = pDepthMarketData->BidPrice2;
        tickData->bidPrice3 = pDepthMarketData->BidPrice3;
        tickData->bidPrice4 = pDepthMarketData->BidPrice4;
        tickData->bidPrice5 = pDepthMarketData->BidPrice5;
        tickData->askPrice1 = pDepthMarketData->AskPrice1;
        tickData->askPrice2 = pDepthMarketData->AskPrice2;
        tickData->askPrice3 = pDepthMarketData->AskPrice3;
        tickData->askPrice4 = pDepthMarketData->AskPrice4;
        tickData->askPrice5 = pDepthMarketData->AskPrice5;
        tickData->askVolume1 = pDepthMarketData->AskVolume1;
        tickData->bidVolume1 = pDepthMarketData->BidVolume1;
        tickData->recvTsc=tsc;
        this->msgQueue->push(Event{EvType::TICK, tsc, tickData});
        //logi("MD OnRtnTick instrument=[{}] time=[{}] price=[{}]",
        //     pDepthMarketData->InstrumentID, updateTime, pDepthMarketData->LastPrice);
    }



    int  connect() override {
        void *handle = dlopen("lib/ost/libutmdapi.so", RTLD_LAZY);
        if(handle == nullptr){
            logi("load libutmdapi.so fail  [{}]", errno, dlerror());
            return -1;
        }
        typedef CUTMDApi *(*CreateApiMdFunc)(const char *);
        CreateApiMdFunc pfnCreateFtdcMdApiFunc = (CreateApiMdFunc) dlsym(handle,
                                                                         "_ZN8CUTMDApi11CreateMDApiEPKcib");
        if (pfnCreateFtdcMdApiFunc == nullptr) {
            logi("load libutmdapi.so fail [{}] [{}]", errno, hstrerror(errno));
            return -1;
        }
        m_pUserApi = pfnCreateFtdcMdApiFunc(".");
        //创建api;将参数nCPUID设置为需要绑定的CPU,可开启极速模式
        //如果同一进程内创建多个api，参数pszFlowPath必须设置为不同的路径
        //m_pUserApi = CUTMDApi::CreateMDApi();
        m_pUserApi->RegisterSpi(this);
        thread t([this]() {
            this->run();
        });
        t.detach();

        return 0;
    }
    void disconnect() override{
        if(this->connected== false)
            return;
        this->setStatus(false);
        try {
            if (m_pUserApi != nullptr) {
                m_pUserApi->Release();
                m_pUserApi = nullptr;
            }
        } catch (exception ex) {
            loge("{} discounnect fail ,{}", name, ex.what());
        }
    }

    void subscribe(set<string> &subContracts) override{
        int i = 0;
        for (auto &item: subContracts) {
            if (quotaInfo->subSet.count(item)>0)
                continue;
            quotaInfo->subSet.insert(item);

            CUTSubInstrumentField req = {0};
            if(Util::starts_with(item,"6"))
                req.ExchangeID= UT_EXG_SSE;
            else
                req.ExchangeID = UT_EXG_SZSE;
            strcpy(req.InstrumentID, item.c_str());

            if(connected){
                int ret = this->m_pUserApi->SubscribeDepthMarketData(&req, 1);
                logi("subscribeContract {} ret={}",item, ret);
            }
        }
    }

    void reSubscribe(){
        for(auto & item :quotaInfo->subSet){
            CUTSubInstrumentField req = {0};
            if(Util::starts_with(item,"6"))
                req.ExchangeID= UT_EXG_SSE;
            else
                req.ExchangeID = UT_EXG_SZSE;
            strcpy(req.InstrumentID, item.c_str());
            int ret = this->m_pUserApi->SubscribeDepthMarketData(&req, 1);
        }
        logi("resubscribeContract count={}",quotaInfo->subSet.size());


    }

    CMultiDelegate<void, Tick*> OnTickData;

private:
    static inline int nRequestID =0 ;
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

    Semaphore  semaphore={0};

    string name;
    CUTMDApi* m_pUserApi;
    string tradingDay;
    Timer timer;
    void run(){
        const char *address = quotaInfo->address.c_str();
        m_pUserApi->RegisterFront(const_cast<char *>(address));
        m_pUserApi->Init();
        m_pUserApi->Join();
    }
};
#endif //TRADECORE_OSTMDAPI_HPP
