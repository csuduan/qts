#include <iostream>
#include <time.h>
#include "util/Util.h"
#include "Logger.h"
#include "TradeEngine.h.bak"
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
    Logger::getLogger().info("\ttrade-core %s\n start...",engineId.c_str());

    TradeEngine* tradeEngine=new TradeEngine(engineId);
    tradeEngine->start();

    timespec time1, time2;
    int temp;
    clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &time1);
    for (int i = 0; i< 1000000; i++)
    {
        //add(temp,i);
        Logger::getLogger().debug("测试 %s",engineId.c_str());
    }

    clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &time2);
    timespec delay= Util::diff(time1,time2);
    cout<<delay.tv_sec<<":"<<delay.tv_nsec<<endl;


    return 0;
}

