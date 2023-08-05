#pragma  once
#include <thread>
#include "define.h"
#include "singleton.h"
class Monitor:public Singleton<Monitor>{
    friend class Singleton<Monitor>;

public:
    double avgQueueDelay=0;
    double avgTickToTrade=0;
    double avgRtnDelay=0;
    int orderCount=0;
    int  tickCount=0;

private:
    Monitor(){
        thread t([this](){
            this->display();
        });
        t.detach();
    }
    Monitor(const Monitor&)=delete;
    Monitor& operator =(const Monitor&)= delete;


     void display(){
        while (true){
            //logi("monitor--> avgQueueDelay:{}",avgQueueDelay);
            //logi("monitor--> tickCount:{}",tickCount);
            //清零
            sleep(10);
        }

    }
};
