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
#include "common/SocketServer.h"
#include "Shm.hpp"
#include "ShmQuote.h"
#include "Context.h"
#include "cpuin.h"
#include "DataBuilder.h"
using std::placeholders::_1;

#include <sched.h>
#include<sys/types.h>
#include<sys/sysinfo.h>
#include "magic_enum.hpp"



TradeExecutor::TradeExecutor(string acctId):id(acctId){
    time_t now = time(nullptr);
    auto tt=localtime(&now);
    int totolSecs=tt->tm_hour*3600+tt->tm_min*60+tt->tm_sec;
    //char tmp[64]={0};
    //strftime(tmp, sizeof(tmp), "%H%M%S000", localtime(&now) );
    this->orderRefNum= totolSecs*1e4; //5位秒数+4位0
    logi("start orderRef:{}",this->orderRefNum);
}



void TradeExecutor::init() {
    logi("tradeExecutor {} init...",this->id);
    string json=Util::readFile("conf/setting.json");
    config::Setting setting;
    xpack::json::decode(json, setting);

    string dbPath=setting.dataPath+"/mts-core.db";
    this->sqliteHelper=new SqliteHelper(dbPath);

    map<string,config::Quote > quoteConfMap;
    for(auto & item:setting.quoteGroups){
        quoteConfMap[item.name] = item;
    }


    json=Util::readFile("conf/account.json");
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
    this->account= buildAccount(*actConf);
    this->tdGateway=GatewayFactory::createTdGateway(account);

    //创建行情
    for(auto &item :actConf->quotes){
        if(!quoteConfMap.contains(item)){
            loge("cannot find quoteConfig,{}",item);
        }else{
            Quote * quote= buildQuote(quoteConfMap[item]);
            this->quotes.push_back(quote);
            MdGateway * mdGateway=GatewayFactory::createMdGateway(quote);
            this->mdGatewayMap[quote->name]=mdGateway;
        }
    }

    try {
        logi("start load strategy.json");
        string json=Util::readFile("conf/strategy.json");
        vector<config::StrategySetting> settings;
        xpack::json::decode(json, settings);
        for(auto & setting :settings){
            if(setting.accountId!=this->id)
                continue;
            StrategySetting *strategySetting= buildStrategySetting(setting);
            this->createStrategy(strategySetting);


            //创建bar
            for(auto & symbol :setting.contracts){
                if(setting.barLevel==0)
                    continue;
                if(!barGeneratorMap.contains(symbol)){
                    barGeneratorMap[symbol]=new vector<BarGenerator *>();
                }
                auto barGenvec=barGeneratorMap[symbol];
                BAR_LEVEL level=(BAR_LEVEL)setting.barLevel;
                auto it=find_if(barGenvec->begin(),barGenvec->end(),[level](BarGenerator * bg){
                    return bg->level=level;
                });
                if(it==barGenvec->end()){
                    //不存在
                    barGenvec->push_back(new BarGenerator(symbol,(BAR_LEVEL)setting.barLevel));
                }

            }

        }
    }catch (exception ex){
        loge("load strategy fail,{}",ex.what());
    }

    SocketAddr addr;
    addr.name=this->id;
    addr.type=SocketType::UDS;
    addr.unName="master";
    this->client =new SocketClient(addr);

    std::thread fastEventThread(std::bind(&TradeExecutor::fastEventHandler, this));
    fastEventThread.detach();

    std::thread msgThread(std::bind( &TradeExecutor::msgHandler, this));
    msgThread.detach();

}

void TradeExecutor::start() {
    logi("tradeExecutor {} start...",this->id);

    //启动server
    //server->setMsgCallback(std::bind(&TradeExecutor::addReqMsg, this, _1));
    this->client->start();
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
    //this->mdGateway->subscribe(contracts);
}

