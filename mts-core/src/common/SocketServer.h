//
// Created by Administrator on 2020/6/18.
//

#ifndef TRADECORE_SERVER_H
#define TRADECORE_SERVER_H
#include <event.h>
#include <event2/listener.h>
#include <string>
#include <functional>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>
#include<sys/un.h>
#include "LockFreeQueue.hpp"
#include "Data.h"
#include "SocketSession.h"
#include "SocketData.h"
#include "SocketBase.h"


//typedef void (*msgCallback)(msg::Message);
typedef std::function<void(Message)> msgCallback;
class SocketServer :public SocketBase{
public:
    msgCallback callback;
    SocketServer(SocketAddr& serverAddr): SocketBase(serverAddr){}
    void   start();
    void   listern_callback(evconnlistener *listener, evutil_socket_t fd,sockaddr *sock, int socklen);
    void   read_callback(struct bufferevent *bev);
    void   write_callback(struct bufferevent *bev);
    void   event_callback(struct bufferevent *bev, short events);
    void   setMsgCallback(const msgCallback & cb){
        this->callback=cb;
    }

    event_base *base;
    //bufferevent *bev;
    void push(const string & msg);
    void push(const Message & msg);
    vector<SocketSession*> sessions;
    LockFreeQueue<Event> queue{1<<10};
private:
    void   runTcp(struct sockaddr_in in);
    void   runUds(struct sockaddr_un un);
    std::string name;
};


#endif //TRADECORE_SERVER_H
