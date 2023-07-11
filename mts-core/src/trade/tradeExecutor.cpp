//
// Created by 段晴 on 2022/1/22.
//

#include "tradeExecutor.h"
#include "gateway/gatewayFactory.hpp"
#include "strategy/strategyFactory.hpp"
#include "strategy/goldStrategy.hpp"
#include "config.h"
#include "xpack/json.h"
#include "monitor.h"
#include <thread>
#include <functional>
#include <unistd.h>
#include<sys/resource.h>
#include "common/socketServer.hpp"
#include "shm.hpp"
#include "shmQuote.h"
#include "context.h"
#include "cpuin.h"
#include "dataBuilder.h"
#include "message.h"

#include "signal.h"


using std::placeholders::_1;


#include <sched.h>
#include<sys/types.h>
#include<sys/sysinfo.h>
#include "magic_enum.hpp"


void TradeExecutor::start() {
    logi("tradeExecutor {} init...", this->id);
    Context::get().init(this->id);

    logi("tradeExecutor {} start udsServer...", this->id);
    //启动udsServer
//    SocketAddr udsAddr;
//    udsAddr.name = "udsServer";
//    udsAddr.type = SocketType::UDS;
//    udsAddr.unName = this->id;
    SocketAddr tcpAddr;
    tcpAddr.name = "tcpServer";
    tcpAddr.type = SocketType::TCP;
    tcpAddr.port = Context::get().setting.port;
    this->udsServer = new SocketServer(tcpAddr, this);
    thread t_uds([this]() {
        this->udsServer->start();
    });
    t_uds.detach();

    //等待创建账户
    //logi("waiting for agent connect...");
    //std::unique_lock<std::mutex> lk(mut);
    //cv.wait(lk);

    logi("tradeExecutor {} start account...", this->id);
    //从db读取账户配置
    string dbPath = Context::get().setting.db;
    this->sqliteHelper = new SqliteHelper(dbPath);
    auto acctConf=this->sqliteHelper->queryAcctConf(this->id);

    //创建账户
    this->acct=new Acct(acctConf);
    Context::get().acct=this->acct;
    this->acct->init();



    try {
        for (auto &setting: Context::get().strategySettings) {
            if (setting.accountId != this->id)
                continue;
            StrategySetting *strategySetting = buildStrategySetting(setting);
            this->createStrategy(strategySetting);


            //创建bar
            for (auto &symbol: setting.contracts) {
                if (setting.barLevel == 0)
                    continue;
                if (!barGeneratorMap.count(symbol) > 0) {
                    barGeneratorMap[symbol] = new vector<BarGenerator *>();
                }
                auto barGenvec = barGeneratorMap[symbol];
                BAR_LEVEL level = (BAR_LEVEL) setting.barLevel;
                auto it = find_if(barGenvec->begin(), barGenvec->end(), [level](BarGenerator *bg) {
                    return bg->level = level;
                });
                if (it == barGenvec->end()) {
                    //不存在
                    barGenvec->push_back(new BarGenerator(symbol, (BAR_LEVEL) setting.barLevel));
                }

            }

        }
    } catch (exception ex) {
        loge("load strategy fail,{}", ex.what());
    }


    std::thread fastEventThread(std::bind(&TradeExecutor::fastEventHandler, this));
    fastEventThread.detach();

    std::thread msgThread(std::bind(&TradeExecutor::msgHandler, this));
    msgThread.detach();

    std::thread testPushThread([this]{
//        while (true){
//            auto msg = buildMsg(MSG_TYPE::ON_ACCT, *acct->acctInfo, this->id);
//            this->udsServer->push(*msg);
//            std::this_thread::sleep_for(std::chrono::microseconds(1));
//        }
    });
    testPushThread.detach();


}

