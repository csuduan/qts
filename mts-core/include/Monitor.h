#pragma  once
#include <thread>
#include "Logger.h"
class Monitor{
public:
    static double avgQueueDelay;
    static double avgTickToTrade;
    static double avgRtnDelay;
    static int orderCount;
    static int  tickCount;

    static void init(){
        thread t(display);
        t.detach();
    }

private:
    static void display(){
        while (true){
            Logger::getLogger().info("monitor--> avgQueueDelay:%f",avgQueueDelay);
            Logger::getLogger().info("monitor--> tickCount:%d",tickCount);
            sleep(10);
        }

    }
};