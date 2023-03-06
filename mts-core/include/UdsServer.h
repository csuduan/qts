//
// Created by Administrator on 2020/6/18.
//

#ifndef TRADECORE_SERVER_H
#define TRADECORE_SERVER_H
#include <event.h>
#include <event2/listener.h>
#include <string>

class UdsServer {
public:
    UdsServer(std::string name): unName(name){}
    void   start();
    void   listern_callback(evconnlistener *listener, evutil_socket_t fd,sockaddr *sock, int socklen);
    void   read_callback(struct bufferevent *bev);
    void   write_callback(struct bufferevent *bev);
    void   event_callback(struct bufferevent *bev, short events);
    void   msg_handler(std::string date);

    event_base *base;

private:
    std::string unName;
    void run();

};


#endif //TRADECORE_SERVER_H
