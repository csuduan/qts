//
// Created by 段晴 on 2022/1/24.
//

#ifndef MTS_CORE_GATEWAYFACTORY_H
#define MTS_CORE_GATEWAYFACTORY_H
#include "Data.h"
#include "Gateway.h"
#include "LockFreeQueue.hpp"

#include "Config.h"
class GatewayFactory {
public:
    /**
     * 创建交易接口
     * @param account
     * @return
     */
    static TdGateway* createTdGateway(Acct * account);
    /**
     * 创建行情接口
     * @param mdInfo
     * @return
     */
    static MdGateway* createMdGateway(Quote* quote);
};


#endif //MTS_CORE_GATEWAYFACTORY_H
