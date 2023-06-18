//
// Created by 段晴 on 2022/1/24.
//

#ifndef MTS_CORE_GATEWAYFACTORY_HPP
#define MTS_CORE_GATEWAYFACTORY_HPP
#include "data.h"
#include "gateway.h"
#include "lockFreeQueue.hpp"

#include "config.h"
#include "acct.h"

#include "gatewayFactory.hpp"
#include "ctpTdGateway.h"
#include "ostTdGateway.hpp"
#include "ctpMdGateway.hpp"
#include "ostMdGateway.hpp"
#include "efhMdGateway.hpp"
#include "define.h"
#include <unistd.h>
#include "config.h"

class GatewayFactory {
public:
    /**
     * 创建交易接口
     * @param account
     * @return
     */
    static TdGateway* createTdGateway(Acct * account){
        logi("create tdGateway {}",account->id);
        TdGateway *gateway= nullptr;
        if(account->acctConf->tdType=="CTP"){
            gateway=new CtpTdGateway(account);
        }else if(account->acctConf->tdType=="OST"){
            gateway=new OstTdGateway(account);
        }else
            throw "unknow tdType";

        if(account->autoConnect){
            logi("connect tdGateway {}",account->id);
            gateway->connect();
        }
        return gateway;
    }
    /**
     * 创建行情接口
     * @param mdInfo
     * @return
     */
    static MdGateway* createMdGateway(Acct* acct){
        logi("create mdGateway {}",acct->id);
        MdGateway *mdGateway = nullptr;

        if(acct->acctConf->mdType=="CTP")
            mdGateway=new CtpMdGateway(acct);
        else if(acct->acctConf->mdType=="OST")
            mdGateway=new OstMdGateway(acct);
        else if(acct->acctConf->mdType=="EFH")
            mdGateway=new ElfMdGateway(acct);
        else
            throw "unknow mdType";


        if(acct->autoConnect){
            logi("connect mdGateway {}",acct->id);
            mdGateway->connect();
        }
        return mdGateway;
    }
};


#endif //MTS_CORE_GATEWAYFACTORY_HPP
