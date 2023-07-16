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
#include "context.h"


int main(int argc, char *argv[]) {
    Context::get().init("quote", "conf/setting-quote.json");
    Monitor::get();
    QuotaExecutor *tradeExecutor = new QuotaExecutor();
    tradeExecutor->start();

    while (true) {
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    }
    return 0;
}




