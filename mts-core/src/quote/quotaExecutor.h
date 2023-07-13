//
// Created by 段晴 on 2022/6/8.
//

#ifndef MTS_CORE_QUOTAEXECUTOR_H
#define MTS_CORE_QUOTAEXECUTOR_H
#include "data.h"
#include "gateway/gateway.h"
#include "tscns.h"
#include "config.h"

class QuotaExecutor {

public:
    void start();

private:
    TSCNS tn;
    vector<MdGateway *> mdGateways;
    void msgHanler();

    void connect();
    void disconnect();

};


#endif //MTS_CORE_QUOTAEXECUTOR_H
