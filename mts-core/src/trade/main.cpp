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
    Context::get().init();
    if(argc !=2){
        loge("args error");
        return -1;
    }
    Monitor::get();

    string acctId=argv[1];
    TradeExecutor * tradeExecutor =new TradeExecutor(acctId);
    tradeExecutor->init();
    tradeExecutor->start();
    return 0;
}




