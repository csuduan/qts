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
#include <event2/bufferevent.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include<sys/un.h>
#include "UdsClient.h"
#include <functional>
#include "Util.h"
#include "Enums.h"
#include "Data.h"
using namespace  std;
using namespace std::placeholders;

void event_cb(struct bufferevent *bev, short events, void *arg){
    UdsClient* tcpClient=(UdsClient *) arg;
    tcpClient->event_callback(bev,events);
}
void write_cb(struct bufferevent *bev, void *arg){
    UdsClient* tcpClient=(UdsClient *) arg;
    tcpClient->write_callback(bev);
}
void read_cb(struct bufferevent *bev, void *arg){
    UdsClient* tcpClient=(UdsClient *) arg;
    tcpClient->read_callback(bev);
}

void usage(){
    cout<<"support cmd:"<<endl;
    cout<<"1. [START_ENG|STOP_ENG|CONNECT_MD|DISCOUNT_MD]"<<endl;
    cout<<"2. [CONNECT_ACT|DISCOUNT_ACT|PAUSE_OPEN|PAUSE_CLOSE] [accountId]"<<endl;

}


void read_terminal(evutil_socket_t fd, short what, void *arg)
{
    //UdsClient* udsClient=(UdsClient *) arg;
    struct bufferevent * connEv= (struct bufferevent *)arg;
    //读数据
    char buf[1024] = {0};
    int len = read(fd, buf, sizeof(buf));
    if(len==0){
        usage();
        return;
    }
    //命令行转义为json再发送
    string cmdline= string(buf);
    string msg= cmdline;
    //cout<<"==>"<<msg<<endl;
    int length=cmdline.length();
    char header[4+1]={0};
    sprintf(header,"%04d",length);

    //发送数据
    bufferevent_write(connEv, header, 4);
    bufferevent_write(connEv, msg.c_str(), length);
}

void UdsClient::start() {
    while (true){
        this->start_uds();
        cout<<"等待10s后重连..."<<endl;
        sleep(10);
    }


}
void UdsClient::start_uds() {
    // init server info
//    struct sockaddr_in serv;
//    memset(&serv, 0, sizeof(serv));
//    serv.sin_family = AF_INET;
//    serv.sin_port = htons(port);
//    serv.sin_addr.s_addr =inet_addr("127.0.0.1"); ;
//    evutil_socket_t fd;
//    fd = socket(AF_INET, SOCK_STREAM, 0);

    this->base = event_base_new();

    struct sockaddr_un serv;
    memset(&serv, 0, sizeof(serv));
    serv.sun_family = AF_UNIX;
    string filename="/tmp/sock/"+unName;
    //unlink(filename.c_str());
    strcpy(serv.sun_path,filename.c_str());

    evutil_socket_t fd;
    fd = socket(AF_UNIX, SOCK_STREAM, 0);

    //通信的fd放到bufferevent中
    struct bufferevent *connEv = bufferevent_socket_new(base, fd, BEV_OPT_CLOSE_ON_FREE);
    //int enable = 1;
    //if(setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, (void*)&enable, sizeof(enable)) < 0)
    //    printf("ERROR: TCP_NODELAY SETTING ERROR!\n");
    //设置回调
    bufferevent_setcb(connEv , read_cb, write_cb, event_cb, this);

    //设置回调生效
    bufferevent_enable(connEv, EV_READ);

    //连接服务器
    if(bufferevent_socket_connect(connEv, (struct sockaddr*)&serv, sizeof(serv))<0){
        perror("connect error");
    }

    //terminal事件
    struct event* terminalEv = event_new(base, STDIN_FILENO, EV_READ | EV_PERSIST,
                                         read_terminal, connEv);
    //添加事件
    event_add(terminalEv, NULL);
    //事件循环
    event_base_dispatch( this->base);
    cout<<"event base exit"<<endl;

    //释放事件
    event_free(terminalEv);
    bufferevent_free(connEv);
    event_base_free(base);
    cout<<"event base free"<<endl;
}

void  UdsClient::event_callback(struct bufferevent *bev, short events) {

    if (events & (BEV_EVENT_EOF|BEV_EVENT_ERROR))
    {
        cout << "connection error" <<  endl;
        //perror("connection error");
        event_base_loopbreak(base);
    }
    else if(events & BEV_EVENT_CONNECTED)
    {
        cout<<"connected\n";
        return ;
    }

}
void  UdsClient::read_callback(struct bufferevent *bev) {
    char buf[1024] = {0};
    bufferevent_read(bev, buf, sizeof(buf));
    cout << "<==" << buf << endl;
}

void  UdsClient::write_callback(struct bufferevent *bev) {
     //cout << "I'm 服务器，成功写数据给客户端，写缓冲回调函数被调用..." << endl;
}




