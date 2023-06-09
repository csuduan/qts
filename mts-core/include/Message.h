//
// Created by 段晴 on 2022/5/3.
//

#ifndef MTS_CORE_MESSAGE_H
#define MTS_CORE_MESSAGE_H

#include "Data.h"

struct OrderReq {
    string symbol;
    string offset;
    string direct;
    double price;
    int volume;
XPACK(M(symbol, offset, direct, price, volume));
};

struct ConnectReq {
    bool status;
    XPACK(M(status));
};


struct CancelReq {
    int orderRef;
    int frontId;
    long long sessionId;
XPACK(M(orderRef));
};

struct QuoteConf {
    string id;
    string type;
    string user;
    string pwd;
    string address;
    string subList;
XPACK(M(id,type,address),O(user,pwd,subList));

};
struct AcctConf {
    string id;
    string owner;
    string group;
    string tdAddress;
    string user;
    string pwd;
    bool enable;
    vector<QuoteConf> quoteConfs;
XPACK(M(id,owner,group,tdAddress,enable),O(user,pwd,quoteConfs));

};

struct AcctInfo {
    string id;
    bool tdStatus = false;
    bool mdStatus = false;

    //资金信息
    double balance=0;
    double mv=0;
    double balanceProfit=0;
    double closeProfit=0;
    double margin=0;//保证金
    double marginRate=0;//保证金占比
    double fee=0;

XPACK(M(id,tdStatus,mdStatus));

};


struct Message {
    string requestId;//请求编号
    string acctId;
    string type;//消息类型
    bool success;
    string data;//报文字符串
    MSG_TYPE msgType;
XPACK(M(type), O(requestId, acctId, data, success));
};

template<class T>
struct MessageS {
    int seq = 0;//序号
    string rid;//接收对象
    string sid;
    string type;//消息类型
    T data;//报文字符串
XPACK(M(seq, type), O(rid, sid, data));
};


struct CommReq {
    string param;
XPACK(O(param));
};

#endif //MTS_CORE_MESSAGE_H
