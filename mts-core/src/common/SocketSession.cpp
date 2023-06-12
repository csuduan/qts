////
//// Created by 段晴 on 2022/3/2.
////
//
//#include "SocketSession.h"
//#include <stdio.h>
//#include <iostream>
//#include <unistd.h>
//#include <cstring>
//#include "Data.h"
//#include "Message.h"
//
//bool SocketSession::send(const string & msg) {
//    int headLen=4;
//    int msgLen=msg.length();
//    int msgLenBig= htonl(msgLen);
//
//    char buffer[headLen+msgLen];
//    memset(buffer,0,headLen+msgLen);
//
//    memcpy(buffer,&msgLenBig,headLen);
//    memcpy(buffer+headLen,msg.c_str(),msgLen);
//    bufferevent_write(bev, buffer, headLen+msgLen);
//
//}
//
//
//void SocketSession::read_callback(struct bufferevent *bev) {
//    this->connected=true;
//    //采用4位长度+json字符串消息体
//    int headLen=4;
//    struct evbuffer *input = bufferevent_get_input(bev);
//    while(true){
//        //循环处理
//        size_t len = evbuffer_get_length(input);
//        if(len>=headLen){
//            unsigned char head[headLen];
//            bufferevent_read(bev, head, headLen);
//            unsigned int msgLen;
//            memcpy(&msgLen,head,headLen);
//            msgLen=ntohl(msgLen);//大端转小端
//            char msg[msgLen+1];
//            len = bufferevent_read(bev, msg, msgLen);
//            msg[len] = '\0';
//
//            try{
//                Message *message=new Message;
//                xpack::json::decode(msg, *message);
//                auto msgType=magic_enum::enum_cast<MSG_TYPE>(message->type);
//                if(msgType.has_value()){
//                    if(msgType== MSG_TYPE::PING){
//                        message->type= enum_string(MSG_TYPE::PING);
//                        string rsp=xpack::json::encode(*message);
//                        this->send(rsp);
//                    }else{
//                        message->msgType=msgType.value();
//                        //this->queue->push(Event{EvType::MSG,0,message});
//                        Message* rsp = listener->onRequest(message);
//                        //todo 同步处理
//                    }
//                }else{
//                    loge("unknow msgType:{}",message->type);
//                }
//                logi("session[{}] recv  msg: {}",this->id, string (msg));
//            }catch(std::exception ex){
//                loge("valid json message:{}",string (msg));
//            }
//        }else{
//            break;
//        }
//    }
//}
//
//void SocketSession::write_callback(struct bufferevent *bev) {
//}
//
//void SocketSession::event_callback(struct bufferevent *bev, short events) {
//    if (events & (BEV_EVENT_EOF | BEV_EVENT_ERROR))
//    {
//        this->connected= false;
//        loge("connection [{}] closed !" ,this->id);
//    }
//    else if(events & BEV_EVENT_CONNECTED)
//    {
//        this->connected=true;
//        logi( "connection [{}] connected !",this->id);
//    }else{
//        logw("unkonw event:{}",events);
//    }
//
//    bufferevent_free(bev);
//}
