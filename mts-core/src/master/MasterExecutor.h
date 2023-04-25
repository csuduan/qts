//
// Created by 段晴 on 2022/3/2.
//

#ifndef MTS_CORE_MASTEREXECUTOR_H
#define MTS_CORE_MASTEREXECUTOR_H
#include "common/SocketServer.h"
#include "common/SocketClient.h"
#include "Util.h"
#include "fmtlog/fmtlog.h"
#include "gateway/Gateway.h"
#include "SqliteHelper.hpp"

class MasterExecutor {
public:
    bool init();
    bool start();
    void dump();


private:
    SocketServer * tcpServer;//对外提供服务
    SocketServer * udsServer;//对内提供服务

    map<string,MdGateway*> mdGatewayMap;//多路行情
    SqliteHelper *sqliteHelper;
    map<string,Contract*> contractMap;
    vector<Quote *> quotes;

    void msgHandler();
    void processMsg(Message * msg);
};


#endif //MTS_CORE_MASTEREXECUTOR_H
