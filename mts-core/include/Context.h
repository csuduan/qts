//
// Created by 段晴 on 2022/2/28.
//

#ifndef MTS_CORE_CONTEXT_H
#define MTS_CORE_CONTEXT_H
#include "Singleton.h"
#include "tscns.h"
#include <thread>
#include "fmtlog/fmtlog.h"

class Context:public Singleton<Context>{
    friend class Singleton<Context>;

public:
    TSCNS tn;

    //初始化上下文
    void init(){
        //初始化日志
        fmtlog::setLogFile(stdout, false);
        fmtlog::setThreadName("main");
        fmtlog::startPollingThread(1e9);

        logi("init context ...");
        //todo 加载配置


        //初始化计时器
        double ghz=tn.init();
        logi("init tsn,ghz:{}",ghz);
        std::this_thread::sleep_for(std::chrono::seconds(1));
        tn.calibrate();
    }
};

#endif //MTS_CORE_CONTEXT_H
