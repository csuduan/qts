//
// Created by 段晴 on 2022/1/24.
//

#ifndef MTS_CORE_GATEWAYFACTORY_HPP
#define MTS_CORE_GATEWAYFACTORY_HPP
#include "gateway.h"
#include "trade/acct.h"
#include "ctpTdGateway.hpp"
#include "ostTdGateway.hpp"
#include "ctpMdGateway.hpp"
#include "ostMdGateway.hpp"
#include "efhMdGateway.hpp"
#include "define.h"

class Acct;
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
    static MdGateway* createMdGateway(QuoteInfo* quota){
        logi("create mdGateway {}",quota->id);
        MdGateway *mdGateway = nullptr;

        if(quota->type=="CTP")
            mdGateway=new CtpMdGateway(quota);
        else if(quota->type=="OST")
            mdGateway=new OstMdGateway(quota);
        else if(quota->type=="EFH")
            mdGateway=new ElfMdGateway(quota);
        else
            throw "unknow mdType";


        if(quota->autoConnect){
            logi("connect mdGateway {}",quota->id);
            mdGateway->connect();
        }
        return mdGateway;
    }
};


#endif //MTS_CORE_GATEWAYFACTORY_HPP
