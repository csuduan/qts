//
// Created by 段晴 on 2022/2/28.
//

#ifndef MTS_CORE_CONTEXT_H
#define MTS_CORE_CONTEXT_H

#include "singleton.h"
#include "tscns.h"
#include <thread>
#include <filesystem>
#include "fmtlog/fmtlog.h"
#include "config.h"
#include "common/util.hpp"
#include "trade/acct.h"

#include "signal.h"

class SegmentationFault : public exception {
    const char *what() const throw() {
        return "SegmentationFault";
    }

};


class Context : public Singleton<Context> {
    friend class Singleton<Context>;

public:
    TSCNS tn;
    config::Setting setting;
    string id;
    //vector<config::StrategySetting> strategySettings;

    //config::Config config;


    void static sigHandler(int signo) {
        logw("recv sig {}", signo);
        //logw("system will exit after 2s");
        std::this_thread::sleep_for(std::chrono::seconds(2));
        fmtlog::poll();
        exit(0);
    }


    void static sigSegmentationFaultHandler(int signo) {
        logw("recv  sig {}", signo);
        throw SegmentationFault();
    }


    //初始化上下文
    void init(const string id, const string &settingPath) {
        this->id = id;
        //加载配置文件
        string settingJson = Util::readFile(settingPath.c_str());
        xpack::json::decode(settingJson, setting);

        //创建目录
        if (!std::filesystem::exists(setting.dataPath))
            std::filesystem::create_directories(setting.dataPath);

        //初始化日志
        if (setting.log2File) {
            if (!std::filesystem::exists("logs"))
                std::filesystem::create_directories("logs");
            string date = Util::getDate();
            string file = "logs/" + id + "_" + date + ".log";
            fmtlog::setLogFile(file.c_str(), false);
        } else {
            fmtlog::setLogFile(stdout, false);
        }
        fmtlog::setThreadName("main");
        fmtlog::startPollingThread(1e9);


        //初始化计时器
        double ghz = tn.init();
        logi("init tsn,ghz:{}", ghz);
        std::this_thread::sleep_for(std::chrono::seconds(1));
        tn.calibrate();

        //信号处理
        signal(SIGABRT, sigHandler);
        signal(SIGTERM, sigHandler);
        signal(SIGINT, sigHandler);
        signal(SIGSTOP, sigHandler);
        signal(SIGQUIT, sigHandler);
        signal(SIGSEGV, sigSegmentationFaultHandler);


        logi("init context  finish...{}", this->id);


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


//        logi("start load strategy.json");
//        string xmlStrategy=Util::readFile("conf/strategy-ost.xml");
//        //config::StrConfig strConfig;
//        //xpack::xml::decode(xmlStrategy, strConfig);
//
//
//        string jsonStrategy=Util::readFile("conf/strategy.json");
//        vector<config::StrategySetting> settings;
//        xpack::json::decode(jsonStrategy, settings);
//        for(auto & setting :settings){
//            if(setting.accountId==this->acctId){
//                strategySettings.push_back(setting);
//            }
//        }



    }
};

#endif //MTS_CORE_CONTEXT_H
