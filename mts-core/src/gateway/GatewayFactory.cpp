//
// Created by 段晴 on 2022/1/24.
//

#include "GatewayFactory.h"
#include "CtpTdGateway.h"
#include "CtpMdGateway.h"
#include "Logger.h"
#include <unistd.h>
#include "LockFreeQueue.h"


MdGateway *GatewayFactory::createMdGateway(MdInfo &mdInfo, LockFreeQueue<Event> * queue) {
    Logger::getLogger().info("create mdGateway %s",mdInfo.id.c_str());
    MdGateway *mdGateway = nullptr;
    mdGateway=new CtpMdGateway(mdInfo,queue);
    mdGateway->connect();
    return mdGateway;
}

TdGateway *GatewayFactory::createTdGateway(Account &account,LockFreeQueue<Event> * queue ) {
    Logger::getLogger().info("create tdGateway %s",account.id.c_str());
    TdGateway *gateway= nullptr;
    if(account.loginInfo.tdType=="CTP"){
        gateway=new CtpTdGateway(account.loginInfo,queue);
    }
    gateway->connect();
    return gateway;
}