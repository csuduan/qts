//
// Created by 段晴 on 2022/6/7.
//

#ifndef MTS_CORE_TORAL2MDGATEWAY_H
#define MTS_CORE_TORAL2MDGATEWAY_H

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <iostream>
#include "tora/TORATstpLev2MdApi.h"
using namespace TORALEV2API;
using namespace  std;
#include "gateway.h"


class ToraL2MdGateway:public CTORATstpLev2MdSpi,public MdGateway{

public:
    ToraL2MdGateway(QuoteInfo *quotaInfo): MdGateway(quotaInfo){
    }

    ///当客户端与交易后台建立起通信连接时（还未登录前），该方法被调用。
    virtual void OnFrontConnected()
    {
        fmtlog::setThreadName("MdGateway");
        logi("Md[{}] OnFrontConnected",this->id);
        CTORATstpReqUserLoginField acc{0};
        //memset(&acc, 0, sizeof(acc));
        strcpy(acc.LogInAccount, quotaInfo->user.c_str());
        acc.LogInAccountType = TORA_TSTP_LACT_UserID;
        strcpy(acc.Password, quotaInfo->pwd.c_str());

        int ret = m_api->ReqUserLogin(&acc, this->nRequestID++);
        logi("Md[{}] ReqLogin ret:{}",this->id,ret);

    };

    virtual void OnFrontDisconnected(int nReason)
    {
        logw("Md[{}] OnFrontDisconnected! nReason[{}]",this->id, nReason);
        this->setStatus(false);

    };

    ///错误应答
    virtual void OnRspError(CTORATstpRspInfoField* pRspInfo, int nRequestID)
    {
        loge("Md[{}] OnRspError {}", this->id,pRspInfo->ErrorMsg);
    };

    ///登录请求响应
    virtual void OnRspUserLogin(CTORATstpRspUserLoginField* pRspUserLogin, CTORATstpRspInfoField* pRspInfo, int nRequestID, bool bIsLast)
    {
        if (pRspInfo && pRspInfo->ErrorID == 0)
        {
            this->tradingDay = pRspUserLogin->TradingDay;
            if(this->tradingDay.length()==0)
                this->tradingDay=Util::getDate();
            logi("Md[{}]  行情接口连接成功,交易日 = {}",this->id, this->tradingDay);
            this->setStatus(true);
            std::this_thread::sleep_for(std::chrono::milliseconds(1000));
            int exchange;
            if(this->id.find("-sh") != string::npos){
                //上海
                exchange = TORA_TSTP_EXD_SSE;
            }else{
                //深圳
                exchange = TORA_TSTP_EXD_SZSE;
            }

            //订阅所有合约
            char* Securities[1];
            Securities[0] = (char*)"00000000";

	// 快照行情订阅
            int ret_md = m_api->SubscribeMarketData(Securities, sizeof(Securities) / sizeof(char*), exchange);
            if (ret_md == 0)
            {
                logi("SubscribeMarketData:::Success,ret={}", ret_md);
            }
            else
            {
                loge("SubscribeMarketData:::Failed, ret={}", ret_md);
            }
//
//#if 1	// 逐笔成交订阅
//            int ret_t = m_api->SubscribeTransaction(Securities, sizeof(Securities) / sizeof(char*), exchange);
//            if (ret_t == 0)
//            {
//                printf("SubscribeTransaction:::Success,ret=%d\n", ret_t);
//            }
//            else
//            {
//                printf("SubscribeTransaction:::Failed,ret=%d)\n", ret_t);
//            }
//#endif
//
//#if 1	// 逐笔委托订阅
//            int ret_od = m_api->SubscribeOrderDetail(Securities, sizeof(Securities) / sizeof(char*), exchange);
//            if (ret_od == 0)
//            {
//                printf("SubscribeOrderDetail:::Success,ret=%d", ret_od);
//            }
//            else
//            {
//                printf("SubscribeOrderDetail:::Failed, ret=%d)", ret_od);
//            }
//#endif
//
//#if 1	// 指数行情订阅
//            int ret_i = m_api->SubscribeIndex(Securities, sizeof(Securities) / sizeof(char*), eid);
//            if (ret_i == 0)
//            {
//                printf("SubscribeIndex:::Success,ret=%d", ret_i);
//            }
//            else
//            {
//                printf("SubscribeIndex:::Failed, ret=%d)", ret_i);
//            }
//#endif
//
//#if 1   //新债逐笔订阅
//            int ret_i = m_api->SubscribeXTSTick(Securities, sizeof(Securities) / sizeof(char*), TORA_TSTP_EXD_SSE);
//            if (ret_i == 0)
//            {
//                printf("SubscribeXTSTick:::Success,ret=%d", ret_i);
//            }
//            else
//            {
//                printf("SubscribeXTSTick:::Failed, ret=%d)", ret_i);
//            }
//
//
//#endif
        }
        else
        {
            loge("OnRspUserLogin fail!");
            this->setStatus(false);

        }
    };


