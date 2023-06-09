//
// Created by 段晴 on 2022/5/5.
//

#ifndef MTS_CORE_MSGLISTENER_H
#define MTS_CORE_MSGLISTENER_H

#include "Message.h"

class MsgListener {
public:
    virtual Message* onRequest(Message* request) =0;
};


#endif //MTS_CORE_MSGLISTENER_H
