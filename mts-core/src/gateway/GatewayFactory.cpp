//
// Created by 段晴 on 2022/1/24.
//

#include "GatewayFactory.h"
#include "CtpTdGateway.h"
#include "OstTdGateway.h"
#include "CtpMdGateway.h"
#include "OstMdGateway.h"
#include "EfhMdGateway.h"
#include "define.h"
#include <unistd.h>
#include "LockFreeQueue.hpp"
#include "Config.h"

MdGateway *GatewayFactory::createMdGateway(Quote* quote) {
    logi("create mdGateway {}",quote->id);
    MdGateway *mdGateway = nullptr;

    if(quote->type=="CTP")
        mdGateway=new CtpMdGateway(quote);
    if(quote->type=="OST")
        mdGateway=new OstMdGateway(quote);
    if(quote->type=="EFH")
        mdGateway=new ElfMdGateway(quote);

    if(mdGateway!= nullptr && quote->autoConnect){
        logi("connect mdGateway {}",quote->id);
        mdGateway->connect();
    }
    return mdGateway;
}

TdGateway *GatewayFactory::createTdGateway(Acct *account) {
    logi("create tdGateway {}",account->id);
    TdGateway *gateway= nullptr;
    if(account->loginInfo.tdType=="CTP"){
        gateway=new CtpTdGateway(account);
    }
    if(account->loginInfo.tdType=="OST"){
        gateway=new OstTdGateway(account);
    }
    if(account->autoConnect){
        logi("connect tdGateway {}",account->id);
        gateway->connect();
    }
    return gateway;
}