    // 登出请求响应
    virtual void OnRspUserLogout(CTORATstpUserLogoutField* pUserLogout, CTORATstpRspInfoField* pRspInfo, int nRequestID, bool bIsLast)
    {
        logw("OnRspUserLogout!");
        this->setStatus(false);
    };

    // 订阅快照行情应答
    virtual void OnRspSubMarketData(CTORATstpSpecificSecurityField* pSpecificSecurity, CTORATstpRspInfoField* pRspInfo, int nRequestID, bool bIsLast)
    {
        if (pRspInfo && pRspInfo->ErrorID == 0 && pSpecificSecurity)
        {
            logi("OnRspSubMarketData SecurityID[{}] ExchangeID[{}] Success!", pSpecificSecurity->SecurityID, pSpecificSecurity->ExchangeID);

        }
    };


//    // 订阅逐笔成交行情应答
//    virtual void OnRspSubTransaction(CTORATstpSpecificSecurityField* pSpecificSecurity, CTORATstpRspInfoField* pRspInfo, int nRequestID, bool bIsLast)
//    {
//        if (pRspInfo && pRspInfo->ErrorID == 0 && pSpecificSecurity)
//        {
//            printf("OnRspSubTransaction SecurityID[%s] ExchangeID[%c] Success!\n", pSpecificSecurity->SecurityID, pSpecificSecurity->ExchangeID);
//
//        }
//    };
//
//    // 订阅逐笔委托行情应答
//    virtual void OnRspSubOrderDetail(CTORATstpSpecificSecurityField* pSpecificSecurity, CTORATstpRspInfoField* pRspInfo, int nRequestID, bool bIsLast)
//    {
//        if (pRspInfo && pRspInfo->ErrorID == 0 && pSpecificSecurity)
//        {
//            printf("OnRspSubOrderDetail SecurityID[%s] ExchangeID[%c] Success!\n", pSpecificSecurity->SecurityID, pSpecificSecurity->ExchangeID);
//
//        }
//    };
//    //订阅新债逐笔行情应答
//    virtual void OnRspSubXTSTick(CTORATstpSpecificSecurityField* pSpecificSecurity, CTORATstpRspInfoField* pRspInfo, int nRequestID, bool bIsLast)
//    {
//        if (pRspInfo && pRspInfo->ErrorID == 0 && pSpecificSecurity)
//        {
//            printf("OnRspSubXTSTick SecurityID[%s] ExchangeID[%c] Success!\n", pSpecificSecurity->SecurityID, pSpecificSecurity->ExchangeID);
//
//        }
//    };


