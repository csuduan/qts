//
// Created by 段晴 on 2022/1/24.
//

#include "GatewayFactory.h"
#include "CtpTdGateway.h"
#include "OstTdGateway.h"
#include "CtpMdGateway.h"
#include "OstMdGateway.h"
#include "define.h"
#include <unistd.h>
#include "LockFreeQueue.hpp"


MdGateway *GatewayFactory::createMdGateway(Account *account) {
    logi("create mdGateway {}",account->id);
    MdGateway *mdGateway = nullptr;
    if(account->loginInfo.mdType=="CTP")
        mdGateway=new CtpMdGateway(account);
    if(account->loginInfo.mdType=="OST")
        mdGateway=new OstMdGateway(account);
    mdGateway->connect();
    return mdGateway;
}

TdGateway *GatewayFactory::createTdGateway(Account *account) {
    logi("create tdGateway {}",account->id);
    TdGateway *gateway= nullptr;
    if(account->loginInfo.tdType=="CTP"){
        gateway=new CtpTdGateway(account);
    }
    if(account->loginInfo.tdType=="OST"){
        gateway=new OstTdGateway(account);
    }
    //gateway->connect();
    return gateway;
}