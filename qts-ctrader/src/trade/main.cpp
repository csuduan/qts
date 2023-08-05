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


int main(int argc, char *argv[]) {



    //自重启
//    while(1){
//        //遍历应用打开的系统文件句柄
//        for(int i=0;i<sysconf(_SC_OPEN_MAX);i++){
//            //过滤标准输入输出并关闭
//            if(i != STDIN_FILENO && i != STDOUT_FILENO && i != STDERR_FILENO)
//                close(i);
//        }
//        //程序启动命令
//        char *args[] = {(char*)"myprogram.exe",(char*)"config.json",0};
//        //调用系统内核
//        execv("/proc/self/exe",args);
//        exit(0);
//
//    }


    //初始化日志
    if (!std::filesystem::exists("logs"))
        std::filesystem::create_directories("logs");

    string date = Util::getDate();
    string file = "logs/mts-trade_" + date + ".log";
    //fmtlog::setLogFile(file.c_str(), false);
    fmtlog::setLogFile(stdout, false);
    fmtlog::setThreadName("main");
    fmtlog::startPollingThread(1e9);
    if (argc != 2) {
        loge("args error");
        return -1;
    }

    string acctId = argv[1];
    Monitor::get();
    Context::get().init(acctId, "conf/setting-trade.json");
    TradeExecutor *tradeExecutor = new TradeExecutor(acctId);
    tradeExecutor->start();

    while (true) {
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    }

    return 0;
}




