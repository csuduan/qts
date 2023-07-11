//
// Created by 段晴 on 2022/6/8.
//

#include "quotaExecutor.h"
#include "common/util.hpp"
#include "gateway/gatewayFactory.hpp"
#include "common/taskScheduler.hpp"

void QuotaExecutor::start() {
    logi("quotaExecutor init...");
    double ghz=tn.init();
    logi("init tsn,ghz:{}",ghz);
    std::this_thread::sleep_for(std::chrono::seconds(1));
    tn.calibrate();

    //加载配置
    logi("start load setting.json");
    string settingJson=Util::readFile("conf/setting-quote.json");
    xpack::json::decode(settingJson, setting);

    if(!std::filesystem::exists(setting.dataPath))
        std::filesystem::create_directories(setting.dataPath);

    //创建quotes
    for (auto & quotaInfo: setting.quotes) {
        if(quotaInfo.enable== false)
            continue;
        vector<string> tmp;
        Util::split(quotaInfo.subList,tmp,",");
        for(auto &item : tmp)
            quotaInfo.subSet.insert(item);
        tmp.clear();

        MdGateway * mdGateway=GatewayFactory::createMdGateway(&quotaInfo);
        this->mdGateways.emplace_back(mdGateway);
    }


    std::thread msgThread([this](){
        this->msgHanler();
    });
    msgThread.detach();


    TaskScheduler *scheduler = new TaskScheduler;
    auto testTask=[](){
        logi("test---");
    };
    scheduler->addTask("test","0 50 21 * * ?",testTask);
    scheduler->addTask("connect","0 9 * * * MON-FRI",[this](){
        for(auto gateway:this->mdGateways)
            gateway->connect();
    });
    scheduler->addTask("connect","0 30 15 * * MON-FRI",[this](){
        for(auto gateway:this->mdGateways)
            gateway->disconnect();
    });

    scheduler->start();


}

void QuotaExecutor::msgHanler() {
    Event event;
    map<string,QuotaFile *> fileMap;

    while (true) {
        //轮询所有的quote队列
        for(auto & mdGateway:this->mdGateways){
            if (mdGateway!= nullptr && mdGateway->getQueue()->pop(event)) {
                switch (event.type) {
                    case EvType::TICK: {
                        Tick *tick = (Tick *) event.data;
                        string id=mdGateway->id;
                        string tradingDay=tick->tradingDay;
                        if(fileMap.count(id)==0){
                            QuotaFile *quotaFile=new QuotaFile{};
                            fileMap[id] = quotaFile;
                        }
                        auto qFile=fileMap[id];
                        if(tradingDay!=qFile->date){
                            if(qFile->ofs.is_open())
                                qFile->ofs.close();
                            qFile->date=tradingDay;
                            qFile->fname=this->setting.dataPath+"/"+id+"-"+tradingDay+".dat";
                            qFile->ofs.open(qFile->fname,ios::app);
                        }
                        string dat=fmt::format("{},{},{},{},{},{},{},{}",
                                               tick->symbol,tick->tradingDay,tick->actionDay,tick->updateTime,
                                               tick->bidPrice1,tick->askPrice1,tick->bidVolume1,tick->askPrice1);
                        qFile->ofs<<dat<<endl;
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
}

void QuotaExecutor::connect() {

}

void QuotaExecutor::disconnect() {

}
