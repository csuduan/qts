//
// Created by 段晴 on 2022/1/23.
//
#include "Logger.h"
#include <string>
#include "UdsClient.h"


int main(int argc,char *argv[]){
    if(argc !=2){
        Logger::getLogger().error("args error");
        return -1;
    }

    string engineId=argv[1];
    Logger::getLogger().info("uds-client %s start...",engineId.c_str());
    UdsClient* tcpClient=new UdsClient(engineId);
    tcpClient->start();
    Logger::getLogger().info("uds-client %s exit...",engineId.c_str());
}