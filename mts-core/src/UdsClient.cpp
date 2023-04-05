//
// Created by 段晴 on 2022/1/23.
//
#include "define.h"
#include <string>
#include "util/UdsClient.h"


int main(int argc,char *argv[]){
    fmtlog::setLogFile(stdout, false);
    fmtlog::startPollingThread(5e9);

    if(argc !=2){
        loge("args error");
        return -1;
    }


    string engineId=argv[1];
    logi("uds-client %s start...",engineId.c_str());
    UdsClient* tcpClient=new UdsClient(engineId);
    tcpClient->start();
    logi("uds-client %s exit...",engineId.c_str());
}