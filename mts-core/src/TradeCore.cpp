//
// Created by 段晴 on 2022/1/23.
//

#include <iostream>
#include <time.h>
#include "Util.h"
#include "Logger.h"
#include "TradeEngine.h"
#include "thread"
//#include <stdio.h>
using namespace std;

void add(int a,int b){
    a+=b;
}

int main(int argc,char *argv[]) {
    if(argc !=2){
        Logger::getLogger().error("args error");
        return -1;
    }
    string engineId=argv[1];
    Logger::getLogger().info("trade-core %s  start...",engineId.c_str());

    TradeEngine* tradeEngine=new TradeEngine(engineId);
    tradeEngine->start();

    return 0;
}