void TradeExecutor::subContract(set<string> contracts, Strategy *strategy) {
    //更新订阅列表
    for (auto &contract: contracts) {
        if (!this->subsMap.count(contract) > 0) {
            this->subsMap[contract] = std::set<Strategy *>();
//            vector<string> contracts;
//            contracts.push_back(contract);
//            this->mdGateway->subscribe(contracts);
        }
        this->subsMap[contract].insert(strategy);
    }
    //开始订阅
    //this->mdGateway->subscribe(contracts);
}



void TradeExecutor::createStrategy(StrategySetting *setting) {
    //创建策略
    Strategy *strategy = factory::get().produce(setting->strategyType);
    strategy->init(this->acct, setting);
    strategyMap[setting->strategyId] = strategy;
    //订阅合约
    set<string> contracts;
    if(!setting->refSymbol.empty())
        contracts.insert(setting->refSymbol);
    if(!setting->trgSymbol.empty())
        contracts.insert(setting->trgSymbol);
    this->subContract(contracts, strategy);
}



void TradeExecutor::onTick(Tick *tick) {
    if (!this->subsMap.count(tick->symbol) > 0)
        return;

    if (barGeneratorMap.count(tick->symbol) > 0) {
        //推送到对应的barGenerator
        auto barvec = barGeneratorMap[tick->symbol];
        for (auto &item: *barvec)
            item->onTick(tick);
    }
    //推送给账户
    this->acct->lastTickMap[tick->symbol] = tick;
    //推送给策略
    for (auto strategy: this->subsMap[tick->symbol]) {
        strategy->onTick(tick);
    }
}

void TradeExecutor::clear() {
    auto iter = this->removeList.begin();
    while (iter != this->removeList.end()) {
        if (*iter != nullptr) {
            Order *order = *iter;
            delete order;
            order = nullptr;
        }
        iter = removeList.erase(iter);
    }
}



void TradeExecutor::fastEventHandler() {
    fmtlog::setThreadName("fastHandler");
    //setpriority(PRIO_PROCESS, 0, -10);
    //该线程绑定到CPU2中
    //int threadId=(int)(std::this_thread::get_id());
    logi("faset event thread {}", getpid());
    if (acct->cpuNumEvent > 0) {
        int cpuNum = acct->cpuNumEvent;
        //绑定CPU
        int cpucorenum = sysconf(_SC_NPROCESSORS_CONF);  /*获取核数*/
        logi("faset evnet thread bind cpucore[{}],totalCore:{}", acct->cpuNumEvent, cpucorenum);
        if (cpupin(cpuNum)) {
            logi("faset event thread bind cpucor[{}] success!!!", cpuNum);
        } else {
            perror("pthread_setaffinity_np");
            loge("faset event thread bind cpucor[{}] fail!!!", cpuNum);
        }
    }

    Event event;
    int count = 0;
    double totalNsec = 0;
    long tickSeq = 0;
    while (true) {
        //轮询quote队列
        if (acct->mdGateway!= nullptr && acct->mdGateway->getQueue()->pop(event)) {
            switch (event.type) {
                case EvType::STATUS: {
                    auto rsp = buildMsg(MSG_TYPE::ON_ACCT, *acct->acctInfo, this->id);
                    this->acct->msgQueue->push(Event{EvType::MSG, 0, rsp});
                    break;
                }
                case EvType::TICK: {
                    Tick *tick = (Tick *) event.data;
                    tick->eventTsc = Context::get().tn.rdtsc();
                    this->onTick(tick);
                    auto rsp = buildMsg(MSG_TYPE::ON_TICK, *tick, this->id);
                    this->acct->msgQueue->push(Event{EvType::MSG, 0, rsp});
                    break;
                }
                default: {
                    break;
                }
            }
            if (event.data != nullptr)
                delete event.data;
        }
    }
}


