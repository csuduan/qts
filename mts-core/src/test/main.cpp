//
// Created by 段晴 on 2022/2/25.
// 行情
// 接收到行情后，写入到共享内存，供交易程序轮询
//
#include "fmtlog/fmtlog.h"
#include <string>
#include "context.h"
#include "lockFreeQueue.hpp"
#include "data.h"
#include <thread>
#include <chrono>
#include "cpuin.h"
#include "SocketData.h"
#include "common/socketServer.hpp"
#include "common/socketClient.hpp"

void writeQueue(LockFreeQueue<Event> *queue){
    while (true){
        long tsc=Context::get().tn.rdns();
        Event event{EvType::TICK,tsc};
        queue->push(event);
        std::this_thread::sleep_for(std::chrono::microseconds(200));
    }
}

void readQueue(LockFreeQueue<Event> *queue){
    cpupin(1);
    Event event;
    while (true){
        long tsc=Context::get().tn.rdns();
        if(queue->pop(event)){
            long delaytsc=tsc-event.tsc;
            //long delayns=Context::get().tn.tscdelay2ns(delaytsc);
            long delayns=0;
            logi("new data, delaytsc={},delayns={}",delaytsc,delayns);
        }

    }
}

void testQuue(){
    LockFreeQueue<Event> *queue =new LockFreeQueue<Event>(1024);

    std::thread t1([queue](){
        readQueue(queue);
    });

    std::thread t2([queue](){
        writeQueue(queue);
    });
}

int main(int argc,char *argv[]){
    string str="hhhhhllllllooooowwwwdddd";
    Message msg;
    msg.type=1;
    msg.no=0;
    msg.data=str;
    string json=xpack::json::encode(msg);
    Message msg1;
    xpack::json::decode(json,msg1);


    Context::get().init();
    fmtlog::setThreadName("main");

    OrderReq req;
    req.symbol="cu2021";
    req.volume=100;
    req.offset= "buy";
    OrderReq req1;
    memcpy(&req1,&req,sizeof(req1));
    //testQuue();
    OrderReq * req2=new OrderReq();
    memcpy(req2,&req,sizeof(OrderReq));


    TSCNS tn;
    tn.init();
    std::this_thread::sleep_for(std::chrono::seconds(1));
    tn.calibrate();

    long n1=tn.rdns();
    uint64_t t1=n1;
    long count=0;
    OFFSET offset=OFFSET::OPEN;
    for(int i=0;i<=1000000;i++){
        auto offset_s=magic_enum::enum_name(offset);
        string st="";
    }
    long n2=tn.rdns();
    std::cout<<"rdns avg cost:"<<(n2-n1)/1000000<<"ns,reverse count:"<<count<<std::endl;

    std::this_thread::sleep_for(std::chrono::seconds (60));
    return 0;
}
