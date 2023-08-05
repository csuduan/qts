//
// Created by 段晴 on 2022/6/8.
//

#include "quotaExecutor.h"
#include "common/util.hpp"
#include "gateway/gatewayFactory.hpp"
#include "common/taskScheduler.hpp"
#include "context.h"

void QuotaExecutor::start() {
    //创建quotes
    for (auto &quotaInfo: Context::get().setting.quotes) {
        if (quotaInfo.enable == false)
            continue;
        if (quotaInfo.subList.length() > 0) {
            vector<string> tmp;
            Util::split(quotaInfo.subList, tmp, ",");
            for (auto &item: tmp)
                quotaInfo.subSet.insert(item);
            tmp.clear();
        }

        if (quotaInfo.subFile.length() > 0) {
            std::ifstream ifs;
            string file = "conf/" + quotaInfo.subFile;
            ifs.open(file, ios::in);
            if (ifs.is_open()) {
                string line;
                while (getline(ifs, line)) {
                    if (line.length() == 0)
                        continue;
                    if (line.find("#") != string::npos)
                        continue;
                    vector<string> tmp;
                    Util::split(line, tmp, ".");
                    quotaInfo.subSet.insert(tmp[0]);
                    tmp.clear();
                }
                ifs.close();
            } else
                loge("open file:{} fail", file);

        }


        MdGateway *mdGateway = GatewayFactory::createMdGateway(&quotaInfo);
        this->mdGateways.emplace_back(mdGateway);
    }


    std::thread msgThread([this]() {
        this->msgHanler();
    });
    msgThread.detach();


    TaskScheduler *scheduler = new TaskScheduler;
    scheduler->addTask("test", "0 0 * * * ?", []() {
        logi("test---");
    });
    scheduler->addTask("connect", "0 25 09 * * *", [this]() {
        for (auto gateway: this->mdGateways)
            gateway->connect();
    });
    scheduler->addTask("disconnect", "0 30 15 * * *", [this]() {
        for (auto gateway: this->mdGateways)
            gateway->disconnect();
    });

    scheduler->start();


}

void QuotaExecutor::msgHanler() {
    Event event;
    map<string, QuotaFile *> fileMap;

    while (true) {
        //轮询所有的quote队列
        bool find = false;
        for (auto &mdGateway: this->mdGateways) {
            if (mdGateway != nullptr && !mdGateway->getQueue()->isEmpty()) {
                if (mdGateway->getQueue()->isFull()) {
                    loge("md[{}] queue is full!", mdGateway->id);
                }
                mdGateway->getQueue()->pop(event);
                find = true;
                try {
                    switch (event.type) {
                        case EvType::TICK: {
                            //Tick *tick = std::any_cast<Tick *>(event.data);
                            Tick *tick = (Tick *) event.data;
                            if (tick == nullptr)
                                continue;

                            string id = mdGateway->id;
                            string tradingDay = tick->tradingDay;
                            if (fileMap.count(id) == 0) {
                                QuotaFile *quotaFile = new QuotaFile{};
                                fileMap[id] = quotaFile;
                            }
                            auto qFile = fileMap[id];
                            if (tradingDay != qFile->date) {
                                if (qFile->ofs.is_open())
                                    qFile->ofs.close();
                                qFile->date = tradingDay;
                                qFile->fname = Context::get().setting.dataPath + "/" + id + "-" + tradingDay + ".dat";
                                qFile->ofs.open(qFile->fname, ios::app);
                                qFile->ofs
                                        << "#symbol,tradingDay,actionDay,updateTime,preClosePrice,preSettlePrice,openPrice,lowPrice,highPrice,lowerLimit,upperLimit,"
                                        << "lastPrice,lastVolume,bidPrice1,bidPrice2,bidPrice3,bidPrice4,bidPrice5,askPrice1,askPrice2,askPrice3,askPrice4,askPrice5,"
                                        << "bidVolume1,bidVolume2,bidVolume3,bidVolume4,bidVolume5,askVolume1,askVolume2,askVolume3,askVolume4,askVolume5"
                                        << endl;
                            }
                            string dat = fmt::format(
                                    "{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}",
                                    tick->symbol, tick->tradingDay, tick->actionDay, tick->updateTime,
                                    tick->preClosePrice, tick->preSettlePrice,
                                    tick->openPrice, tick->lowPrice, tick->highPrice, tick->lowerLimit,
                                    tick->upperLimit,
                                    tick->lastPrice, tick->lastVolume,
                                    tick->bidPrice1, tick->bidPrice2, tick->bidPrice3, tick->bidPrice4, tick->bidPrice5,
                                    tick->askPrice1, tick->askPrice2, tick->askPrice3, tick->askPrice4, tick->askPrice5,
                                    tick->bidVolume1, tick->bidVolume2, tick->bidVolume3, tick->bidVolume4,
                                    tick->bidVolume5,
                                    tick->askVolume1, tick->askVolume2, tick->askVolume3, tick->askVolume4,
                                    tick->askVolume5);
                            qFile->ofs << dat << endl;
                            //delete tick;
                            //event.data.reset();
                            break;
                        }
                        default: {


                            break;
                        }
                    }
                    if (event.data != nullptr) {
                        delete event.data;
                        event.data = nullptr;
                    }
                } catch (exception ex) {
                    loge("event hanler exception!{}", ex.what());
                }

            }
        }
        if (!find)
            std::this_thread::sleep_for(std::chrono::microseconds(100));
    }
}

void QuotaExecutor::connect() {

}

void QuotaExecutor::disconnect() {

}
