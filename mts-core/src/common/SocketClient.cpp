//
// Created by Administrator on 2020/6/15.
//
#include <iostream>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <string.h>
#include <fcntl.h>
#include <event2/event.h>
#include <event.h>
#include <event2/bufferevent.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include<sys/un.h>
#include "SocketClient.h"
#include <functional>
#include "Util.h"
#include "Enums.h"
#include "Data.h"
using namespace  std;
using namespace std::placeholders;

void event_cb(struct bufferevent *bev, short events, void *arg){
    SocketClient* tcpClient=(SocketClient *) arg;
    tcpClient->event_callback(bev,events);
}
void write_cb(struct bufferevent *bev, void *arg){
    SocketClient* tcpClient=(SocketClient *) arg;
    tcpClient->write_callback(bev);
}
void read_cb(struct bufferevent *bev, void *arg){
    SocketClient* tcpClient=(SocketClient *) arg;
    tcpClient->read_callback(bev);
}

void usage(){
    cout<<"support cmd:"<<endl;
    cout<<"1. [START_ENG|STOP_ENG|CONNECT_MD|DISCOUNT_MD]"<<endl;
    cout<<"2. [CONNECT_ACT|DISCOUNT_ACT|PAUSE_OPEN|PAUSE_CLOSE] [accountId]"<<endl;

}

//
//void read_terminal(evutil_socket_t fd, short what, void *arg)
//{
//    //UdsClient* udsClient=(UdsClient *) arg;
//    struct bufferevent * connEv= (struct bufferevent *)arg;
//    //读数据
//    char buf[1024] = {0};
//    int len = read(fd, buf, sizeof(buf));
//    if(len==0){
//        usage();
//        return;
//    }
//    //命令行转义为json再发送
//    string cmdline= string(buf);
//    string msg= cmdline;
//    //cout<<"==>"<<msg<<endl;
//    int length=cmdline.length();
//    char header[4+1]={0};
//    sprintf(header,"%04d",length);
//
//    //发送数据
//    bufferevent_write(connEv, header, 4);
//    bufferevent_write(connEv, msg.c_str(), length);
//}

void SocketClient::start() {
    this->base = event_base_new();
    while (true){
        connect();
        loge("wait 10s for connecting...");
        sleep(10);
    }
}



void  SocketClient::event_callback(struct bufferevent *bev, short events) {
    if (events & (BEV_EVENT_EOF|BEV_EVENT_ERROR))
    {
        if(this->connected==true){
            loge ("connect {} error !!!",this->socketAddr.name );
            this->connected= false;
        }
        event_base_loopbreak(base);
    }
    else if(events & BEV_EVENT_CONNECTED)
    {
        logi("connect {} success!!!",this->socketAddr.name);
        this->connected = true;
        //发送一个PING包
        Message message={0};
        message.actId=this->socketAddr.name;
        message.type= enum_string(MSG_TYPE::PING);
        request(message);
        return ;
    }

}
void  SocketClient::read_callback(struct bufferevent *bev) {
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
                Message *message=new Message();
                xpack::json::decode(msg, *message);
                logi("recv msg: {} {}", message->type,message->data);
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

void  SocketClient::write_callback(struct bufferevent *bev) {
     //cout << "I'm 服务器，成功写数据给客户端，写缓冲回调函数被调用..." << endl;
}



void SocketClient::connect() {
    if(socketAddr.type== SocketType::UDS){
        struct sockaddr_un serv;
        memset(&serv, 0, sizeof(serv));
        serv.sun_family = AF_UNIX;
        string filename="/tmp/ipc/"+socketAddr.unName;
        strcpy(serv.sun_path,filename.c_str());

        evutil_socket_t fd;
        fd = socket(AF_UNIX, SOCK_STREAM, 0);
        connEv = bufferevent_socket_new(base, fd, BEV_OPT_CLOSE_ON_FREE);
        //设置回调
        bufferevent_setcb(connEv , read_cb, write_cb, event_cb, this);
        //设置回调生效
        bufferevent_enable(connEv, EV_READ);
        //连接服务器
        if(bufferevent_socket_connect(connEv, (struct sockaddr*)&serv, sizeof(serv))<0){
            //perror("connect error");
            loge("connect server fail!");
            return;
        }

    }else{
        struct sockaddr_in in;
        memset(&in, 0, sizeof(in));
        in.sin_family = AF_INET;
        in.sin_port = htons(socketAddr.port);
        in.sin_addr.s_addr =inet_addr("127.0.0.1"); ;
        evutil_socket_t fd;
        fd = socket(AF_INET, SOCK_STREAM, 0);
        connEv = bufferevent_socket_new(base, fd, BEV_OPT_CLOSE_ON_FREE);
        //设置回调
        bufferevent_setcb(connEv , read_cb, write_cb, event_cb, this);
        //设置回调生效
        bufferevent_enable(connEv, EV_READ);
        //连接服务器
        if(bufferevent_socket_connect(connEv, (struct sockaddr*)&in, sizeof(in))<0){
            //perror("connect error");
            loge("connect server fail!");
            return;
        }

    }

    //int enable = 1;
    //if(setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, (void*)&enable, sizeof(enable)) < 0)
    //    printf("ERROR: TCP_NODELAY SETTING ERROR!\n");


//    //terminal事件
//    struct event* terminalEv = event_new(base, STDIN_FILENO, EV_READ | EV_PERSIST,
//                                         read_terminal, connEv);
//    //添加事件
//    event_add(terminalEv, NULL);
    //事件循环
    event_base_dispatch( this->base);
    //logw("event base exit");
    //释放事件
    //event_free(terminalEv);
    bufferevent_free(connEv);
    //event_base_free(base);
    //logw("event conn free");
}

void SocketClient::request(const string &msg) {
    if(!this->connected){
        loge("connection[{}] is closed!",this->socketAddr.name);
        return;
    }
    int headLen=4;
    int msgLen=msg.length();
    unsigned char head[headLen];
    memcpy(head,&msgLen,headLen);

    char buffer[headLen+msgLen];
    memset(buffer,0,headLen+msgLen);
    memcpy(buffer,head,headLen);
    memcpy(buffer+headLen,msg.c_str(),msgLen);
    //reverse(head,headLen);//小端转大端
    bufferevent_write(connEv, buffer, headLen+msgLen);
}

void SocketClient::request(const Message &msg) {
    string json=xpack::json::encode(msg);
    logi("client[{}]send msg:{}",this->socketAddr.name,json);
    this->request(json);
}





