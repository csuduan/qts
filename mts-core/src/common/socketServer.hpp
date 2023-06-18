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
#include "lockFreeQueue.hpp"
#include "data.h"
#include "socketBase.h"
#include "message.h"

#include "msgListener.h"

#include <iostream>

#include <stdio.h>
#include <string.h>
#include "json/json.h"
#include "magic_enum.hpp"
#include "data.h"


#include <event2/bufferevent.h>
#include <event2/thread.h>
#include <arpa/inet.h>
#include <thread>
#include <functional>
#include "define.h"
#include <filesystem>

using namespace std;
using namespace std::placeholders;
using namespace std::filesystem;



//typedef void (*msgCallback)(msg::Message);
typedef std::function<void(Message)> msgCallback;
class SocketServer : public SocketBase{
public:
    msgCallback callback;
    SocketServer(SocketAddr& serverAddr, MsgListener *listener): SocketBase(serverAddr){
        this->listener=listener;
    }
    void   start(){
        name = socketAddr.name;
        logi("start server [{}] ...", name);

        if (this->socketAddr.type == SocketType::UDS) {
            struct sockaddr_un un;
            memset(&un, 0, sizeof(sockaddr_un));
            un.sun_family = AF_UNIX;
            //unlink(unName);
            if (!exists("/tmp/ipc/"))
                create_directories("/tmp/ipc/");
            string filename = "/tmp/ipc/" + socketAddr.unName + ".sock";
            unlink(filename.c_str());
            strcpy(un.sun_path, filename.c_str());
            runUds(un);
        } else {
            struct sockaddr_in in;
            memset(&in, 0, sizeof(in));
            in.sin_family = AF_INET;
            in.sin_port = htons(socketAddr.port);
            in.sin_addr.s_addr = 0;
            runTcp(in);
        }

    }
    void   listern_callback(evconnlistener *listener, evutil_socket_t fd,sockaddr *sock, int socklen){
        logi("server[{}] accept  client  connect", name);
        //保存连接
        bufferevent *bev = bufferevent_socket_new(this->base, fd, BEV_OPT_CLOSE_ON_FREE);
        //SocketSession *session = new SocketSession(bev, this->listener);
        //this->sessions.push_back(session);

        connections.insert(bev);
        bufferevent_setcb(bev,
                          [](bufferevent *bev, void *arg) {
                              SocketServer *server = (SocketServer *) arg;
                              server->read_callback(bev);
                          }, NULL,
                          [](bufferevent *bev, short events, void *arg) {
                              SocketServer *server = (SocketServer *) arg;
                              server->event_callback(bev, events);
                          }, this);
        bufferevent_setwatermark(bev, EV_READ, 4, 0);
        bufferevent_enable(bev, EV_READ | EV_PERSIST);
    }
    void   read_callback(struct bufferevent *bev){
        //采用4位长度+json字符串消息体
        int headLen=4;
        struct evbuffer *input = bufferevent_get_input(bev);
        while(true){
            //循环处理
            size_t len = evbuffer_get_length(input);
            if(len>=headLen){
                unsigned char head[headLen];
                bufferevent_read(bev, head, headLen);
                unsigned int msgLen;
                memcpy(&msgLen,head,headLen);
                msgLen=ntohl(msgLen);//大端转小端
                char msg[msgLen+1];
                len = bufferevent_read(bev, msg, msgLen);
                msg[len] = '\0';

                try{
                    logi("socketServer recv  msg: {}", string (msg));
                    Message *message=new Message;
                    xpack::json::decode(msg, *message);
                    auto msgType=magic_enum::enum_cast<MSG_TYPE>(message->type);
                    if(msgType.has_value()){
                        if(msgType== MSG_TYPE::PING){
                            message->type= enum_string(MSG_TYPE::PING);
                            string rsp=xpack::json::encode(*message);
                            this->push(rsp);
                        }else{
                            message->msgType=msgType.value();
                            if(!message->data.empty())
                                xpack::json::decode(message->data,message->jsonData);
                            //this->queue->push(Event{EvType::MSG,0,message});
                            Message* rsp = listener->onRequest(message);
                            this->push(*rsp);
                            delete rsp;
                            delete message;
                        }
                    }else{
                        loge("unknow msgType:{}",message->type);
                    }
                }catch(std::exception ex){
                    loge("process  message  error:{}",string (msg));
                }
            }else{
                break;
            }
        }
    }
    void   write_callback(struct bufferevent *bev){
    }
    void   event_callback(struct bufferevent *bev, short events){
        if (events & (BEV_EVENT_EOF | BEV_EVENT_ERROR))
        {
            loge("connection  closed !" );
            connections.erase(bev);
            bufferevent_free(bev);
        }
        else if(events & BEV_EVENT_CONNECTED)
        {
            logi( "connection  connected !");
        }else{
            logw("unkonw event:{}",events);
        }
    }
    void   setMsgCallback(const msgCallback & cb){
        this->callback=cb;
    }