    // 快照行情通知
    virtual void OnRtnMarketData(CTORATstpLev2MarketDataField* pDepthMarketData, const int FirstLevelBuyNum, const int FirstLevelBuyOrderVolumes[], const int FirstLevelSellNum, const int FirstLevelSellOrderVolumes[])
    {
        long tsc=Context::get().tn.rdtsc();
        //float updateTime=(float)pDepthMarketData->/1000;

        //todo tick从对象池获取
        Tick *tickData = new Tick();
        tickData->tradingDay = this->tradingDay;
        tickData->symbol = pDepthMarketData->SecurityID;
        tickData->exchange = pDepthMarketData->ExchangeID;
        tickData->preClosePrice = pDepthMarketData->PreClosePrice;
        tickData->openPrice = pDepthMarketData->OpenPrice;
        tickData->lowPrice=pDepthMarketData->LowestPrice;
        tickData->lowerLimit=pDepthMarketData->LowerLimitPrice;
        tickData->highPrice=pDepthMarketData->HighestPrice;
        tickData->upperLimit= pDepthMarketData->UpperLimitPrice;
        tickData->volume = pDepthMarketData->TotalVolumeTrade;
        tickData->lastPrice = pDepthMarketData->LastPrice;

        tickData->updateTime = pDepthMarketData->DataTimeStamp;
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
        tickData->askVolume2 = pDepthMarketData->AskVolume2;
        tickData->askVolume3 = pDepthMarketData->AskVolume3;
        tickData->askVolume4 = pDepthMarketData->AskVolume4;
        tickData->askVolume5 = pDepthMarketData->AskVolume5;

        tickData->bidVolume1 = pDepthMarketData->BidVolume1;
        tickData->bidVolume2 = pDepthMarketData->BidVolume2;
        tickData->bidVolume3 = pDepthMarketData->BidVolume3;
        tickData->bidVolume4 = pDepthMarketData->BidVolume4;
        tickData->bidVolume5 = pDepthMarketData->BidVolume5;

        tickData->recvTsc=tsc;
        this->msgQueue->push(Event{EvType::TICK, tsc, tickData});

//        printf("OnRtnMarketData TimeStamp[%d]  SecurityID[%s] ExchangeID[%c]  PreClosePrice[%f] LowestPrice[%f] HighestPrice[%f] OpenPrice[%f] LastPrice[%f]"
//               "BidPrice{[%f] [%f] [%f] [%f] [%f] [%f] [%f] [%f] [%f] [%f]}"
//               "AskPrice{[%f] [%f] [%f] [%f] [%f] [%f] [%f] [%f] [%f] [%f]}"
//               "BidVolume{[%lld] [%lld] [%lld] [%lld] [%lld] [%lld] [%lld] [%lld] [%lld] [%lld]}"
//               "AskVolume{[%lld] [%lld] [%lld] [%lld] [%lld] [%lld] [%lld] [%lld] [%lld] [%lld]}",
//               pDepthMarketData->DataTimeStamp,
//               pDepthMarketData->SecurityID,
//               pDepthMarketData->ExchangeID, pDepthMarketData->PreClosePrice, pDepthMarketData->LowestPrice,
//               pDepthMarketData->HighestPrice, pDepthMarketData->OpenPrice,
//               pDepthMarketData->LastPrice,
//               pDepthMarketData->BidPrice1, pDepthMarketData->BidPrice2, pDepthMarketData->BidPrice3,
//               pDepthMarketData->BidPrice4, pDepthMarketData->BidPrice5, pDepthMarketData->BidPrice6,
//               pDepthMarketData->BidPrice7, pDepthMarketData->BidPrice8, pDepthMarketData->BidPrice9, pDepthMarketData->BidPrice10,
//               pDepthMarketData->AskPrice1, pDepthMarketData->AskPrice2, pDepthMarketData->AskPrice3,
//               pDepthMarketData->AskPrice4, pDepthMarketData->AskPrice5, pDepthMarketData->AskPrice6,
//               pDepthMarketData->AskPrice7, pDepthMarketData->AskPrice8, pDepthMarketData->AskPrice9, pDepthMarketData->AskPrice10,
//               pDepthMarketData->BidVolume1, pDepthMarketData->BidVolume2, pDepthMarketData->BidVolume3,
//               pDepthMarketData->BidVolume4, pDepthMarketData->BidVolume5, pDepthMarketData->BidVolume6,
//               pDepthMarketData->BidVolume7, pDepthMarketData->BidVolume8, pDepthMarketData->BidVolume9, pDepthMarketData->BidVolume10,
//               pDepthMarketData->AskVolume1, pDepthMarketData->AskVolume2, pDepthMarketData->AskVolume3,
//               pDepthMarketData->AskVolume4, pDepthMarketData->AskVolume5, pDepthMarketData->AskVolume6,
//               pDepthMarketData->AskVolume7, pDepthMarketData->AskVolume8, pDepthMarketData->AskVolume9, pDepthMarketData->AskVolume10
//        );
//
//        printf("BuyVolumes %d{", FirstLevelBuyNum);
//
//        for (size_t index = 0; index < FirstLevelBuyNum; index++)
//        {
//            printf("%d ", FirstLevelBuyOrderVolumes[index]);
//        }
//        printf("}");
//
//        printf("SellVolumes %d{", FirstLevelSellNum);
//
//        for (size_t index = 0; index < FirstLevelSellNum; index++)
//        {
//            printf("%d ", FirstLevelSellOrderVolumes[index]);
//        }
//        printf("}\n");
    };

//    // 逐笔成交通知
//    virtual void OnRtnTransaction(CTORATstpLev2TransactionField* pTransaction)
//    {
//        printf("OnRtnTransaction SecurityID[%s] ", pTransaction->SecurityID);
//        printf("ExchangeID[%c] ", pTransaction->ExchangeID);
//        //深圳逐笔成交，TradeTime的格式为【时分秒毫秒】例如例如100221530，表示10:02:21.530;
//        //上海逐笔成交，TradeTime的格式为【时分秒百分之秒】例如10022153，表示10:02:21.53;
//        printf("TradeTime[%d] ", pTransaction->TradeTime);
//        printf("TradePrice[%.4f] ", pTransaction->TradePrice);
//        printf("TradeVolume[%lld] ", pTransaction->TradeVolume);
//        printf("ExecType[%c] ", pTransaction->ExecType);//上海逐笔成交没有这个字段，只有深圳有。值2表示撤单成交，BuyNo和SellNo只有一个是非0值，以该非0序号去查找到的逐笔委托即为被撤单的委托。
//        printf("MainSeq[%d] ", pTransaction->MainSeq);
//        printf("SubSeq[%lld] ", pTransaction->SubSeq);
//        printf("BuyNo[%lld] ", pTransaction->BuyNo);
//        printf("SellNo[%lld] ", pTransaction->SellNo);
//        printf("TradeBSFlag[%c]\n", pTransaction->TradeBSFlag);
//        printf("Info1[%d] ", pTransaction->Info1);
//        printf("Info2[%d] ", pTransaction->Info2);
//        printf("Info3[%d] \n", pTransaction->Info3);
//
//    };
//
//    // 订阅逐笔委托行情通知
//    virtual void OnRtnOrderDetail(CTORATstpLev2OrderDetailField* pOrderDetail)
//    {
//
//        printf("OnRtnOrderDetail SecurityID[%s] ", pOrderDetail->SecurityID);
//        printf("ExchangeID[%d] ", pOrderDetail->ExchangeID);
//        printf("OrderTime[%d] ", pOrderDetail->OrderTime);
//        printf("Price[%.4f] ", pOrderDetail->Price);
//        printf("Volume[%lld] ", pOrderDetail->Volume);
//        printf("OrderType[%c] ", pOrderDetail->OrderType);
//        printf("MainSeq[%d] ", pOrderDetail->MainSeq);
//        printf("SubSeq[%d] ", pOrderDetail->SubSeq);
//        printf("Side[%c] ", pOrderDetail->Side);
//        printf("BizIndex[%d] ", pOrderDetail->BizIndex);
//        printf("Info1[%d] ", pOrderDetail->Info1);
//        printf("Info2[%d] ", pOrderDetail->Info2);
//        printf("Info3[%d] \n", pOrderDetail->Info3);
//
//    };
//
//    // 订阅指数行情应答
//    virtual void OnRspSubIndex(CTORATstpSpecificSecurityField* pSpecificSecurity, CTORATstpRspInfoField* pRspInfo, int nRequestID, bool bIsLast)
//    {
//        if (pRspInfo && pRspInfo->ErrorID == 0 && pSpecificSecurity)
//        {
//            printf("OnRspSubIndex SecurityID[%s] ExchangeID[%c] Success!\n", pSpecificSecurity->SecurityID, pSpecificSecurity->ExchangeID);
//
//        }
//    };
//    // 订阅逐笔委托行情通知
//    virtual void OnRtnIndex(CTORATstpLev2IndexField* pIndex)
//    {
//        printf("OnRtnIndex SecurityID[%s] ", pIndex->SecurityID);
//        printf("ExchangeID[%d] ", pIndex->ExchangeID);
//        printf("DataTimeStamp[%d] ", pIndex->DataTimeStamp);//精确到秒，上海指数5秒一笔，深圳3秒一笔
//        printf("LastIndex[%.2f] ", pIndex->LastIndex);
//        printf("PreCloseIndex[%.2f] ", pIndex->PreCloseIndex);
//        printf("OpenIndex[%.2f] ", pIndex->OpenIndex);
//        printf("LowIndex[%.2f] ", pIndex->LowIndex);
//        printf("HighIndex[%.2f] ", pIndex->HighIndex);
//        printf("CloseIndex[%.2f] ", pIndex->CloseIndex);
//        printf("Turnover[%.2f] ", pIndex->Turnover);
//        printf("TotalVolumeTraded[%lld]\n", pIndex->TotalVolumeTraded);
//    };
//    //订阅新债逐笔行情通知
//    virtual void OnRtnXTSTick(CTORATstpLev2XTSTickField* pIndex)
//    {
//        printf("OnRtnXTSTick SecurityID[%s] ", pIndex->SecurityID);
//        printf("ExchangeID[%d] ", pIndex->ExchangeID);
//        printf("MainSeq[%d] ", pIndex->MainSeq);
//        printf("SubSeq[%d] ", pIndex->SubSeq);
//        printf("TickTime[%d] ", pIndex->TickTime);
//        printf("TickType[%d] ", pIndex->TickType);
//        printf("BuyNo[%d] ", pIndex->BuyNo);
//        printf("SellNo[%d] ", pIndex->SellNo);
//        printf("Price[%d] ", pIndex->Price);
//        printf("Volume[%d] ", pIndex->Volume);
//        printf("TradeMoney[%d] ", pIndex->TradeMoney);
//        printf("Side[%d] ", pIndex->Side);
//        printf("TradeBSFlag[%d] ", pIndex->TradeBSFlag);
//        printf("MDSecurityStat[%d] ", pIndex->MDSecurityStat);
//        printf("Info1[%d] ", pIndex->Info1);
//        printf("Info2[%d] ", pIndex->Info2);
//        printf("Info3[%d] ", pIndex->Info3);
//    };


