//
// Created by 段晴 on 2022/1/23.
//

#include <iostream>
#include "common/util.hpp"
#include "trade/tradeExecutor.h"
#include "monitor.h"
#include "context.h"
#include "signal.h"
#include <filesystem>

//#include <stdio.h>

void sigHandler(int signo) {
    logw("recv sig {}", signo);
    //logw("system will exit after 2s");
    std::this_thread::sleep_for(std::chrono::seconds(2));
    fmtlog::poll();
    exit(0);
    //raise(signo);
}

int main(int argc,char *argv[]) {
    signal(SIGABRT, sigHandler);
    signal(SIGTERM, sigHandler);
    signal(SIGHUP, sigHandler);
    signal(SIGINT, sigHandler);
    signal(SIGSTOP, sigHandler);
    signal(SIGQUIT, sigHandler);




    //初始化日志
    if(!std::filesystem::exists("logs"))
        std::filesystem::create_directories("logs");

    string date=Util::getDate();
    string file="logs/mts-trade_"+date+".log";
    //fmtlog::setLogFile(file.c_str(), false);
    fmtlog::setLogFile(stdout, false);
    fmtlog::setThreadName("main");
    fmtlog::startPollingThread(1e9);
    if(argc !=2){
        loge("args error");
        return -1;
    }

    string acctId=argv[1];
    Monitor::get();

    TradeExecutor * tradeExecutor =new TradeExecutor(acctId);
    tradeExecutor->init();
    tradeExecutor->start();

    while (true){
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    }

    return 0;
}




