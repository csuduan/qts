//
// Created by 段晴 on 2022/2/18.
//
#include "OstMdGateway.h"

int OstMdGateway::nRequestID = 0;

map<TUTExchangeIDType,string> OstMdGateway::exgMap ={
        {UT_EXG_SSE,"SSE"},
        {UT_EXG_SZSE,"SZE"},
        {UT_EXG_SHFE,"SHFE"},
        {UT_EXG_CFFEX,"CFFEX"},
        {UT_EXG_DCE,"DCE"},
        {UT_EXG_CZCE,"CZCE"},
        {UT_EXG_INE,"INE"},
        {UT_EXG_HKEX,"HK"}
};
map<string,TUTExchangeIDType> OstMdGateway::reExgMap={
        {"SSE",UT_EXG_SSE},
        {"SZE",UT_EXG_SZSE},
        {"SHFE",UT_EXG_SHFE},
        {"CFFEX",UT_EXG_CFFEX},
        {"DCE",UT_EXG_DCE},
        {"CZCE",UT_EXG_CZCE},
        {"INE",UT_EXG_INE},
        {"HK",UT_EXG_HKEX}
};


int OstMdGateway::connect() {
    void *handle = dlopen("lib/ost/libutmdapi.so", RTLD_LAZY);
    if(handle == nullptr){
        logi("{} load libutmdapi.so fail  [{}]", name,errno, dlerror());
        return -1;
    }
    typedef CUTMDApi *(*CreateApiMdFunc)(const char *);
    CreateApiMdFunc pfnCreateFtdcMdApiFunc = (CreateApiMdFunc) dlsym(handle,
                                                                     "_ZN8CUTMDApi11CreateMDApiEPKcib");
    if (pfnCreateFtdcMdApiFunc == nullptr) {
        logi("{} load libutmdapi.so fail [{}] [{}]",name, errno, hstrerror(errno));
        return -1;
    }
    m_pUserApi = pfnCreateFtdcMdApiFunc(".");
    //创建api;将参数nCPUID设置为需要绑定的CPU,可开启极速模式
    //如果同一进程内创建多个api，参数pszFlowPath必须设置为不同的路径
    //m_pUserApi = CUTMDApi::CreateMDApi();
    m_pUserApi->RegisterSpi(this);
    thread t([this]() {
        this->Run();
    });
    t.detach();
    return 0;
}

void OstMdGateway::disconnect() {

}

void OstMdGateway::Run() {
    const char *address = quote->address.c_str();
    m_pUserApi->RegisterFront(const_cast<char *>(address));
    m_pUserApi->Init();
    m_pUserApi->Join();
}

void OstMdGateway::subscribe(set<string> &subContracts) {
    int i = 0;
    for (auto &item: subContracts) {
        if (quote->subList.contains(item))
            continue;
        quote->subList.insert(item);

        CUTSubInstrumentField req = {0};
        if(item.starts_with("6"))
            req.ExchangeID= UT_EXG_SSE;
        else
            req.ExchangeID = UT_EXG_SZSE;
        strcpy(req.InstrumentID, item.c_str());

        if(isConnected){
            int ret = this->m_pUserApi->SubscribeDepthMarketData(&req, 1);
            logi("{} subscribeContract {} ret={}", name,item, ret);
        }
    }
}
void OstMdGateway::reSubscribe() {
      for(auto & item :this->quote->subList){
          CUTSubInstrumentField req = {0};
          if(item.starts_with("6"))
              req.ExchangeID= UT_EXG_SSE;
          else
              req.ExchangeID = UT_EXG_SZSE;
          strcpy(req.InstrumentID, item.c_str());
          int ret = this->m_pUserApi->SubscribeDepthMarketData(&req, 1);
      }
       logi("{} subscribeContract count={}", name,this->quote->subList.size());


}

void OstMdGateway::OnFrontConnected() {
    fmtlog::setThreadName("MdGateway");
    logi("{} OnFrontConnected",name);
    CUTReqLoginField reqLoginField = {0};
    strcpy(reqLoginField.UserID, quote->userId.c_str());
    strcpy(reqLoginField.Password, quote->password.c_str());
    int ret = m_pUserApi->ReqLogin(&reqLoginField, this->nRequestID++);
    logi("{} ReqLogin ret:{}",name, ret);
}

void OstMdGateway::OnFrontDisconnected(int nReason) {
    logi("{} OnFrontDisconnected n=", name,nReason);
    this->isConnected = false;
}

void OstMdGateway::OnRspError(CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    loge("{} OnRspError {}", name,pRspInfo->ErrorMsg);
}

void OstMdGateway::OnRspLogin(CUTRspLoginField *pRspLogin, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    if (bIsLast && pRspInfo->ErrorID == 0) {
        this->tradingDay = pRspLogin->TradingDay;
        logi("{}  行情接口连接成功,交易日 = {}",name, this->tradingDay);
        this->isConnected = true;
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
        //重新订阅
        timer.delay(5000, [this]() {
            this->reSubscribe();
        });

    } else {
        loge("{} 行情接口连接失败, ErrorMsg={}",name, pRspInfo->ErrorMsg);
    }

}

void
OstMdGateway::OnRspSubDepthMarketData(CUTSubInstrumentField *pSubInstrument, CUTRspInfoField *pRspInfo, int nRequestID,
                                      bool bIsLast) {
    if (pRspInfo->ErrorID == 0) {
        logi("{} OnRspSubMarketData {}", name,pSubInstrument->InstrumentID);
    } else
        loge("{} OnRspSubMarketData fail {} {}", name,pSubInstrument->InstrumentID, pRspInfo->ErrorMsg);

}

void OstMdGateway::OnRtnDepthMarketData(CUTDepthMarketDataField *pDepthMarketData) {
    long tsc=Context::get().tn.rdtsc();
    float updateTime=(float)pDepthMarketData->UpdateTime/1000;

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
    this->quote->queue->push(Event{EvType::TICK,tsc, tickData});
    //logi("MD OnRtnTick instrument=[{}] time=[{}] price=[{}]",
    //     pDepthMarketData->InstrumentID, updateTime, pDepthMarketData->LastPrice);
}
