//
// Created by 段晴 on 2022/1/22.
//

#include "TradeEngine.h"
#include "UdsServer.h"
#include "Configure.h"
#include "Util.h"
#include "GatewayFactory.h"
#include <thread>
#include <functional>
#include "Monitor.h"


TradeEngine::TradeEngine(std::string engineId):engineId(engineId) {
    this->queue=new LockFreeQueue<Event>(1<<10);
}

void TradeEngine::createMd(MdInfo mdInfo) {
    this->mdGateway=GatewayFactory::createMdGateway(mdInfo,this->queue);
}

void TradeEngine::init() {
    Logger::getLogger().info("tradeEngine %s init...",this->engineId.c_str());

    try{
        map<string,MdInfo> mdMap;
        auto root=Util::loadJson("./config.json");
        for(int i=0;i<root["mds"].size();i++){
            Json::Value mdNode=root["mds"][i];
            string mdId=mdNode["mdId"].asString();
            vector<string> tmp;
            Util::split(mdNode["address"].asString(),tmp,"|");
            string type=tmp[0];
            string mdAddress=tmp[1];
            MdInfo mdInfo{mdId,type,mdAddress};
            mdMap[mdId]=mdInfo;
        }
        vector<string> sublist;
        for(int i=0;i<root["sublist"].size();i++){
            string subContract=root["sublist"][i].asString();
            sublist.push_back(subContract);
        }

        Json::Value tradeEngines=root["tradeEngines"];
        for(int i=0;i<tradeEngines.size();i++){
            Json::Value tradeEngine=tradeEngines[i];
            if(engineId!=tradeEngine["engineId"].asString())
                continue;
            string mdId=tradeEngine["mdId"].asString();
            MdInfo mdInfo=mdMap[mdId];
            this->createMd(mdInfo);
            this->mdGateway->subscribe(sublist);

            Json::Value accounts=tradeEngine["accounts"];
            for(int j=0;j<accounts.size();j++){
                auto accountNode=accounts[j];
                string id=accountNode["id"].asString();
                string name=accountNode["name"].asString();
                vector<string> tmp;
                Util::split(accountNode["user"].asString(),tmp,"|");
                string userId=tmp[0];
                string passwd=tmp[1];
                tmp.clear();
                Util::split(accountNode["tdAddress"].asString(),tmp,"|");
                string tdType=tmp[0];
                string tdAddress=tmp[1];
                string authInfo=tmp[2];
                tmp.clear();
                Util::split(authInfo,tmp,":");
                string brokerId=tmp[0];
                string appId=tmp[1];
                string authCode=tmp[2];

                Account account;
                account.id=id;
                account.name=name;
                account.loginInfo.accoutId=id;
                account.loginInfo.userId=userId;
                account.loginInfo.password=passwd;
                account.loginInfo.tdType=tdType;
                account.loginInfo.address=tdAddress;
                account.loginInfo.brokerId=brokerId;
                account.loginInfo.appId=appId;
                account.loginInfo.authCode=authCode;
                this->createTradeExecutor(account);
            }
        }
    }catch (exception ex){
        Logger::getLogger().error("parse config error!!!");
        exit(1);
    }

    std::thread t(std::bind( &TradeEngine::eventHanle, this));
    t.detach();
}

void TradeEngine::close() {

}

void TradeEngine::createTradeExecutor(Account account) {
    Logger::getLogger().info("create tradeExecutor %s",account.id.c_str());
    TradeExecutor*  tradeExecutor=new TradeExecutor(account,this->queue);
    this->tradeExecutorMap[account.id]= tradeExecutor;
}

void TradeEngine::createStrategy(StrategySetting strategySetting) {

}

void TradeEngine::connectAccount(std::string accountId) {

}

void TradeEngine::disconnectAccount(std::string accoundId) {

}

void TradeEngine::connectMd() {

}

void TradeEngine::disconnectMd() {

}

void TradeEngine::start() {
    this->init();
    Monitor::init();

    //启动server
    UdsServer* server =new UdsServer(this->engineId);
    server->start();
}

void TradeEngine::eventHanle() {
    Event event;
    int count=0;
    double totalNsec=0;
    while (true){
        if(this->queue->pop(event)){
            if(event.type == EventType::TICK ){
                Monitor::tickCount++;
                //Tick * tick= (Tick *)event.data;
                timespec now;
                Util::getTime(&now);
                totalNsec+=Util::delaynsec(&event.time,&now);
                count++;
                Monitor::avgQueueDelay = totalNsec/count;
            }
        }

    }
}
