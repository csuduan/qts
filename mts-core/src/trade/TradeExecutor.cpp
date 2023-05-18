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
#include "Message.h"
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
}

void TradeExecutor::start() {
    logi("tradeExecutor {} start...",this->id);
    //create account
    this->account= buildAccount(Context::get().config.account);


    //启动udsServer
    SocketAddr udsAddr;
    udsAddr.name="udsServer";
    udsAddr.type=SocketType::UDS;
    udsAddr.unName=this->id;
    this->udsServer =new SocketServer(udsAddr);
    thread t_uds([this](){
        this->udsServer->start();
    });
    t_uds.detach();



    //connect to agent
    SocketAddr addr;
    addr.name=this->id;
    addr.type=SocketType::UDS;
    addr.unName=Context::get().config.account.agent;
    this->agentClient =new SocketClient(addr);
    this->agentClient->start();

    while (!this->agentClient->connected){
        //等待连接agent成功
        sleep(1);
    }
    logi("connect to agent[{}] success !",this->account->agent);

    string dbPath=Context::get().config.setting.dataPath+"/mts-core.db";
    this->sqliteHelper=new SqliteHelper(dbPath);

    map<string,config::QuoteConf > quoteConfMap;
    for(auto & item:Context::get().config.setting.quoteGroups){
        quoteConfMap[item.name] = item;
    }


    this->tdGateway=GatewayFactory::createTdGateway(account);

    //创建行情
    for(auto &item :Context::get().config.account.quotes){
        if(!quoteConfMap.count(item)>0){
            loge("cannot find quoteConfig,{}",item);
        }else{
            Quote * quote= buildQuote(quoteConfMap[item]);
            quote->autoConnect=this->account->autoConnect;
            this->quotes.push_back(quote);
            MdGateway * mdGateway=GatewayFactory::createMdGateway(quote);
            this->mdGatewayMap[quote->name]=mdGateway;
        }
    }

    try {

        for(auto & setting :Context::get().config.strategySettings){
            if(setting.accountId!=this->id)
                continue;
            StrategySetting *strategySetting= buildStrategySetting(setting);
            this->createStrategy(strategySetting);


            //创建bar
            for(auto & symbol :setting.contracts){
                if(setting.barLevel==0)
                    continue;
                if(!barGeneratorMap.count(symbol)>0){
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



    std::thread fastEventThread(std::bind(&TradeExecutor::fastEventHandler, this));
    fastEventThread.detach();

    std::thread msgThread(std::bind( &TradeExecutor::msgHandler, this));
    msgThread.detach();


}

void TradeExecutor::subContract(set<string> contracts, Strategy *strategy) {
    //更新订阅列表
    for(auto &contract :contracts){
        if(!this->subsMap.count(contract)>0){
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
    if(!this->subsMap.count(tick->symbol)>0)
        return;

    if(barGeneratorMap.count(tick->symbol)>0){
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
        order->statusMsg="交易已断开";
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
        //账户持仓量检查
        if(order->offset != OPEN){
            auto position=account->getPosition(order->symbol,order->getPosDirection());
            if(position->pos<order->totalVolume){
                //持仓量不足
                order->status = ORDER_STATUS::ERROR;
                order->statusMsg=  "持仓不足";
                loge("Order {} check fail,positon not enough",order->orderRef);
                return false;
            }
        }
        //自成交检查
        auto vec=workingMap[order->symbol];
        if(vec.size()>0){
            auto it=find_if(vec.begin(),vec.end(),[order](Order * existOrder){
                //存在交易方向相反，且未结束的报单
                return order->direction!=existOrder->direction && !existOrder->finished;
            });
            if(it!=vec.end()){
                order->status = ORDER_STATUS::ERROR;
                order->statusMsg=  "自成交风险";
                loge("Order {} check fail, exist self trading with:{}",order->orderRef,(*it)->orderRef);
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
    if(STATUS_FINISHED.count(order->status)>0 && order->tradedVolume == order->realTradedVolume)
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
        //this->removeList.push_back(order);
    }
}

void TradeExecutor::onTrade(Trade *trade) {
    //更新账户持仓
    Position * position=this->account->getPosition(trade->symbol,trade->getPosDirection());
    if(trade->offset == OPEN)
        position->tdPos+=trade->volume;
    else{
        if(trade->offset == CLOSETD)
            position->tdPos-=trade->volume;
        else{
            //平仓和平昨，都优先平昨
            if(position->ydPos>=trade->volume)
                position->ydPos-=trade->volume;
            else{
                position->tdPos-=trade->volume-position->ydPos;
                position->ydPos=0;
            }
        }
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
                    auto rsp=buildMsg(MSG_TYPE::ON_ORDER,*order,this->id);
                    this->msgQueue.push(Event{EvType::MSG,0,&rsp});
                    break;
                }
                case EvType::TRADE:{
                    Trade * trade=(Trade *) event.data;
                    this->onTrade(trade);
                    if(account->orderMap.count(trade->orderRef)){
                        auto order=account->orderMap[trade->orderRef];
                        this->onOrder(order);
                    }

                    auto rsp=buildMsg(MSG_TYPE::ON_TRADE,*trade,this->id);
                    this->msgQueue.push(Event{EvType::MSG,0,&rsp});
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

}


void TradeExecutor::msgHandler() {
    fmtlog::setThreadName("msgHandler");
    Event event;
    while (true){
        try {
            bool find=false;
            if(this->agentClient->queue.pop(event)){
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
        case MSG_TYPE::CONNECT:{
            ConnectReq req;
            msg->data.decode(req);
            if(req.type==1 || req.type==3){
                if(req.status)
                    tdGateway->connect();
                else
                    tdGateway->disconnect();
            }

            if(req.type==2 || req.type==3){
                for(auto & [name,mdGateway] :mdGatewayMap)
                    if(req.status)
                        mdGateway->connect();
                    else
                        mdGateway->disconnect();
            }
            break;
        }


        case MSG_TYPE::ACT_ORDER:{
            OrderReq req;
            msg->data.decode(req);
            this->insertOrder(&req);
            break;
        }
        case MSG_TYPE::ACT_CANCEL:{
            CancelReq req;
            msg->data.decode(req);
            this->cancelorder(req);
            break;
        }


        case MSG_TYPE::EXIT:{
            //断开交易及行情
            for(auto & [name,mdGateway] :mdGatewayMap)
                mdGateway->disconnect();
            this->tdGateway->disconnect();
            //this->stopFlag=true;
            //等待响应消息发送完毕再退出
            sleep(3);
            exit(0);
            break;
        }


        case MSG_TYPE::MD_SUBS:{
            CommReq commReq;
            msg->data.decode(commReq);
            set<string> contracts;
            contracts.insert(commReq.param);
            for(auto & [name,mdGateway] :mdGatewayMap)
                mdGateway->subscribe(contracts);
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
            this->agentClient->request(xpack::json::encode(msg));
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




