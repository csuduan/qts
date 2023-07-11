//
// Created by 段晴 on 2022/1/23.
// 行情接收器，接受多个行情源，持久化到文件
//

#include <iostream>
#include "common/util.hpp"
#include "monitor.h"
#include "signal.h"
#include <filesystem>

#include "quotaExecutor.h"

//#include <stdio.h>

void sigHandler(int signo) {
    logw("recv sig {}", signo);
    //logw("system will exit after 2s");
    std::this_thread::sleep_for(std::chrono::seconds(2));
    fmtlog::poll();
    exit(0);
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
    string file="logs/mts-quota_"+date+".log";
    //fmtlog::setLogFile(file.c_str(), false);
    fmtlog::setLogFile(stdout, false);
    fmtlog::setThreadName("main");
    fmtlog::startPollingThread(1e9);




    //string acctId=argv[1];
    Monitor::get();

    QuotaExecutor * tradeExecutor =new QuotaExecutor();
    tradeExecutor->start();

    while (true){
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    }
    return 0;
}




