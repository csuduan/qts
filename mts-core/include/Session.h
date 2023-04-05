//
// Created by Administrator on 2020/6/15.
//

#ifndef TRADECORE_SESSION_H
#define TRADECORE_SESSION_H

#endif //TRADECORE_SESSION_H

#include "util/UdsClient.h"

class Session{
private:
    TcpClient *client;
public:
    void Connect();
};