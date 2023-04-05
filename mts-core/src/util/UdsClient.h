//
// Created by Administrator on 2020/6/15.
//
#include <string>
#include <event2/event.h>
#ifndef TRADECORE_TCPCLIENT_H
#define TRADECORE_TCPCLIENT_H


class UdsClient {
public:

    UdsClient(std::string unName): unName(unName){
    };
    ~UdsClient(){
    };

    void   start();

    void   read_callback(struct bufferevent *bev);
    void   write_callback(struct bufferevent *bev);
    void   event_callback(struct bufferevent *bev, short events);
    struct event_base* base;


private:
    std::string unName;
    bool  connected=false;



    void   start_uds();



};


#endif //TRADECORE_TCPCLIENT_H
