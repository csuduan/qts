#pragma  once
#include <thread>
#include "define.h"
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
            //logi("monitor--> avgQueueDelay:{}",avgQueueDelay);
            //logi("monitor--> tickCount:{}",tickCount);

            //清零
            sleep(10);
        }

    }
};

int Monitor::tickCount=0;
double Monitor::avgQueueDelay=0;
double Monitor::avgRtnDelay =0;
int Monitor::orderCount=0;