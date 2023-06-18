//
// Created by 段晴 on 2022/1/22.
//

#include "TradeExecutor.h"
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


TradeExecutor::TradeExecutor(string acctId) : id(acctId) {
    time_t now = time(nullptr);
    auto tt = localtime(&now);
    int totolSecs = tt->tm_hour * 3600 + tt->tm_min * 60 + tt->tm_sec;
    //char tmp[64]={0};
    //strftime(tmp, sizeof(tmp), "%H%M%S000", localtime(&now) );
    this->orderRefNum = totolSecs * 1e4; //5位秒数+4位0
    logi("start orderRef:{}", this->orderRefNum);
}


void TradeExecutor::init() {
    logi("tradeExecutor {} init...", this->id);
    Context::get().init(this->id);
}

void TradeExecutor::start() {


    logi("tradeExecutor {} start udsServer...", this->id);
    //启动udsServer
    SocketAddr udsAddr;
    udsAddr.name = "udsServer";
    udsAddr.type = SocketType::UDS;
    udsAddr.unName = this->id;
    this->udsServer = new SocketServer(udsAddr, this);
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
    this->acct=buildAccount(acctConf);
    Context::get().acct=this->acct;

    //创建Gateway
    this->tdGateway = GatewayFactory::createTdGateway(acct);
    this->mdGateway = GatewayFactory::createMdGateway(acct);

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


    this->configed = true;

    std::thread fastEventThread(std::bind(&TradeExecutor::fastEventHandler, this));
    fastEventThread.detach();

    std::thread msgThread(std::bind(&TradeExecutor::msgHandler, this));
    msgThread.detach();


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
    Strategy *strategy = factory::get().produce(setting->className);
    strategy->init(this, setting);
    strategyMap[setting->strategyId] = strategy;
    //订阅合约
    this->subContract(setting->contracts, strategy);
}

bool TradeExecutor::insertOrder(Order *order) {
    if (!this->tdGateway->isConnected()) {
        order->status = ORDER_STATUS::ERROR;
        order->statusMsg = "交易已断开";
        loge("td not connected !!!");
        return false;
    }
    //生成ordref;
    try {
        order->orderRef = this->orderRefNum++;
        auto contract = acct->contractMap[order->symbol];
        if (contract == nullptr) {
            loge("{},insertOrder fial,can not find contract info[{}]", this->id, order->symbol);
            return false;
        }
        order->exchange = contract->exchange;
        auto lastTick = this->lastTickMap[order->symbol];
        if (order->price == 0 && lastTick != nullptr) {
            order->price = order->direction == TRADE_DIRECTION::BUY ? lastTick->askPrice1 : lastTick->bidPrice1;
        }
        if (order->price == 0 && lastTick != nullptr) {
            order->price = lastTick->lastPrice;
        }
        order->offset_s = enum_string(order->offset);
        order->direction_s = enum_string(order->direction);
        //账户持仓量检查
        if (order->offset != OPEN) {
            auto position = acct->getPosition(order->symbol, order->getPosDirection());
            if (position->pos < order->totalVolume) {
                //持仓量不足
                order->status = ORDER_STATUS::ERROR;
                order->statusMsg = "持仓不足";
                loge("Order {} check fail,positon not enough", order->orderRef);
                return false;
            }
        }
        //自成交检查
        auto vec = workingMap[order->symbol];
        if (vec.size() > 0) {
            auto it = find_if(vec.begin(), vec.end(), [order](Order *existOrder) {
                //存在交易方向相反，且未结束的报单
                return order->direction != existOrder->direction && !existOrder->finished;
            });
            if (it != vec.end()) {
                order->status = ORDER_STATUS::ERROR;
                order->statusMsg = "自成交风险";
                loge("Order {} check fail, exist self trading with:{}", order->orderRef, (*it)->orderRef);
                return false;
            }
        }
        bool ret = this->tdGateway->insertOrder(order);
        if (ret) {
            vec.push_back(order);
        }
    } catch (exception ex) {
        loge("{} insert order err,{}", this->acct->id, ex.what());
    }

}

void TradeExecutor::cancelorder(CancelReq &req) {
    if (!this->tdGateway->isConnected()) {
        loge("td not connected !!!");
        return;
    }
    Action action = {0};
    action.orderRef = req.orderRef;
    action.sessionId = req.sessionId;
    action.frontId = req.frontId;
    this->tdGateway->cancelOrder(&action);
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
    //TODO  如何销毁lastTick
    this->lastTickMap[tick->symbol] = tick;
    for (auto strategy: this->subsMap[tick->symbol]) {
        strategy->onTick(tick);
    }

    //TODO 推动给agent
    //Message* msg=buildMsg(MSG_TYPE::ON_TICK,tick,this->id);
    //this->msgQueue.push(Event{EvType::MSG,0,msg});

}

void TradeExecutor::onOrder(Order *order) {
    if (STATUS_FINISHED.count(order->status) > 0 && order->tradedVolume == order->realTradedVolume)
        order->finished = true;
    auto startegy = this->strategyOrderMap[order->orderRef];
    if (startegy != NULL)
        startegy->onOrder(order);
    if (order->finished) {
        auto vec = workingMap[order->symbol];
        std::remove_if(vec.begin(), vec.end(), [order](Order *existOrder) {
            return order->orderRef == existOrder->orderRef;
        });
        //this->workingOrderMap.erase(order->orderRef);
        //todo delete order
        //this->removeList.push_back(order);
    }
}

