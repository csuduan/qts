//
// Created by 段晴 on 2022/1/24.
//

#include "GatewayFactory.h"
#include "CtpTdGateway.h"
#include "CtpMdGateway.h"
#include "define.h"
#include <unistd.h>
#include "LockFreeQueue.hpp"


MdGateway *GatewayFactory::createMdGateway(Account *account) {
    logi("create mdGateway {}",account->id);
    MdGateway *mdGateway = nullptr;
    mdGateway=new CtpMdGateway(account);
    mdGateway->connect();
    return mdGateway;
}

TdGateway *GatewayFactory::createTdGateway(Account *account) {
    logi("create tdGateway {}",account->id);
    TdGateway *gateway= nullptr;
    if(account->loginInfo.tdType=="CTP"){
        gateway=new CtpTdGateway(account);
    }
    gateway->connect();
    return gateway;
}