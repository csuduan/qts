//
// Created by Administrator on 2020/7/2.
//

#include <thread>
#include "CtpMdGateway.h"
#include "Data.h"
#include "Util.h"

int CtpMdGateway::nRequestID;

int CtpMdGateway::connect() {
    void *handle = dlopen("../lib/ctp/thostmduserapi_se.so", RTLD_LAZY);
    if (handle != nullptr) {
        typedef CThostFtdcMdApi *(*CreateApiMdFunc)(const char *);
        CreateApiMdFunc pfnCreateFtdcMdApiFunc = (CreateApiMdFunc) dlsym(handle,
                                                                         "_ZN15CThostFtdcMdApi15CreateFtdcMdApiEPKcbb");
        if (pfnCreateFtdcMdApiFunc == nullptr) {
            Logger::getLogger().info("load thosttraderapi.so fail [%d] [%s]", errno, hstrerror(errno));
            return -1;
        }
        m_pUserApi = pfnCreateFtdcMdApiFunc(".");
        m_pUserApi->RegisterSpi(this);
    } else {
        Logger::getLogger().info("load thosttraderapi.dll fail  [%s]", errno, dlerror());
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
    const char *address = this->mdInfo.mdAddress.c_str();
    m_pUserApi->RegisterFront(const_cast<char *>(address));
    m_pUserApi->Init();
    Logger::getLogger().info("ctp connecting...%s", address);
    m_pUserApi->Join();
}

void CtpMdGateway::ReqUserLogin() {
    CThostFtdcReqUserLoginField reqUserLogin;
    int ret = m_pUserApi->ReqUserLogin(&reqUserLogin, nRequestID++);
    Logger::getLogger().info("\tlogin ret = %d\n", ret);
}

void CtpMdGateway::subscribe(vector<string> &subContracts) {
    char **str = new char *[subContracts.size() + 1];
    int i = 0;
    for (string &item: subContracts) {
        if (this->contracts.contains(item))
            continue;
        this->contracts.insert(item);
        str[i++] = const_cast<char *>(item.c_str());
    }
    if (i > 0) {
        int ret = this->m_pUserApi->SubscribeMarketData(str, i);
        Logger::getLogger().info("%s subscribeContract count:%d ret = %d", mdInfo.id.c_str(), i, ret);
    }
}

void CtpMdGateway::OnFrontConnected() {
    Logger::getLogger().info("%s OnFrontConnected", mdInfo.id.c_str());
    this->ReqUserLogin();
}

void CtpMdGateway::OnFrontDisconnected(int nReason) {
    Logger::getLogger().info("%s OnFrontDisconnected reson=[%d]", mdInfo.id.c_str(), nReason);
    this->isConnected = false;
}

void CtpMdGateway::OnRspUserLogin(CThostFtdcRspUserLoginField *pRspUserLogin, CThostFtdcRspInfoField *pRspInfo,
                                  int nRequestID, bool bIsLast) {
    if (bIsLast && pRspInfo->ErrorID == 0) {
        this->tradingDay = this->m_pUserApi->GetTradingDay();
        Logger::getLogger().info("%s OnRspUserLogin 行情接口 连接[成功] 获取当前行情日 = {%s}", mdInfo.id.c_str(),
                                 this->tradingDay.c_str());
        this->isConnected = true;
        //重新订阅
        this->reSubscribe();
    } else {
        Logger::getLogger().error("%s OnRspUserLogin ErrorID={%s}, ErrorMsg={%s}", mdInfo.id.c_str(), pRspInfo->ErrorID,
                                  pRspInfo->ErrorMsg);
    }
}

void CtpMdGateway::OnRspSubMarketData(CThostFtdcSpecificInstrumentField *pSpecificInstrument,
                                      CThostFtdcRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    if (pSpecificInstrument && pRspInfo->ErrorID == 0) {
        Logger::getLogger().info("%s OnRspSubMarketData [%s]", mdInfo.id.c_str(), pSpecificInstrument);
    }
}

void CtpMdGateway::OnRtnDepthMarketData(CThostFtdcDepthMarketDataField *pDepthMarketData) {
    Logger::getLogger().info("%s OnRtnDepthMarketData instrument=[%s] time=[%s]", mdInfo.id.c_str(),
                             pDepthMarketData->InstrumentID, pDepthMarketData->UpdateTime);
    Tick *tickData = new Tick();
    tickData->tradingDay = pDepthMarketData->TradingDay;
    tickData->symbol = pDepthMarketData->InstrumentID;
    tickData->exchange = pDepthMarketData->ExchangeID;
    tickData->lastPrice = pDepthMarketData->LastPrice;
    tickData->preSettlePrice = pDepthMarketData->PreSettlementPrice;
    tickData->openPrice = pDepthMarketData->OpenPrice;
    tickData->volume = pDepthMarketData->Volume;
    tickData->actionTime = pDepthMarketData->UpdateTime;
    //todo
    Util::getTime(&tickData->timeStampRecv);
    //this->OnTickData(tickData);
    this->queue->push(Event(EventType::TICK,tickData));
}

void CtpMdGateway::reSubscribe() {
    if (contracts.size() > 0) {
        Logger::getLogger().info("%s reSubscribe count:[%d]", mdInfo.id.c_str(), contracts.size());
        char **str = new char *[contracts.size() + 1];
        int i = 0;
        for (auto &item: contracts) {
            str[i++] = const_cast<char *>(item.c_str());
        }
        int ret = this->m_pUserApi->SubscribeMarketData(str, contracts.size());
        Logger::getLogger().info("%s reSubscribe  ret = %d", mdInfo.id.c_str(), ret);
    }
}