void TradeExecutor::onTrade(Trade *trade) {
    //更新账户持仓
    Position *position = this->acct->getPosition(trade->symbol, trade->getPosDirection());
    if (trade->offset == OPEN)
        position->tdPos += trade->volume;
    else {
        if (trade->offset == CLOSETD)
            position->tdPos -= trade->volume;
        else {
            //平仓和平昨，都优先平昨
            if (position->ydPos >= trade->volume)
                position->ydPos -= trade->volume;
            else {
                position->tdPos -= trade->volume - position->ydPos;
                position->ydPos = 0;
            }
        }
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

void TradeExecutor::connect() {
    this->tdGateway->connect();
}

void TradeExecutor::disconnect() {
    this->tdGateway->disconnect();
}

void TradeExecutor::insertOrder(OrderReq *orderReq) {
    Order *order = new Order();
    order->symbol = orderReq->symbol;
    order->direction = TRADE_DIRECTION_MAP[orderReq->direct];
    order->offset = OFFSET_MAP[orderReq->offset];
    order->price = orderReq->price;
    order->totalVolume = orderReq->volume;
    this->insertOrder(order);
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
        //优先处理账户事件队列，再处理行情组的事件队列
        if (this->acct->tdQueue->pop(event)) {
            switch (event.type) {
                case EvType::STATUS: {
                    auto rsp = buildMsg(MSG_TYPE::ON_ACCT, *acct->acctInfo, this->id);
                    this->msgQueue.push(Event{EvType::MSG, 0, rsp});
                    break;
                }
                case EvType::ORDER: {
                    Order *order = (Order *) event.data;
                    long t1 = Context::get().tn.rdns();
                    logi("OnRtnOrder\t{} {} {} {}  traded:{}/{} status:{} msg:{}", order->orderRef, order->symbol,
                         order->offset_s, order->direction_s, order->tradedVolume,
                         order->totalVolume, order->status_s, order->statusMsg);
                    long t2 = Context::get().tn.rdns();
                    long cost = t2 - t1;
                    logi("log cost:{}", cost);
                    this->onOrder(order);
                    //转发到系统消息队列
                    auto rsp = buildMsg(MSG_TYPE::ON_ORDER, order, this->id);
                    this->msgQueue.push(Event{EvType::MSG, 0, &rsp});
                    break;
                }
                case EvType::TRADE: {
                    Trade *trade = (Trade *) event.data;
                    this->onTrade(trade);
                    if (acct->orderMap.count(trade->orderRef)) {
                        auto order = acct->orderMap[trade->orderRef];
                        this->onOrder(order);
                    }

                    auto rsp = buildMsg(MSG_TYPE::ON_TRADE, trade, this->id);
                    this->msgQueue.push(Event{EvType::MSG, 0, &rsp});
                    break;
                }
                case EvType::POSITON: {
                    break;
                }
                default: {
                    break;
                }
            }
            if (event.data != nullptr)
                delete event.data;
        }
        //轮询quote队列
        if (acct->mdQueue->pop(event)) {
            switch (event.type) {
                case EvType::STATUS: {
                    auto rsp = buildMsg(MSG_TYPE::ON_ACCT, *acct->acctInfo, this->id);
                    this->msgQueue.push(Event{EvType::MSG, 0, rsp});
                    break;
                }
                case EvType::TICK: {
                    Tick *tick = (Tick *) event.data;
                    tick->eventTsc = Context::get().tn.rdtsc();
                    this->onTick(tick);
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
            /*if(this->udsServer->queue.pop(event)){
                find=true;
                Message *msg=(Message*)event.data;
                this->processMessage(msg);
                delete msg;
            }*/
            //处理推送消息
            if (this->msgQueue.pop(event)) {
                find = true;
                switch (event.type) {
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
    response->success = true;

    switch (msg->msgType) {
        case MSG_TYPE::SYNC: {
            if (this->acct == nullptr) {
                AcctConf acctConf;
                msg->jsonData.decode(acctConf);
                //this->acct = buildAccount(acctConf);
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

        case MSG_TYPE::CONNECT: {
            logi("start connect cmd ...");
            bool status = msg->jsonData["status"].GetBool();
            if (status) {
                tdGateway->connect();
                mdGateway->connect();

            } else {
                tdGateway->disconnect();
                mdGateway->disconnect();
            }
            auto rsp = buildMsg(MSG_TYPE::ON_ACCT, *acct->acctInfo, this->id);
            this->msgQueue.push(Event{EvType::MSG, 0, rsp});
            logi("finish connect cmd...");
            break;
        }


        case MSG_TYPE::ACT_ORDER: {
            OrderReq req;
            msg->jsonData.decode(req);
            this->insertOrder(&req);
            break;
        }
        case MSG_TYPE::ACT_CANCEL: {
            CancelReq req;
            msg->jsonData.decode(req);
            this->cancelorder(req);
            break;
        }


        case MSG_TYPE::EXIT: {
            //断开交易及行情
            this->mdGateway->disconnect();
            this->tdGateway->disconnect();
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
            mdGateway->subscribe(contracts);

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




