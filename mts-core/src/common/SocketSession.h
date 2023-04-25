//
// Created by 段晴 on 2022/3/2.
//

#ifndef MTS_CORE_SOCKETSESSION_H
#define MTS_CORE_SOCKETSESSION_H
#include <string>
#include <event.h>
#include "fmtlog/fmtlog.h"
#include "LockFreeQueue.hpp"
#include "Data.h"
using std::string;

enum SessionStatus{
    DISCONNECED,
    CONNECTED,
    INITED,
};
class SocketSession {

public:
    SocketSession(bufferevent *bev,LockFreeQueue<Event> *queue){
        this->bev=bev;
        this->queue=queue;
    }

    SessionStatus status;
    bool  connected= true;
    bufferevent * bev;

    void   read_callback(struct bufferevent *bev);
    void   write_callback(struct bufferevent *bev);
    void   event_callback(struct bufferevent *bev, short events);

    string id;//回话ID
    LockFreeQueue<Event> *queue;

    bool send(const string & msg);
};


#endif //MTS_CORE_SOCKETSESSION_H
