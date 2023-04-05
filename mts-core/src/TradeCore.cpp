//
// Created by 段晴 on 2022/1/23.
//

#include <iostream>
#include <time.h>
#include "util/Util.h"
#include "define.h"
#include "engine/TradeExecutor.h"
#include "thread"
#include "Monitor.h"
#include "util/UdsServer.h"

#include "message.h"
//#include <stdio.h>
using namespace std;

void add(int a,int b){
    a+=b;
}

void testJson(){
    msg::Message msg;
    msg.no=1;
    msg.type=msg::MD_CONNECT;
    msg.data="{}";
    string json = xpack::json::encode(msg);

    msg::Message msg1;
    xpack::json::decode(json, msg1);
    cout<<json<<endl;

}

int main(int argc,char *argv[]) {
    //testJson();

    fmtlog::setLogFile(stdout, false);
    fmtlog::startPollingThread(5e9);
    //fmtlog::poll(true);

    if(argc !=2){
        loge("args error");
        return -1;
    }
    Monitor::init();

    string acctId=argv[1];
    TradeExecutor * tradeExecutor =new TradeExecutor(acctId);
    tradeExecutor->init();
    tradeExecutor->start();



    return 0;
}




