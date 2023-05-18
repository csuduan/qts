//
// Created by 段晴 on 2022/3/2.
//

#include "MasterExecutor.h"
#include "Config.h"
#include "gateway/GatewayFactory.h"
#include <thread>
#include <filesystem>
#include "DataBuilder.h"

bool MasterExecutor::init() {
    string json=Util::readFile("conf/setting.json");
    config::Setting setting;
    xpack::json::decode(json, setting);

    //读取db
    string dbPath=setting.dataPath+"/mts-core.db";
    this->sqliteHelper=new SqliteHelper(dbPath);


    //读取合约信息
    vector<Contract*> contracts;
    sqliteHelper->queryContracts(contracts);
    for(auto & item :contracts)
        contractMap[item->symbol] = item;


    for(auto & item:setting.quoteGroups){
        if(!item.enable)
            continue;
        Quote *quote= buildQuote(item);
        this->quotes.push_back(quote);
        quote->dumpPath=setting.dataPath+"/quote";
        if(!std::filesystem::exists(quote->dumpPath)){
            std::filesystem::create_directories(quote->dumpPath);
        }
        //quote->contractMap=&this->contractMap;
        //create gateway
        MdGateway* mdGateway=GatewayFactory::createMdGateway(quote);
        mdGatewayMap[item.name]=mdGateway;
    }


    //tcpServer
    SocketAddr tcpAddr;
    tcpAddr.name="tcpServer";
    tcpAddr.type=SocketType::TCP;
    tcpAddr.port=setting.tcpPort;
    this->tcpServer =new SocketServer(tcpAddr);

    SocketAddr udsAddr;
    udsAddr.name="udsServer";
    udsAddr.type=SocketType::UDS;
    udsAddr.unName="master";
    this->udsServer =new SocketServer(udsAddr);

    return true;
}

bool MasterExecutor::start() {
    //启动dump线程
    thread t([this](){
        this->dump();
    });
    t.detach();


    //启动服务端消息处理线程
    thread t_s([this](){
        this->msgHandler();
    });
    t_s.detach();

    //启动tcpServer
    thread t_tcp([this](){
        this->tcpServer->start();
    });
    t_tcp.detach();

    //启动udsServer
    thread t_uds([this](){
        this->udsServer->start();
    });
    t_uds.detach();

    while (true){
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    }
}

struct FileInfo{
    string  tradingDay;
    ofstream  ofile;
};

void MasterExecutor::msgHandler() {
    Event event;
    while (true){
        try{
            bool find= false;
            //处理tcpServer消息
            if(this->tcpServer->queue.pop(event)){
                find=true;
                Message *msg=(Message *)event.data;
                this->processMsg(msg);
                delete msg;
            }
            if(this->udsServer->queue.pop(event)){
                find= true;
                Message *msg=(Message *)event.data;
                this->processMsg(msg);
                delete msg;
            }
            if(!find){
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
            }
        }catch (...){
            loge("msg handler error!");
        }
    }
}

void MasterExecutor::processMsg(Message *msg) {
    switch (msg->msgType) {
        case MSG_TYPE::PING:{
            CommReq req={0};
            string rsp= buildMsg(MSG_TYPE::PONG, req);
            this->tcpServer->push(rsp);
            break;
        }
        case MSG_TYPE::ON_ORDER:
        case MSG_TYPE::ON_LOG:
        case MSG_TYPE::ON_BAR:
        case MSG_TYPE::ON_TRADE:
        case MSG_TYPE::ON_POSITION:
        case MSG_TYPE::ON_ACCOUNT:
        case MSG_TYPE::ON_CONTRACT:
            //转发给tcpServer
            this->tcpServer->push(*msg);
            break;

        case MSG_TYPE::ACT_CONNECT:
        case MSG_TYPE::ACT_DISCONN:
        case MSG_TYPE::ACT_PAUSE_OPEN:
        case MSG_TYPE::ACT_PAUSE_CLOSE:
        case MSG_TYPE::ACT_ORDER:
        case MSG_TYPE::ACT_CANCEL:{
            //转发给udsServer
            this->udsServer->push(*msg);
            break;
        }
        default:{
            //delete event.data;
            break;
        }
    }
}

void MasterExecutor::dump(){
    int lastSeq=0;
    logi("start dump quote...");
    Event event;
    while (true){
        bool  empty= true;
        map<string,FileInfo> fileMap;
        //循环遍历每一个quote
        for(auto quote :quotes){
            auto  queue=quote->queue;
            if(queue->pop(event)){
                empty= false;
                switch (event.type) {
                    case EvType::TICK:{
                        Tick * tick=(Tick *)event.data;
                        auto & fileInfo=fileMap[quote->name];

                        if(fileMap[quote->name].tradingDay!=tick->tradingDay){
                            //交易日切换
                            fileInfo.tradingDay=tick->tradingDay;
                            fileInfo.ofile.close();
                            fileInfo.ofile.open(quote->dumpPath+"/"+quote->name+"-"+fileInfo.tradingDay+".csv",ios::app);
                        }

                        fileMap[quote->name].ofile
                                <<tick->tradingDay<<","
                                <<tick->updateTime<<","
                                <<tick->symbol<<","
                                <<tick->exchange<<","
                                <<tick->lastPrice<<","
                                <<tick->preSettlePrice<<","
                                <<tick->openPrice<<","
                                <<tick->volume<<","
                                //<<fixed<<setprecision(4)
                                <<tick->bidPrice1<<","
                                <<tick->bidPrice2<<","
                                <<tick->bidPrice3<<","
                                <<tick->bidPrice4<<","
                                <<tick->bidPrice5<<","
                                <<tick->askPrice1<<","
                                <<tick->askPrice2<<","
                                <<tick->askPrice3<<","
                                <<tick->askPrice4<<","
                                <<tick->askPrice5<<","
                                <<tick->bidVolume1<<","
                                <<tick->bidVolume2<<","
                                <<tick->bidVolume3<<","
                                <<tick->bidVolume4<<","
                                <<tick->bidVolume5<<","
                                <<tick->askVolume1<<","
                                <<tick->askVolume2<<","
                                <<tick->askVolume3<<","
                                <<tick->askVolume4<<","
                                <<tick->askVolume5<<","
                                <<endl;

                        delete tick;
                        break;
                    }
                    case EvType::TRADE:{
                        break;
                    }
                    case EvType::ORDER:{
                    }
                }
            }
        }
        if(empty){
            for(auto &fileInfo:fileMap){
                fileInfo.second.ofile.flush();
            }
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
    }
}






