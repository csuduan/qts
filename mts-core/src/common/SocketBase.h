//
// Created by 段晴 on 2022/3/2.
//

#ifndef MTS_CORE_SOCKETBASE_H
#define MTS_CORE_SOCKETBASE_H

#include "fmtlog/fmtlog.h"

enum SocketType{
    UDS,
    TCP
};


struct SocketAddr{
    string name;
    SocketType type; //0-uds,1-tcp
    int port;
    string unName;
};

class SocketBase{

protected:
    SocketAddr socketAddr;
public:
    SocketBase(SocketAddr &socketAddr): socketAddr(socketAddr){
    }

};

#endif //MTS_CORE_SOCKETBASE_H