    void subscribe(set<string> &contracts) override{

    }
    int  connect() override{
        void *handle = dlopen("lib/tora/liblev2mdapi.so", RTLD_LAZY);
        if(handle == nullptr){
            logi("MD[{}] load lib fail  [{}]",this->id, errno, dlerror());
            return -1;
        }

        typedef CTORATstpLev2MdApi *(*CreateApiMdFunc)(const char *);
        CreateApiMdFunc pfnCreateFtdcMdApiFunc = (CreateApiMdFunc) dlsym(handle,
                                                                         "_ZN11TORALEV2API18CTORATstpLev2MdApi19CreateTstpLev2MdApiERKcb");
        if (pfnCreateFtdcMdApiFunc == nullptr) {
            logi("MD[{}] load lib fail [{}] [{}]",this->id, errno, hstrerror(errno));
            return -1;
        }
        m_api = pfnCreateFtdcMdApiFunc(".");

        // 打印接口版本号
        //logi("Level2MdApiVersion:[{}]", CTORATstpLev2MdApi::GetApiVersion());
        // 创建接口对象
        //CTORATstpLev2MdApi* demo_md_api = CTORATstpLev2MdApi::CreateTstpLev2MdApi();//TCP方式
        //CTORATstpLev2MdApi *demo_md_api = CTORATstpLev2MdApi::CreateTstpLev2MdApi(TORA_TSTP_MST_MCAST);//组播方式、UDP方式

        // 注册回调接口
        m_api->RegisterSpi(this);
        // 注册单个行情前置服务地址
        std::thread t([this]() {
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
            if (m_api != nullptr) {
                m_api->Release();
                m_api = nullptr;
            }
        } catch (exception ex) {
            loge("{} discounnect fail ,{}", quotaInfo->id, ex.what());
        }
    }

private:
    CTORATstpLev2MdApi* m_api;
    static inline int nRequestID =0 ;

    void run(){
        const char *address = quotaInfo->address.c_str();
        m_api->RegisterFront(const_cast<char *>(address));
        // 注册组播地址
        //demo_md_api->RegisterMulticast((char*)"udp://224.224.224.236: 8889", (char*)"172.16.22.203",NULL);	//请注意接口初始化时的输入参数,第三个参数用NULL就可以

        m_api->Init();
        m_api->Join();
    }
};

#endif //MTS_CORE_TORAL2MDGATEWAY_H
