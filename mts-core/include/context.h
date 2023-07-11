//
// Created by 段晴 on 2022/2/28.
//

#ifndef MTS_CORE_CONTEXT_H
#define MTS_CORE_CONTEXT_H
#include "singleton.h"
#include "tscns.h"
#include <thread>
#include "fmtlog/fmtlog.h"
#include "config.h"
#include "common/util.hpp"
#include "trade/acct.h"

class Context:public Singleton<Context>{
    friend class Singleton<Context>;

public:
    TSCNS tn;
    config::TradeSetting setting;
    vector<config::StrategySetting> strategySettings;
    string  acctId;
    Acct * acct;



    //config::Config config;

    //初始化上下
    void init(const string& acctId){
        //初始化计时器
        this->acctId=acctId;
        double ghz=tn.init();
        logi("init tsn,ghz:{}",ghz);
        std::this_thread::sleep_for(std::chrono::seconds(1));
        tn.calibrate();


        //加载配置文件
        logi("init context ...{}",acctId);
        //加载配置
        logi("start load setting.json");
        string settingJson=Util::readFile("conf/setting-trade.json");
        xpack::json::decode(settingJson, setting);
        //账户信息由agent推送
        /*logi("start load account.json");
        string accountJson=Util::readFile("conf/account.json");
        vector<config::AcctConf> acctConfs;
        xpack::json::decode(accountJson, acctConfs);
        bool find= false;
        for(auto & conf:acctConfs){
            if(conf.id==this->acctId){
                config.account=conf;
                find=true;
            }
            //actConf=&conf;
        }
        if(!find){
            loge("cannot find account config [{}]",this->acctId);
            exit(-1);
        }*/


        logi("start load strategy.json");
        string xmlStrategy=Util::readFile("conf/strategy-ost.xml");
        //config::StrConfig strConfig;
        //xpack::xml::decode(xmlStrategy, strConfig);


        string jsonStrategy=Util::readFile("conf/strategy.json");
        vector<config::StrategySetting> settings;
        xpack::json::decode(jsonStrategy, settings);
        for(auto & setting :settings){
            if(setting.accountId==this->acctId){
                strategySettings.push_back(setting);
            }
        }



    }
};

#endif //MTS_CORE_CONTEXT_H
