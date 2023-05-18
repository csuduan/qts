//
// Created by 段晴 on 2022/2/28.
//

#ifndef MTS_CORE_CONTEXT_H
#define MTS_CORE_CONTEXT_H
#include "Singleton.h"
#include "tscns.h"
#include <thread>
#include "fmtlog/fmtlog.h"
#include "Config.h"
#include "Util.h"

class Context:public Singleton<Context>{
    friend class Singleton<Context>;

public:
    string  acctId;
    TSCNS tn;
    config::Config config;

    //初始化上下
    void init(const string& acctId){
        this->acctId=acctId;
        logi("init context ...{}",acctId);
        //加载配置
        logi("start load setting.json");
        string settingJson=Util::readFile("conf/setting.json");
        xpack::json::decode(settingJson, config.setting);

        logi("start load account.json");
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
        }

        logi("start load strategy.json");
        string jsonStrategy=Util::readFile("conf/strategy.json");
        vector<config::StrategySetting> settings;
        xpack::json::decode(jsonStrategy, settings);
        for(auto & setting :settings){
            if(setting.accountId==this->acctId){
                config.strategySettings.push_back(setting);
            }
        }



        //初始化计时器
        double ghz=tn.init();
        logi("init tsn,ghz:{}",ghz);
        std::this_thread::sleep_for(std::chrono::seconds(1));
        tn.calibrate();
    }
};

#endif //MTS_CORE_CONTEXT_H
