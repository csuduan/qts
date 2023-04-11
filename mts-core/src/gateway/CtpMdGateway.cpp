//
// Created by Administrator on 2020/7/2.
//

#include <thread>
#include <netdb.h>
#include "CtpMdGateway.h"
#include "Data.h"
#include "util/Util.h"

int CtpMdGateway::nRequestID;

int CtpMdGateway::connect() {
    void *handle = dlopen("../lib/ctp/thostmduserapi_se.so", RTLD_LAZY);
    if (handle != nullptr) {
        //查看函数符号  objdump -tT ctp/thostmduserapi_se.so  |grep CreateFtdcMdApi
        //或者nm -D   （-T 的行表示导出函数）
        typedef CThostFtdcMdApi *(*CreateApiMdFunc)(const char *);
        CreateApiMdFunc pfnCreateFtdcMdApiFunc = (CreateApiMdFunc) dlsym(handle,
                                                                         "_ZN15CThostFtdcMdApi15CreateFtdcMdApiEPKcbb");
        if (pfnCreateFtdcMdApiFunc == nullptr) {
            logi("load thosttraderapi.so fail [{}] [{}]", errno, hstrerror(errno));
            return -1;
        }
        m_pUserApi = pfnCreateFtdcMdApiFunc(".");
        m_pUserApi->RegisterSpi(this);
    } else {
        logi("load thosttraderapi.dll fail  [{}]", errno, dlerror());
        return -1;
    }

    std::thread t(std::bind(&CtpMdGateway::Run, this));
    t.detach();
    return 0;
}


void CtpMdGateway::unSubscribe(string contract) {

}


void CtpMdGateway::disconnect() {

}

void CtpMdGateway::Run() {
    const char *address = account->loginInfo.mdAddress.c_str();
    m_pUserApi->RegisterFront(const_cast<char *>(address));
    m_pUserApi->Init();
    logi("ctp connecting...{}", address);
    m_pUserApi->Join();
}

void CtpMdGateway::ReqUserLogin() {
    CThostFtdcReqUserLoginField reqUserLogin;
    int ret = m_pUserApi->ReqUserLogin(&reqUserLogin, nRequestID++);
    logi("\tlogin ret = {}\n", ret);
}


void CtpMdGateway::OnFrontConnected() {
    fmtlog::setThreadName("MdGateway");
    logi("MD OnFrontConnected");
    this->ReqUserLogin();
}

void CtpMdGateway::OnFrontDisconnected(int nReason) {
    logi("MD OnFrontDisconnected reson=[{}]", nReason);
    this->isConnected = false;
}

void CtpMdGateway::OnRspUserLogin(CThostFtdcRspUserLoginField *pRspUserLogin, CThostFtdcRspInfoField *pRspInfo,
                                  int nRequestID, bool bIsLast) {
    logi("MD OnRspUserLogin");
    if (bIsLast && pRspInfo->ErrorID == 0) {
        this->tradingDay = this->m_pUserApi->GetTradingDay();
        logi("MD  行情接口连接成功,交易日 = {}", this->tradingDay);
        this->isConnected = true;
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
        //重新订阅
        this->reSubscribe();
    } else {
        loge("MD 行情接口连接失败, ErrorMsg={}", pRspInfo->ErrorMsg);
    }
}

void CtpMdGateway::OnRspSubMarketData(CThostFtdcSpecificInstrumentField *pSpecificInstrument,
                                      CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    if (pRspInfo->ErrorID == 0) {
        logi("MD OnRspSubMarketData {}",pSpecificInstrument->InstrumentID);
    }else
        loge("MD OnRspSubMarketData fail {} {}",pSpecificInstrument->InstrumentID,pRspInfo->ErrorMsg);

}

void CtpMdGateway::OnRtnDepthMarketData(CThostFtdcDepthMarketDataField *pDepthMarketData) {
    //logi("MD OnRtnTick instrument=[{}] time=[{}] price=[{}]",
    //                         pDepthMarketData->InstrumentID, pDepthMarketData->UpdateTime,pDepthMarketData->LastPrice);
    Tick *tickData = new Tick();
    tickData->tradingDay = pDepthMarketData->TradingDay;
    tickData->symbol = pDepthMarketData->InstrumentID;
    tickData->exchange = pDepthMarketData->ExchangeID;
    tickData->lastPrice = pDepthMarketData->LastPrice;
    tickData->preSettlePrice = pDepthMarketData->PreSettlementPrice;
    tickData->openPrice = pDepthMarketData->OpenPrice;
    tickData->volume = pDepthMarketData->Volume;
    tickData->actionTime = pDepthMarketData->UpdateTime;
    tickData->bidPrice1=pDepthMarketData->BidPrice1;
    tickData->bidPrice2=pDepthMarketData->BidPrice2;
    tickData->bidPrice3=pDepthMarketData->BidPrice3;
    tickData->bidPrice4=pDepthMarketData->BidPrice4;
    tickData->bidPrice5=pDepthMarketData->BidPrice5;
    tickData->askPrice1=pDepthMarketData->AskPrice1;
    tickData->askPrice2=pDepthMarketData->AskPrice2;
    tickData->askPrice3=pDepthMarketData->AskPrice3;
    tickData->askPrice4=pDepthMarketData->AskPrice4;
    tickData->askPrice5=pDepthMarketData->AskPrice5;

    //todo
    Util::getTime(&tickData->timeStampRecv);
    //fmtlog::TSCNS().rdtsc();
    //this->OnTickData(tickData);
    this->queue->push(Event(EventType::TICK,tickData));
}


void CtpMdGateway::subscribe(set<string> &subContracts) {
    char **str = new char *[subContracts.size() + 1];
    int i = 0;
    for (auto &item: subContracts) {
        if (this->contracts.contains(item))
            continue;
        this->contracts.insert(item);
        str[i++] = const_cast<char *>(item.c_str());
    }
    if (i > 0 && this->isConnected) {
        int ret = this->m_pUserApi->SubscribeMarketData(str, i);
        logi("MD subscribeContract count:{} ret = {}", i, ret);
    }
}

void CtpMdGateway::reSubscribe() {
    if (contracts.size() > 0) {
        char **str = new char *[contracts.size() + 1];
        int i = 0;
        for (auto &item: contracts) {
            str[i++] = const_cast<char *>(item.c_str());
        }
        //char **ppInstrumentID = new char*[2];
        //ppInstrumentID[0]="ni2203";
        int ret = this->m_pUserApi->SubscribeMarketData(str, i);
        logi("{} reSubscribe  count={} ret = {}", account->id, contracts.size(),ret);
    }
}

void CtpMdGateway::OnRspError(CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    loge("MD OnRspError {}",pRspInfo->ErrorMsg);
}