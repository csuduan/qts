//
// Created by 段晴 on 2022/1/22.
//

#include "TradeExecutor.h"
#include "gateway/GatewayFactory.h"
#include "StrategyFactory.hpp"
#include "strategy/GoldStrategy.h"
#include "Config.h"
#include "xpack/json.h"
#include "Monitor.h"
#include <thread>
#include <functional>
#include <unistd.h>
#include<sys/resource.h>
#include "util/UdsServer.h"
using std::placeholders::_1;

TradeExecutor::TradeExecutor(string acctId):id(acctId){
    time_t now = time(nullptr);
    char tmp[64]={0};
    strftime(tmp, sizeof(tmp), "%H%M%S0000", localtime(&now) );
    this->orderRefNum= atol(tmp);
}



void TradeExecutor::init() {
    logi("tradeExecutor {} init...",this->id);
    string json=Util::readFile("./account.json");
    vector<config::Account> acctConfs;
    xpack::json::decode(json, acctConfs);

    config::Account* actConf= nullptr;
    for(auto & conf:acctConfs){
        if(conf.id==this->id)
            actConf=&conf;
    }
    if(actConf== nullptr){
        loge("cannot find account config [{}]",this->id);
        exit(-1);
    }

    vector<string> tmp;
    Util::split(actConf->user,tmp,"|");
    string userId=tmp[0];
    string passwd=tmp[1];
    tmp.clear();
    Util::split(actConf->tdAddress,tmp,"|");
    string tdType=tmp[0];
    string tdAddress=tmp[1];
    string brokerId;
    string appId;
    string authCode;
    if(tmp.size()>=3){
        string authInfo=tmp[2];
        tmp.clear();
        Util::split(authInfo,tmp,":");
        brokerId=tmp[0];
        appId=tmp[1];
        authCode=tmp[2];
    }
    tmp.clear();
    Util::split(actConf->mdAddress,tmp,"|");
    string mdType=tmp[0];
    string mdAddress=tmp[1];
    tmp.clear();
    //create account

    Account * account=new Account();
    account->id=actConf->id;
    account->name=actConf->name;
    account->loginInfo.userId=userId;
    account->loginInfo.password=passwd;
    account->loginInfo.tdType=tdType;
    account->loginInfo.tdAddress=tdAddress;
    account->loginInfo.brokerId=brokerId;
    account->loginInfo.appId=appId;
    account->loginInfo.authCode=authCode;
    account->loginInfo.mdType=mdType;
    account->loginInfo.mdAddress=mdAddress;
    this->account=account;
    this->mdGateway=GatewayFactory::createMdGateway(account);
    this->tdGateway=GatewayFactory::createTdGateway(account);

    //初始化订阅
    std::set<string> subList;
    for(auto & item :actConf->sublist){
        subList.insert(item);
    }
    this->mdGateway->subscribe(subList);


    try {
        logi("start load strategy.json");
        string json=Util::readFile("./strategy.json");
        vector<config::StrategySetting> settings;
        xpack::json::decode(json, settings);
        for(auto & setting :settings){
            if(setting.accountId!=this->id)
                continue;
            StrategySetting *strategySetting=new StrategySetting();
            strategySetting->className=setting.className;
            strategySetting->strategyId=setting.strategyId;
            strategySetting->accountId=setting.accountId;
            strategySetting->contracts=setting.contracts;
            strategySetting->paramMap=setting.paramMap;
            this->createStrategy(strategySetting);
        }
    }catch (exception ex){
        loge("load strategy fail,{}",ex.what());
    }


    std::thread fastEventThread(std::bind(&TradeExecutor::fastEventHandler, this));
    fastEventThread.detach();

    std::thread msgThread(std::bind( &TradeExecutor::msgHandler, this));
    msgThread.detach();

    std::thread clearThread(std::bind( &TradeExecutor::clearWork, this));
    clearThread.detach();
}

void TradeExecutor::start() {
    logi("tradeExecutor {} start...",this->id);

    //启动server
    this->server =new UdsServer(this->id);
    server->setMsgCallback(std::bind(&TradeExecutor::addMsgQueue, this,_1));
    server->start();
}

void TradeExecutor::subContract(set<string> contracts, Strategy *strategy) {
    //更新订阅列表
    for(auto &contract :contracts){
        if(!this->subsMap.contains(contract)){
            this->subsMap[contract]=std::set<Strategy*>();
//            vector<string> contracts;
//            contracts.push_back(contract);
//            this->mdGateway->subscribe(contracts);
        }
        this->subsMap[contract].insert(strategy);
    }
    //开始订阅
    this->mdGateway->subscribe(contracts);
}

void TradeExecutor::onTick(Tick *tick) {
    if(!this->subsMap.contains(tick->symbol))
        return;
    //TODO  如何销毁lastTick
    this->lastTickMap[tick->symbol]=tick;
    for (auto strategy:     this->subsMap[tick->symbol]) {
        strategy->onTick(tick);
    }

}

void TradeExecutor::createStrategy(StrategySetting *setting) {
    Strategy * strategy=factory::get().produce(setting->className);
    strategy->init(this,setting);
    strategyMap[setting->strategyId] = strategy;
    //订阅合约
    this->subContract(setting->contracts,strategy);
}

