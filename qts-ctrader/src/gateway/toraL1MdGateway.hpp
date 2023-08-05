//
// Created by 段晴 on 2022/6/10.
//

#ifndef MTS_CORE_TORAL3MDGATEWAY_H
#define MTS_CORE_TORAL3MDGATEWAY_H

#include "context.h"
#include "tora/TORATstpXMdApi.h"
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "data.h"
#include "gateway.h"
#include <dlfcn.h>
#include <netdb.h>

namespace TORALEV1API {
    class ToraL1MdGateway : public CTORATstpXMdSpi, public MdGateway {

    public:
        ToraL1MdGateway(QuoteInfo *quotaInfo) : MdGateway(quotaInfo) {
        }
        
        virtual void OnFrontConnected() {
            fmtlog::setThreadName("MdGateway");
            logi("Md[{}] OnFrontConnected", this->id);

            CTORATstpReqUserLoginField req_user_login_field{0};
            // memset(&req_user_login_field, 0, sizeof(req_user_login_field));

            int ret = m_api->ReqUserLogin(&req_user_login_field, this->nRequestID++);
            logi("Md[{}] ReqLogin ret:{}", this->id, ret);
        }

        virtual void OnFrontDisconnected(int nReanson) {
            logw("Md[{}] OnFrontDisconnected! nReason[{}]", this->id, nReanson);
            this->setStatus(false);
        }

        virtual void OnRspUserLogin(CTORATstpRspUserLoginField *pRspUserLoginField,
                                    CTORATstpRspInfoField *pRspInfoField,
                                    int nRequestID) {
            if (pRspInfoField->ErrorID == 0) {
                this->tradingDay = pRspUserLoginField->TradingDay;
                if (this->tradingDay.length() == 0)
                    this->tradingDay = Util::getDate();
                logi("Md[{}]  行情接口连接成功,交易日 = {}", this->id, this->tradingDay);
                this->setStatus(true);
                std::this_thread::sleep_for(std::chrono::milliseconds(1000));


                //订阅所有合约
                // char *Securities[1];
                // Securities[0] = (char *) "688598";


                if (quotaInfo->subSet.size() == 0)
                    quotaInfo->subSet.insert("00000000");
                char *Securities[quotaInfo->subSet.size()];
                int i = 0;
                for (auto &str: quotaInfo->subSet) {
                    Securities[i++] = (char *) str.c_str();
                }


                // 快照行情订阅
                int ret_md = m_api->SubscribeMarketData(
                        Securities, quotaInfo->subSet.size(), TORA_TSTP_EXD_SSE);
                if (ret_md == 0) {
                    logi("SubscribeMarketData:::Success,ret={}", ret_md);
                } else {
                    loge("SubscribeMarketData:::Failed, ret={}", ret_md);
                }
            } else {
                loge("OnRspUserLogin fail!");
                this->setStatus(false);
            }
        }

        virtual void
        OnRspUserLogout(CTORATstpUserLogoutField *pUserLogoutField,
                        TORALEV1API::CTORATstpRspInfoField *pRspInfoField,
                        int nRequestID) {
            logw("OnRspUserLogout!");
            this->setStatus(false);
        }

        virtual void
        OnRspSubMarketData(CTORATstpSpecificSecurityField *pSpecificSecurityField,
                           TORALEV1API::CTORATstpRspInfoField *pRspInfoField) {
            if (pRspInfoField && pRspInfoField->ErrorID == 0 &&
                pSpecificSecurityField) {
                logi("OnRspSubMarketData SecurityID[{}] ExchangeID[{}] Success!",
                     pSpecificSecurityField->SecurityID,
                     pSpecificSecurityField->ExchangeID);
            }
        }

        virtual void OnRtnMarketData(CTORATstpMarketDataField *pDepthMarketData) {
            long tsc = Context::get().tn.rdtsc();
            // todo tick从对象池获取

            Tick *tickData = new Tick();
            tickData->tradingDay = this->tradingDay;
            tickData->symbol = pDepthMarketData->SecurityID;
            tickData->exchange = pDepthMarketData->ExchangeID;
            tickData->preClosePrice = pDepthMarketData->PreClosePrice;
            tickData->openPrice = pDepthMarketData->OpenPrice;
            tickData->lowPrice = pDepthMarketData->LowestPrice;
            tickData->lowerLimit = pDepthMarketData->LowerLimitPrice;
            tickData->highPrice = pDepthMarketData->HighestPrice;
            tickData->upperLimit = pDepthMarketData->UpperLimitPrice;
            tickData->volume = pDepthMarketData->Volume;
            tickData->lastPrice = pDepthMarketData->LastPrice;

            tickData->updateTime =
                    Util::str2time(string(pDepthMarketData->UpdateTime)) +
                    pDepthMarketData->UpdateMillisec / 1000.0;

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

            tickData->recvTsc = tsc;
            // delete tickData;
            this->msgQueue->push(Event{EvType::TICK, tsc, tickData});
        }

        void subscribe(set<string> &contracts) override {}

        int connect() override {
            void *handle = dlopen("lib/tora/libxfastmdapi.so", RTLD_LAZY);
            if (handle == nullptr) {
                logi("MD[{}] load lib fail  [{}]", this->id, errno, dlerror());
                return -1;
            }

            using CreateApiMdFunc = CTORATstpXMdApi *(*)(char const &, char const &);
            CreateApiMdFunc pfnCreateFtdcMdApiFunc = (CreateApiMdFunc) dlsym(
                    handle, "_ZN11TORALEV1API15CTORATstpXMdApi16CreateTstpXMdApiERKcS2_");
            if (pfnCreateFtdcMdApiFunc == nullptr) {
                logi("MD[{}] load lib fail [{}] [{}]", this->id, errno, strerror(errno));
                return -1;
            }
            m_api = pfnCreateFtdcMdApiFunc(TORA_TSTP_MST_TCP, TORA_TSTP_MST_TCP);

            // 注册回调接口
            m_api->RegisterSpi(this);
            // 注册单个行情前置服务地址
            std::thread t([this]() { this->run(); });
            t.detach();

            return 0;
        }

        void disconnect() override {
            if (this->connected == false)
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
        CTORATstpXMdApi *m_api;
        static inline int nRequestID = 0;

        void run() {
            const char *address = quotaInfo->address.c_str();
            m_api->RegisterFront(const_cast<char *>(address));
            // 注册组播地址
            // demo_md_api->RegisterMulticast((char*)"udp://224.224.224.236: 8889",
            // (char*)"172.16.22.203",NULL);
            // //请注意接口初始化时的输入参数,第三个参数用NULL就可以

            m_api->Init();
            m_api->Join();
        }
    };
} // namespace TORALEV1API

#endif // MTS_CORE_TORAL3MDGATEWAY_H
