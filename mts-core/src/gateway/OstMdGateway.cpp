//
// Created by 段晴 on 2022/2/18.
//
#include "OstMdGateway.h"

int OstMdGateway::nRequestID=0;
int OstMdGateway::connect() {
//        void *handle = dlopen("../lib/ost/libutmdapi.so", RTLD_LAZY);
//        if (handle != nullptr) {
//            typedef CUTMDApi *(*CreateApiMdFunc)(const char *);
//            CreateApiMdFunc pfnCreateFtdcMdApiFunc = (CreateApiMdFunc) dlsym(handle,
//                                                                             "_ZN8CUTMDApi11CreateMDApiEPKcib");
//            if (pfnCreateFtdcMdApiFunc == nullptr) {
//                logi("load thosttraderapi.so fail [{}] [{}]", errno, hstrerror(errno));
//                return -1;
//            }
//            m_pUserApi = pfnCreateFtdcMdApiFunc(".");
//            m_pUserApi->RegisterSpi(this);
//            m_pUserApi->test();
//        } else {
//            logi("load thosttraderapi.dll fail  [{}]", errno, dlerror());
//            return -1;
//        }

//创建api;将参数nCPUID设置为需要绑定的CPU,可开启极速模式
    //如果同一进程内创建多个api，参数pszFlowPath必须设置为不同的路径
    m_pUserApi = CUTMDApi::CreateMDApi();
    m_pUserApi->RegisterSpi(this);
    thread t([this](){
        this->Run();
    });
    t.detach();
    return 0;
}
void OstMdGateway::disconnect(){

}

void OstMdGateway::Run() {
        const char *address = account->loginInfo.mdAddress.c_str();
        m_pUserApi->RegisterFront(const_cast<char *>(address));
        m_pUserApi->Init();
        m_pUserApi->Join();
}

void OstMdGateway::subscribe(set<string> &subContracts){
    int i = 0;
    for (auto &item: subContracts) {
        if (this->contracts.contains(item))
            continue;
        this->contracts.insert(item);
        CUTSubInstrumentField req={0};
        req.ExchangeID=UT_EXG_SZSE; //todo 交易所映射
        strcpy(req.InstrumentID, item.c_str());
        int ret = this->m_pUserApi->SubscribeDepthMarketData(&req, 1);
        logi("subscribeContract {} ret={}", item, ret);
    }
}
void OstMdGateway::reSubscribe() {
    for(auto & item:this->contracts){
        CUTSubInstrumentField req={0};
        req.ExchangeID=UT_EXG_SZSE; //todo 交易所映射
        strcpy(req.InstrumentID, item.c_str());
        int ret = this->m_pUserApi->SubscribeDepthMarketData(&req, 1);
        logi("subscribeContract {} ret={}", item, ret);
    }

}

void OstMdGateway::OnFrontConnected() {
    fmtlog::setThreadName("MdGateway");
    logi("OnFrontConnected");
    CUTReqLoginField reqLoginField={0};
    strcpy(reqLoginField.UserID, account->loginInfo.userId.c_str());
    strcpy(reqLoginField.Password, account->loginInfo.password.c_str());
    int ret=m_pUserApi->ReqLogin(&reqLoginField,this->nRequestID++);
    logi("ReqLogin ret:",ret);
}

void OstMdGateway::OnFrontDisconnected(int nReason) {
    logi("OnFrontDisconnected n=",nReason);
    this->isConnected= false;
}

void OstMdGateway::OnRspError(CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    loge("OnRspError {}",pRspInfo->ErrorMsg);
}

void OstMdGateway::OnRspLogin(CUTRspLoginField *pRspLogin, CUTRspInfoField *pRspInfo, int nRequestID, bool bIsLast) {
    logi("OnRspLogin");

    if (bIsLast && pRspInfo->ErrorID == 0) {
        this->tradingDay = pRspLogin->TradingDay;
        logi("MD  行情接口连接成功,交易日 = {}", this->tradingDay);
        this->isConnected = true;
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
        //重新订阅
        this->reSubscribe();
    } else {
        loge("MD 行情接口连接失败, ErrorMsg={}", pRspInfo->ErrorMsg);
    }

}

void
OstMdGateway::OnRspSubDepthMarketData(CUTSubInstrumentField *pSubInstrument, CUTRspInfoField *pRspInfo, int nRequestID,
                                      bool bIsLast) {
    if (pRspInfo->ErrorID == 0) {
        logi("MD OnRspSubMarketData {}",pSubInstrument->InstrumentID);
    }else
        loge("MD OnRspSubMarketData fail {} {}",pSubInstrument->InstrumentID,Util::g2u(pRspInfo->ErrorMsg));

}

void OstMdGateway::OnRtnDepthMarketData(CUTDepthMarketDataField *pDepthMarketData) {
    logi("MD OnRtnTick instrument=[{}] time=[{}] price=[{}]",
                            pDepthMarketData->InstrumentID, pDepthMarketData->UpdateTime,pDepthMarketData->LastPrice);
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