void TradeExecutor::insertOrder(Order *order) {
    //生成ordref;
    try {
        order->orderRef= to_string(this->orderRefNum++);
        auto contract=account->contractMap[order->symbol];
        if(contract== nullptr){
            loge("{},insertOrder fial,can not find contract info[{}]",this->id,order->symbol);
            return;
        }
        order->exchange=contract->exchange;
        auto lastTick =this->lastTickMap[order->symbol];
        if(order->price==0 && lastTick!= nullptr){
            order->price=order->direction == TRADE_DIRECTION::BUY?lastTick->askPrice1:lastTick->bidPrice1;
        }
        //todo 自成交检查,可用仓位检查，可用保证金检查
        this->workingOrderMap[order->orderRef] = order;
        this->tdGateway->insertOrder(order);
    }catch (exception ex){
        loge("{} insert order err,{}",this->account->id,ex.what());
    }


}

void TradeExecutor::cancelorder(string orderRef) {
    if(this->workingOrderMap.contains(orderRef)){
        this->tdGateway->cancelOrder(workingOrderMap[orderRef]);
    }
}

void TradeExecutor::onOrder(Order *order) {
    if( !this->workingOrderMap.contains(order->symbol))
        return;
    if(STATUS_FINISHED.contains(order->status))
        order->finished=true;
    auto startegy=this->strategyOrderMap[order->orderRef];
    startegy->onOrder(order);
    if(order->finished){
        this->workingOrderMap.erase(order->orderRef);
        this->removeList.push_back(order);
    }
}

void TradeExecutor::clear() {
    auto iter = this->removeList.begin();
    while (iter != this->removeList.end())
    {
        if(*iter!= nullptr){
            Order * order=*iter;
            delete order;
            order= nullptr;
        }
        iter=removeList.erase(iter);
    }
}

void TradeExecutor::connect() {
    this->tdGateway->connect();
}

void TradeExecutor::disconnect() {
    this->tdGateway->disconnect();
}

void TradeExecutor::insertOrder(msg::OrderReq *orderReq) {
    Order *order =new Order();
    order->symbol=orderReq->symbol;
    order->direction=TRADE_DIRECTION_MAP[orderReq->direct];
    order->offset=OFFSET_MAP[orderReq->offset];
    order->price=orderReq->price;
    order->totalVolume=orderReq->volume;
    this->insertOrder(order);
}


[[noreturn]] void TradeExecutor::fastEventHandler() {
    fmtlog::setThreadName("event");
    //setpriority(PRIO_PROCESS, 0, -10);
    Event event;
    int count=0;
    double totalNsec=0;
    while (true){
        if(this->account->eventQueue.pop(event)){
            switch (event.type) {
                case EventType::TICK:{
                    Monitor::tickCount++;
                    timespec now;
                    Util::getTime(&now);
                    totalNsec+=Util::delaynsec(&event.time,&now);
                    count++;
                    Monitor::avgQueueDelay = totalNsec/count;
                    Tick * tick= (Tick *)event.data;
                    this->onTick(tick);
                    //todo 生产bar
                    if(!barGeneratorMap.contains(tick->symbol))
                        barGeneratorMap[tick->symbol]=new BarGenerator();
                    barGeneratorMap[tick->symbol]->onTick(tick);
                    break;
                }
                case EventType::ORDER:{
                    Order* order=(Order *)event.data;
                    this->onOrder(order);
                    break;
                }
                case EventType::TRADE:
                    break;
            }
        }

    }
}

void TradeExecutor::clearWork() {
    fmtlog::setThreadName("clear");
    while (true){
        this->clear();
        sleep(10);
    }

}


void TradeExecutor::msgHandler() {
    Event event;
    while (true){
        if(this->msgQueue.empty() ){
            usleep(100*1000);
            continue;
        }
        msg::Message message=this->msgQueue.front();
        this->msgQueue.pop();
        try {
            msg::MSG_TYPE msgType=msg::msgTypeMap[message.type];
            switch (msgType) {
                case msg::MSG_TYPE::EXIT:{
                    //todo 退出前收尾工作
                    exit(0);
                    break;
                }
                case msg::MSG_TYPE::MD_CONNECT:{
                    this->mdGateway->disconnect();
                    break;
                }
                case msg::MSG_TYPE::MD_DISCOUN:{
                    this->mdGateway->connect();
                    break;
                }
                case msg::MSG_TYPE::MD_SUBS:{
                    msg::CommReq commReq;
                    xpack::json::decode(message.data, commReq);
                    set<string> contracts;
                    contracts.insert(commReq.param);
                    this->mdGateway->subscribe(contracts);
                    break;
                }
                case msg::MSG_TYPE::ACT_DISCONN:{
                    msg::CommReq commReq;
                    xpack::json::decode(message.data, commReq);
                    this->disconnect();
                    break;
                }
                case msg::MSG_TYPE::ACT_CONNECT:{
                    msg::CommReq commReq;
                    xpack::json::decode(message.data, commReq);
                    this->connect();
                    break;
                }
                case msg::MSG_TYPE::ACT_ORDER:{
                    msg::OrderReq orderReq;
                    xpack::json::decode(message.data, orderReq);
                    xpack::json::decode(message.data, orderReq);
                    this->insertOrder(&orderReq);
                    break;
                }
                case msg::MSG_TYPE::ACT_CANCEL:{
                    msg::CancelReq cancelReq;
                    xpack::json::decode(message.data, cancelReq);
                    this->cancelorder(cancelReq.orderRef);
                    break;
                }

            }

        }catch (exception ex){
            cout<<"msg handle error" <<ex.what()<<endl;
        }
    }
}

void TradeExecutor::addMsgQueue(msg::Message msg) {
    this->msgQueue.push(msg);
}

