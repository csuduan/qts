//
// Created by Administrator on 2020/6/15.
// 用于本地C++进程间通信
//

#ifndef TRADECORE_TCPCLIENT_H
#define TRADECORE_TCPCLIENT_H
#include <string>
#include <event2/event.h>
#include "SocketData.h"
#include "SocketBase.h"
#include "Enums.h"
#include "Data.h"


class SocketClient :public SocketBase{
public:
    SocketClient(SocketAddr & socketAddr): SocketBase(socketAddr){
    };
    ~SocketClient(){
    };

    void   start();
    void   read_callback(struct bufferevent *bev);
    void   write_callback(struct bufferevent *bev);
    void   event_callback(struct bufferevent *bev, short events);
    struct event_base* base;

    void  request(const string & msg);
    void  request(const Message & msg);
    LockFreeQueue<Event> queue{1<<10};

private:
    bool  connected=false;
    bufferevent * connEv;
    void   connect();

};


#endif //TRADECORE_TCPCLIENT_H
