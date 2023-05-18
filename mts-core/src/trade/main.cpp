//
// Created by 段晴 on 2022/1/23.
//

#include <iostream>
#include "Util.h"
#include "trade/TradeExecutor.h"
#include "Monitor.h"
#include "Context.h"
//#include <stdio.h>
int main(int argc,char *argv[]) {
    //初始化日志
    fmtlog::setLogFile(stdout, false);
    fmtlog::setThreadName("main");
    fmtlog::startPollingThread(1e9);
    if(argc !=2){
        loge("args error");
        return -1;
    }

    string acctId=argv[1];
    Context::get().init(acctId);
    Monitor::get();

    TradeExecutor * tradeExecutor =new TradeExecutor(acctId);
    tradeExecutor->init();
    tradeExecutor->start();

    while (true){
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    }

    return 0;
}




