//
// Created by 段晴 on 2022/2/25.
// 行情接收器，收到行情后写入文件
//
#include "fmtlog/fmtlog.h"
#include <string>
#include "Context.h"
#include "MasterExecutor.h"
#include "signal.h"
#include <chrono>

void sigHandler(int signo) {
    logw("recv sig {}", signo);
    //logw("system will exit after 2s");
    std::this_thread::sleep_for(std::chrono::seconds(2));
    fmtlog::poll();
    //exit(-1);
    raise(signo);
}

int main(int argc,char *argv[]){
    signal(SIGFPE, sigHandler);
    signal(SIGABRT, sigHandler);
    signal(SIGPIPE, SIG_IGN);

    Context::get().init();
    fmtlog::setThreadName("main");
    std::string name="server";

    MasterExecutor* executor=new MasterExecutor();
    if(!executor->init()){
        loge("serverExecutor init fail");
        return -1;
    }
    executor->start();
    return 0;
}