void TradeExecutor::onTick(Tick *tick) {
    if(!this->subsMap.contains(tick->symbol))
        return;

    if(barGeneratorMap.contains(tick->symbol)){
        //推送到对应的barGenerator
        auto barvec=barGeneratorMap[tick->symbol];
        for (auto &item : *barvec)
            item->onTick(tick);
    }
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

bool TradeExecutor::insertOrder(Order *order) {
    if(!this->tdGateway->isConnected()){
        order->status=ORDER_STATUS::ERROR;
        order->statusMsg="td not connected";
        loge("td not connected !!!");
        return false;
    }
    //生成ordref;
    try {
        order->orderRef= this->orderRefNum++;
        auto contract=account->contractMap[order->symbol];
        if(contract== nullptr){
            loge("{},insertOrder fial,can not find contract info[{}]",this->id,order->symbol);
            return false;
        }
        order->exchange=contract->exchange;
        auto lastTick =this->lastTickMap[order->symbol];
        if(order->price==0 && lastTick!= nullptr){
            order->price=order->direction == TRADE_DIRECTION::BUY?lastTick->askPrice1:lastTick->bidPrice1;
        }
        if(order->price==0 && lastTick!= nullptr){
            order->price=lastTick->lastPrice;
        }
        order->offset_s= enum_string(order->offset);
        order->direction_s = enum_string(order->direction);
        //todo 账户持仓检查
        //......
        //自成交检查
        auto vec=workingMap[order->symbol];
        if(vec.size()>0){
            auto it=find_if(vec.begin(),vec.end(),[order](Order * existOrder){
                //存在交易方向相反，且未结束的报单
                return order->direction!=existOrder->direction && !existOrder->finished;
            });
            if(it!=vec.end()){
                order->status = ORDER_STATUS::ERROR;
                order->statusMsg=  "exist self trading";
                loge("Order {} and {} exist self trading",order->orderRef,(*it)->orderRef);
                return false;
            }
        }
        bool  ret=this->tdGateway->insertOrder(order);
        if(ret){
            vec.push_back(order);
        }
    }catch (exception ex){
        loge("{} insert order err,{}",this->account->id,ex.what());
    }


}

void TradeExecutor::cancelorder(CancelReq & req) {
    if(!this->tdGateway->isConnected()){
        loge("td not connected !!!");
        return;
    }
    Action action={0};
    action.orderRef=req.orderRef;
    action.sessionId=req.sessionId;
    action.frontId=req.frontId;
    this->tdGateway->cancelOrder(&action);
}



void TradeExecutor::onOrder(Order *order) {
    if(STATUS_FINISHED.contains(order->status))
        order->finished=true;
    auto startegy=this->strategyOrderMap[order->orderRef];
    if(startegy!=NULL)
        startegy->onOrder(order);
    if(order->finished){
        auto vec=workingMap[order->symbol];
        std::remove_if(vec.begin(), vec.end(), [order](Order * existOrder){
           return order->orderRef == existOrder->orderRef;
        });
        //this->workingOrderMap.erase(order->orderRef);
        //todo delete order
        this->removeList.push_back(order);
    }
}

void TradeExecutor::onTrade(Trade *trade) {
    //更新账户持仓
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

void TradeExecutor::insertOrder(OrderReq *orderReq) {
    Order *order =new Order();
    order->symbol=orderReq->symbol;
    order->direction=TRADE_DIRECTION_MAP[orderReq->direct];
    order->offset=OFFSET_MAP[orderReq->offset];
    order->price=orderReq->price;
    order->totalVolume=orderReq->volume;
    this->insertOrder(order);
}


void TradeExecutor::fastEventHandler() {
    fmtlog::setThreadName("fastHandler");
    //setpriority(PRIO_PROCESS, 0, -10);
    //该线程绑定到CPU2中
    //int threadId=(int)(std::this_thread::get_id());
    logi("faset event thread {}", getpid());
    if(account->cpuNumEvent>0){
        int cpuNum=account->cpuNumEvent;
        //绑定CPU
        int cpucorenum = sysconf(_SC_NPROCESSORS_CONF);  /*获取核数*/
        logi("faset evnet thread bind cpucore[{}],totalCore:{}",account->cpuNumEvent,cpucorenum);
        if(cpupin(cpuNum)){
            logi("faset event thread bind cpucor[{}] success!!!",cpuNum);
        }else{
            perror("pthread_setaffinity_np");
            loge("faset event thread bind cpucor[{}] fail!!!",cpuNum);
        }
    }

    Event event;
    int count=0;
    double totalNsec=0;
    long tickSeq=0;
    while (true){
        //优先处理账户事件队列，再处理行情组的事件队列
        if(this->account->queue->pop(event)){
            switch (event.type) {
                case EvType::ORDER:{
                    Order * order=(Order *)event.data;
                    long t1= Context::get().tn.rdns();
                    logi("OnRtnOrder\t{} {} {} {}  traded:{}/{} status:{} msg:{}", order->orderRef, order->symbol, order->offset_s,order->direction_s,order->tradedVolume,
                         order->totalVolume, order->status_s, order->statusMsg);
                    long t2= Context::get().tn.rdns();
                    long cost=t2-t1;
                    logi("log cost:{}",cost);
                    this->onOrder(order);
                    //转发到系统消息队列
                    this->reward(buildMsg(MSG_TYPE::ON_ORDER,*order));
                    break;
                }
                case EvType::TRADE:{
                    Trade * trade=(Trade *) event.data;
                    this->reward(buildMsg(MSG_TYPE::ON_ORDER,*trade));
                    break;
                }
                case EvType::POSITON:{
                    break;
                }
                default:{
                    break;
                }
            }
        }
        for(auto & quote :this->quotes){
            //轮询quote队列
            if(quote->queue->pop(event)){
                switch (event.type) {
                    case EvType::TICK:{
                        Tick * tick= (Tick *)event.data;
                        tick->eventTsc= Context::get().tn.rdtsc();
                        this->onTick(tick);
                        break;
                    }
                    default:{
                        break;
                    }
                }
            }

        }
    }
}


void TradeExecutor::reward(Message *msg) {
    //为了不影响fasetQueue性能，不执行调用push命令
    msg->actId=this->id;
    this->msgQueue.push(Event{EvType::MSG,0,msg});
}


void TradeExecutor::msgHandler() {
    fmtlog::setThreadName("msgHandler");
    Event event;
    while (true){
        try {
            bool find=false;
            if(this->client->queue.pop(event)){
                find=true;
                Message *msg=(Message*)event.data;
                this->processMessage(msg);
                delete msg;
            }
            if(this->msgQueue.pop(event)){
                find=true;
                Message *msg=(Message*)event.data;
                this->processMessage(msg);
                delete msg;
            }
            if(!find){
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
            }
        }catch (...){
            loge("msgQueue handle error!");
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
    }
}

void TradeExecutor::processMessage(Message * msg) {
    switch (msg->msgType) {
        //以下为需要处理的消息
        case MSG_TYPE::ACT_ORDER:{
            OrderReq req;
            xpack::json::decode(msg->data, req);
            this->insertOrder(&req);
            break;
        }
        case MSG_TYPE::ACT_CANCEL:{
            CancelReq req;
            xpack::json::decode(msg->data, req);
            this->cancelorder(req);
            break;
        }


        case MSG_TYPE::STOP:{
            //断开交易及行情
            for(auto & [name,mdGateway] :mdGatewayMap)
                mdGateway->disconnect();
            this->tdGateway->disconnect();
            this->stopFlag=true;
            //等待响应消息发送完毕再退出
            //exit(0);
            break;
        }
        case MSG_TYPE::MD_CONNECT:{
            for(auto & [name,mdGateway] :mdGatewayMap)
                mdGateway->disconnect();
            break;
        }
        case MSG_TYPE::MD_DISCOUN:{
            for(auto & [name,mdGateway] :mdGatewayMap)
                mdGateway->connect();
            break;
        }
        case MSG_TYPE::MD_SUBS:{
            CommReq commReq;
            xpack::json::decode(msg->data, commReq);
            set<string> contracts;
            contracts.insert(commReq.param);
            for(auto & [name,mdGateway] :mdGatewayMap)
                mdGateway->subscribe(contracts);
            break;
        }
        case MSG_TYPE::ACT_DISCONN:{
            CommReq commReq;
            xpack::json::decode(msg->data, commReq);
            this->disconnect();
            break;
        }
        case MSG_TYPE::ACT_CONNECT:{
            CommReq commReq;
            xpack::json::decode(msg->data, commReq);
            this->connect();
            break;
        }
        case MSG_TYPE::ACT_PAUSE_OPEN:{
            break;
        }
        case MSG_TYPE::ACT_PAUSE_CLOSE:{
            break;
        }


            //以下为需要推送的消息
        case MSG_TYPE::ON_ACCOUNT:
        case MSG_TYPE::ON_CONTRACT:
        case MSG_TYPE::ON_POSITION:
        case MSG_TYPE::ON_TRADE:
        case MSG_TYPE::ON_ORDER:
        case MSG_TYPE::ON_BAR:
        case MSG_TYPE::ON_LOG:{
            this->client->request(xpack::json::encode(msg));
            break;
        }

            delete msg;
    }
}

BarGenerator* TradeExecutor::getBarGenerator(string symbol, BAR_LEVEL level) {
    auto barGenvec=barGeneratorMap[symbol];
    auto it=find_if(barGenvec->begin(),barGenvec->end(),[level](BarGenerator * bg){
        return bg->level=level;
    });
    if(it!=barGenvec->end()){
        return *it;
    }
    return nullptr;
}

void TradeExecutor::attachQuote(string name,int size) {
    //shm=new Shm<MemTick>(name, size);
    //shm->init();
}




