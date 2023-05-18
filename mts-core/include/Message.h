//
// Created by 段晴 on 2022/5/3.
//

#ifndef MTS_CORE_MESSAGE_H
#define MTS_CORE_MESSAGE_H

#include "Data.h"

struct OrderReq{
    string symbol;
    string offset;
    string direct;
    double price;
    int volume;
    XPACK(M(symbol,offset,direct,price, volume));
};

struct ConnectReq{
    int type;//1-md,2-td,3-all
    bool status;
    XPACK(M(type,status));
};


struct CancelReq{
    int orderRef;
    int frontId;
    long long sessionId;
    XPACK(M(orderRef));
};



struct Message{
    int seq=0;//序号
    string   rid;//接收对象
    string   sid;
    string   type;//消息类型
    xpack::JsonData   data;//报文字符串
    MSG_TYPE msgType;
    XPACK(M(seq, type), O(rid,sid, data));
};

template<class T>
struct MessageS{
    int seq=0;//序号
    string   rid;//接收对象
    string   sid;
    string   type;//消息类型
    T   data;//报文字符串
XPACK(M(seq, type), O(rid,sid, data));
};



struct CommReq{
    string param;
    XPACK(O(param));
};

#endif //MTS_CORE_MESSAGE_H
