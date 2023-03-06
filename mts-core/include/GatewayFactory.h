//
// Created by 段晴 on 2022/1/24.
//

#ifndef MTS_CORE_GATEWAYFACTORY_H
#define MTS_CORE_GATEWAYFACTORY_H
#include "Data.h"
#include "Gateway.h"
#include "LockFreeQueue.h"

class GatewayFactory {
public:
    /**
     * 创建交易接口
     * @param account
     * @return
     */
    static TdGateway* createTdGateway(Account & account, LockFreeQueue<Event> * queue);
    /**
     * 创建行情接口
     * @param mdInfo
     * @return
     */
    static MdGateway* createMdGateway(MdInfo & mdInfo, LockFreeQueue<Event> * queue);
};


#endif //MTS_CORE_GATEWAYFACTORY_H
