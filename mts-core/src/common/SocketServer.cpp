//
// Created by Administrator on 2020/6/18.
//

#include "SocketServer.h"

#include <iostream>

#include <stdio.h>
#include <string.h>
#include "json/json.h"
#include "magic_enum.hpp"
#include "Data.h"



#include <event2/bufferevent.h>
#include <event2/thread.h>
#include <arpa/inet.h>
#include <thread>
#include <functional>
#include "define.h"
#include <filesystem>
using namespace std;
using namespace  std::placeholders;
using namespace std::filesystem;

void SocketServer::start() {
    name=socketAddr.name;
    logi("socket server [{}] start..",name);

    if(this->socketAddr.type==SocketType::UDS){
        struct sockaddr_un un;
        memset(&un, 0, sizeof(sockaddr_un));
        un.sun_family = AF_UNIX;
        //unlink(unName);
        if(!exists("/tmp/ipc/"))
            create_directories("/tmp/ipc/");
        string filename="/tmp/ipc/"+socketAddr.unName;
        unlink(filename.c_str());
        strcpy(un.sun_path,filename.c_str());
        runUds(un);
    }else{
        struct sockaddr_in in;
        memset(&in, 0, sizeof(in));
        in.sin_family = AF_INET;
        in.sin_port = htons(socketAddr.port);
        in.sin_addr.s_addr = 0;
        runTcp(in);
    }



}

void SocketServer::runTcp(struct sockaddr_in in) {
    this->base = event_base_new();
    evconnlistener *listener
            = evconnlistener_new_bind(base,
                                      [](evconnlistener *listener, evutil_socket_t fd,sockaddr *sock, int socklen, void *arg){
                                          SocketServer* server=(SocketServer*)arg;
                                          server->listern_callback(listener,fd,sock,socklen);
                                      }
                    ,this,LEV_OPT_REUSEABLE|LEV_OPT_CLOSE_ON_FREE,10, (struct sockaddr*)&in, sizeof(in));
    if(!listener){
        loge("socket  server [{}] listen fail! {}",this->socketAddr.name,strerror(errno));
        throw exception();
    }
    logi("socket server [{}] listen success",this->socketAddr.name);

    //evconnlistener_set_error_cb(listener, accept_error_cb);
    event_base_dispatch(base);
    evconnlistener_free(listener);
    event_base_free(base);
    logw("{} SocketServer loop exit",name);
}

void SocketServer::runUds(struct sockaddr_un un) {
    this->base = event_base_new();
    evconnlistener *listener
            = evconnlistener_new_bind(base,
                                      [](evconnlistener *listener, evutil_socket_t fd,sockaddr *sock, int socklen, void *arg){
                                          SocketServer* server=(SocketServer*)arg;
                                          server->listern_callback(listener,fd,sock,socklen);
                                      }
                    ,this,LEV_OPT_REUSEABLE|LEV_OPT_CLOSE_ON_FREE,10, (struct sockaddr*)&un, sizeof(un));
    if(!listener){
        //perror("Cannot create listener");
        loge("socket server [{}] listen fail! {}",this->socketAddr.name,strerror(errno));
        throw exception();
    }
    logi("socket server [{}] listen success",this->socketAddr.name);

    event_base_dispatch(base);
    evconnlistener_free(listener);
    event_base_free(base);
    logw("{} SocketServer loop exit",name);
}
void SocketServer::listern_callback(evconnlistener *listener, int fd, sockaddr *sock, int socklen) {
    logi("[{}] accept  client  connect",name);
    //保存连接
    bufferevent * bev = bufferevent_socket_new(this->base, fd, BEV_OPT_CLOSE_ON_FREE);
    SocketSession * session=new SocketSession(bev,&this->queue);
    this->sessions.push_back(session);

    bufferevent_setcb(bev,
            [](bufferevent *bev, void *arg){SocketSession* session=(SocketSession*)arg;session->read_callback(bev);}
    , NULL,
    [](bufferevent *bev, short events, void *arg){SocketSession* session=(SocketSession*)arg; session->event_callback(bev, events);}
    , session);
    bufferevent_setwatermark(bev,EV_READ,4,0);
    bufferevent_enable(bev, EV_READ | EV_PERSIST);
}


/**
 * 字节序号反转
 * @param p
 * @param size
 */
void reverse(unsigned char *p,int size){
    for(int i=0;i<size/2;++i){
        int temp = p[i];
        p[i]=p[size-1-i];
        p[size-1-i] = temp;
    }
}


void SocketServer::read_callback(struct bufferevent *bev) {
    //采用4位长度+json字符串消息体
    int headLen=4;
    struct evbuffer *input = bufferevent_get_input(bev);
    while(true){
        //循环处理
        size_t len = evbuffer_get_length(input);
        if(len>=headLen){
            unsigned char head[headLen];
            bufferevent_read(bev, head, headLen);
            //reverse(head,headLen);//大端转小端
            unsigned int msgLen;
            memcpy(&msgLen,head,headLen);
            char msg[msgLen+1];
            len = bufferevent_read(bev, msg, msgLen);
            msg[len] = '\0';

            try{
                logi("recv msg: {}", string (msg));
                Message *message=new Message;
                xpack::json::decode(msg, *message);
                auto msgType=magic_enum::enum_cast<MSG_TYPE>(message->type);
                if(msgType.has_value()){
                    message->msgType=msgType.value();
                    this->queue.push(Event{EvType::MSG,0,message});
                }else{
                    loge("unknow msgType:{}",message->type);
                }
            }catch(exception ex){
                loge("valid json message");
            }

        }else{
            break;
        }
    }
}

void SocketServer::write_callback(struct bufferevent *bev) {
}

void SocketServer::event_callback(struct bufferevent *bev, short events) {
    if (events & (BEV_EVENT_EOF | BEV_EVENT_ERROR))
    {
        loge("connection closed !" );
//        for (auto iter = sessions.cbegin(); iter != sessions.cend(); iter++) {
//            cout << (*iter) << endl;
//            if((*iter)->bev ==bev){
//                (*iter)->status==DISCONNECED;
//                auto session=(*iter);
//                iter = sessions.erase(iter);
//                delete session;
//            }
//        }
    }
    else if(events & BEV_EVENT_CONNECTED)
    {
        logi( "connection connected !");
    }else{
        logw("unkonw event:{}",events);

    }
    //bufferevent_free(bev);
    //cout << "bufferevent 资源已经被释放..." << endl;
}

void SocketServer::push(const string &msg) {
    for(auto & item :sessions){
        if(item->connected == true)
            item->send(msg);
    }
}

void SocketServer::push(const Message &msg) {
    string json=xpack::json::encode(msg);
    if(msg.actId.length()>0){
        //发给指定session
        string id=msg.actId;
        auto it=find_if(sessions.begin(),sessions.end(),[id](SocketSession * s){
            return s->id==id && s->connected;
        });
        if(it==sessions.end()){
            //不存在
            loge("client[{}] not connected",id);
        }
        (*it)->send(json);
    }else{
        for(auto & item :sessions){
            if(item->connected == true)
                item->send(json);
        }
    }
}