    event_base *base;
    //bufferevent *bev;
    void push(const string & msg){
        for (auto &bev: connections) {
            int headLen=4;
            int msgLen=msg.length();
            int msgLenBig= htonl(msgLen);

            char buffer[headLen+msgLen];
            memset(buffer,0,headLen+msgLen);

            memcpy(buffer,&msgLenBig,headLen);
            memcpy(buffer+headLen,msg.c_str(),msgLen);
            bufferevent_write(bev, buffer, headLen+msgLen);
        }
    }
    void push(const Message & msg){
        string json = xpack::json::encode(msg);
        this->push(json);
    }
    //vector<SocketSession*> sessions;
    set<bufferevent *> connections;
    LockFreeQueue<Event> queue{1<<10};
private:
    MsgListener *listener;
    void   runTcp(struct sockaddr_in in){
        this->base = event_base_new();
        evconnlistener *listener
                = evconnlistener_new_bind(base,
                                          [](evconnlistener *listener, evutil_socket_t fd, sockaddr *sock, int socklen,
                                             void *arg) {
                                              SocketServer *server = (SocketServer *) arg;
                                              server->listern_callback(listener, fd, sock, socklen);
                                          }, this, LEV_OPT_REUSEABLE | LEV_OPT_CLOSE_ON_FREE, 10, (struct sockaddr *) &in,
                                          sizeof(in));
        if (!listener) {
            loge("server[{}] listen fail! {}", this->socketAddr.name, strerror(errno));
            throw exception();
        }
        logi("server[{}] listen success", this->socketAddr.name);

        //evconnlistener_set_error_cb(listener, accept_error_cb);
        event_base_dispatch(base);
        evconnlistener_free(listener);
        event_base_free(base);
        loge("server[{}] loop exit", name);
    }
    void   runUds(struct sockaddr_un un){
        this->base = event_base_new();
        evconnlistener *listener
                = evconnlistener_new_bind(base,
                                          [](evconnlistener *listener, evutil_socket_t fd, sockaddr *sock, int socklen,
                                             void *arg) {
                                              SocketServer *server = (SocketServer *) arg;
                                              server->listern_callback(listener, fd, sock, socklen);
                                          }, this, LEV_OPT_REUSEABLE | LEV_OPT_CLOSE_ON_FREE, 10, (struct sockaddr *) &un,
                                          sizeof(un));
        if (!listener) {
            //perror("Cannot create listener");
            loge("server[{}] listen fail! {}", this->socketAddr.name, strerror(errno));
            throw exception();
        }
        logi("server[{}] listen success", this->socketAddr.name);

        event_base_dispatch(base);
        evconnlistener_free(listener);
        event_base_free(base);
        logw("server[{}] loop exit", name);
    }
    std::string name;
};


#endif //TRADECORE_SERVER_H
