//
// Created by Administrator on 2020/6/18.
//

#include "UdsServer.h"
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>
#include<sys/un.h>
#include <iostream>

#include <stdio.h>
#include <string.h>
#include "json/json.h"



#include <event2/bufferevent.h>
#include <event2/thread.h>
#include <arpa/inet.h>
#include <thread>
#include <functional>
#include "Logger.h"

using namespace std;
using namespace  std::placeholders;

void UdsServer::start() {
    //std::thread t(std::bind(&UdsServer::run,this));
    //t.detach();
    Logger::getLogger().info("uds server start...");
    run();


}
void UdsServer::run() {
    struct sockaddr_un un;
    memset(&un, 0, sizeof(sockaddr_un));
    un.sun_family = AF_UNIX;
   //unlink(unName);
    string filename="/tmp/sock/"+unName;
    unlink(filename.c_str());
    strcpy(un.sun_path,filename.c_str());

    this->base = event_base_new();
    evconnlistener *listener
            = evconnlistener_new_bind(base,
                                      [](evconnlistener *listener, evutil_socket_t fd,sockaddr *sock, int socklen, void *arg){
                                          UdsServer* server=(UdsServer*)arg;
                                          server->listern_callback(listener,fd,sock,socklen);
                                      }
                    ,this,LEV_OPT_REUSEABLE|LEV_OPT_CLOSE_ON_FREE,10, (struct sockaddr*)&un, sizeof(un));
    if(!listener){
        perror("Cannot create listener");
        return ;
    }

    event_base_dispatch(base);

    evconnlistener_free(listener);
    event_base_free(base);
    cout<<"UdsServer loop exit";
}
void UdsServer::listern_callback(evconnlistener *listener, int fd, sockaddr *sock, int socklen) {
    cout<<"accept  client  connect\n";
    bufferevent *bev = bufferevent_socket_new(this->base, fd, BEV_OPT_CLOSE_ON_FREE);
    bufferevent_setcb(bev,
            [](bufferevent *bev, void *arg){UdsServer* server=(UdsServer*)arg;server->read_callback(bev);}
    , NULL,
    [](bufferevent *bev, short events, void *arg){UdsServer* server=(UdsServer*)arg; server->event_callback(bev, events);}
    , this);
    bufferevent_setwatermark(bev,EV_READ,4,0);
    bufferevent_enable(bev, EV_READ | EV_PERSIST);
}

void UdsServer::read_callback(struct bufferevent *bev) {
    int headLen=4;
    //请求格式：4位消息长度+消息体
    struct evbuffer *input = bufferevent_get_input(bev);
    while(true){
        //循环处理
        size_t len = evbuffer_get_length(input);
        if(len>=4){
            char head[5]={0};
            bufferevent_read(bev, head, headLen);
            int msgLen = atoi(head);
            char msg[msgLen+1];
            len = bufferevent_read(bev, msg, msgLen);
            msg[len] = '\0';
            //cout<<"recv:"<<msg<<endl;
            //消息解析
            this->msg_handler(msg);

        }else{
            break;
        }
        char reply[] = "OK";
        bufferevent_write(bev, reply, strlen(reply));
    }
}

void UdsServer::write_callback(struct bufferevent *bev) {

}

void UdsServer::event_callback(struct bufferevent *bev, short events) {
    if (events & BEV_EVENT_EOF)
    {
        cout << "connection closed !" <<  endl;
    }
    else if (events & BEV_EVENT_ERROR)
    {
        cout << "some other error !" << endl;
    }
    else if(events & BEV_EVENT_CONNECTED)
    {
        cout << "connection connected !" <<  endl;
    }else{
        cout<<"unkonw event:"<<events <<endl;

    }

    //bufferevent_free(bev);
    //cout << "bufferevent 资源已经被释放..." << endl;
}

void UdsServer::msg_handler(std::string msg) {
    cout<<"handle msg:"<<msg<<endl;
    try {
        Json::Value root;
        Json::Reader reader;
        reader.parse(msg, root);
        const std::string cmd = root["cmd"].asString();
        const std::string data= root["data"].asString();
        cout<<"cmd:"<<cmd<<" "<<"data:"<<data<<endl;
    }catch (exception ex){
        cout<<"parse json error" <<ex.what()<<endl;
    }

}