void TradeExecutor::msgHandler() {
    fmtlog::setThreadName("msgHandler");
    Event event;
    while (true) {
        try {
            bool find = false;
            //处理推送消息
            if (this->acct->msgQueue->pop(event)) {
                find = true;
                switch (event.type) {
                    case EvType::STATUS: {
                        auto msg = buildMsg(MSG_TYPE::ON_ACCT, *acct->acctInfo, this->id);
                        this->udsServer->push(*msg);
                        break;
                    }
                    case EvType::READY:{
                        //账户状态就绪
                        set<string> contracts;
                        int i=0;
                        for (const auto &item : this->acct->accoPositionMap){
                            //if(i++<20)
                                contracts.insert(item.second->symbol);
                        }
                        this->acct->subscribe(contracts);
                        break;
                    }
                    case EvType::MSG: {
                        Message *msg = (Message *) event.data;
                        this->udsServer->push(*msg);
                        delete msg;
                        break;
                    }
                }
            }
            if (!find) {
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
            }
        } catch (...) {
            loge("msgQueue handle error!");
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
    }
}


BarGenerator *TradeExecutor::getBarGenerator(string symbol, BAR_LEVEL level) {
    auto barGenvec = barGeneratorMap[symbol];
    auto it = find_if(barGenvec->begin(), barGenvec->end(), [level](BarGenerator *bg) {
        return bg->level = level;
    });
    if (it != barGenvec->end()) {
        return *it;
    }
    return nullptr;
}


Message *TradeExecutor::onRequest(Message *msg) {
    Message *response = new Message;
    response->type = enum_string(MSG_TYPE::RETURN);
    response->requestId = msg->requestId;
    switch (msg->msgType) {
        case MSG_TYPE::SYNC: {
            if (this->acct == nullptr) {
                AcctConf acctConf;
                msg->jsonData.decode(acctConf);
                Context::get().acct = this->acct;
                this->cv.notify_one();
            }
            response->data = xpack::json::encode(acct->acctInfo);
            break;
        }
        case MSG_TYPE::QRY_ACCT: {
            response->data = xpack::json::encode(*acct->acctInfo);
            break;
        }
        case MSG_TYPE::QRY_POSITION: {
            vector<Position> pos;
            for (const auto &item : this->acct->accoPositionMap)
                pos.push_back(*item.second);
            response->data = xpack::json::encode(pos);
            break;
        }
        case MSG_TYPE::QRY_TRADE: {
            break;
        }
        case MSG_TYPE::QRY_ORDER: {
            break;
        }

        case MSG_TYPE::CONNECT: {
            logi("start connect cmd ...");
            bool status = msg->jsonData["status"].GetBool();
            if (status) {
                this->acct->connect();
            } else {
                this->acct->disconnect();
            }
            response->data = xpack::json::encode(*acct->acctInfo);
            logi("finish connect cmd...");
            break;
        }


        case MSG_TYPE::ACT_ORDER: {
            OrderReq req;
            msg->jsonData.decode(req);
            this->acct->insertOrder(&req);
            break;
        }
        case MSG_TYPE::ACT_CANCEL: {
            CancelReq req;
            msg->jsonData.decode(req);
            this->acct->cancelorder(req);
            break;
        }


        case MSG_TYPE::EXIT: {
            //断开交易及行情
            this->acct->disconnect();
            //this->stopFlag=true;
            //等待响应消息发送完毕再退出
            sleep(3);
            exit(0);
            break;
        }


        case MSG_TYPE::MD_SUBS: {
            CommReq commReq;
            xpack::json::decode(msg->data, commReq);
            set<string> contracts;
            contracts.insert(commReq.param);
            this->acct->subscribe(contracts);
            break;
        }
        case MSG_TYPE::PAUSE_OPEN: {
            break;
        }
        case MSG_TYPE::PAUSE_CLOSE: {
            break;
        }
    }
    return response;
}

void TradeExecutor::shutdown() {
    logw("shutdown ...");
}

int TradeExecutor::signalHanler(int signo) {
    return 0;
}





