//
// Created by 段晴 on 2022/5/3.
//

#ifndef MTS_CORE_MESSAGE_H
#define MTS_CORE_MESSAGE_H

#include "data.h"

struct OrderReq {
    string symbol;
    TRADE_TYPE offset;
    TRADE_DIRECTION direct;
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

struct QuoteInfo {
    string id;
    string type;
    string user;
    string pwd;
    string address;
    string subList;
    string subFile;
    bool enable;
    bool autoConnect;

    bool status;
    set<string> subSet;
XPACK(M(id, type, address, enable), O(user, pwd, subList, subFile, autoConnect));
};

struct AcctConf {
    string id;
    string owner;
    string group;
    string tdType;
    string tdAddress;
    string user;
    string pwd;
    string mdAddress;
    string mdType;
    string subList;
    string straFile;
    bool enable;
XPACK(M(id, group, tdType, tdAddress, mdType, mdAddress, enable),
      O(owner, user, pwd, subList, straFile));
};

struct AcctInfo {
    string id;
    string group;

    //状态
    bool status = true;//账户状态
    bool tdStatus = false;
    bool mdStatus = false;
    bool pauseOpen = false;
    bool pauseClose = false;

    //资金信息
    double mv = 0;
    double balanceProfit = 0;
    double marginRate = 0;//保证金占比
    double fee = 0;

    double preBalance = 0; // 昨日账户结算净值
    double balance = 0; // 账户净值
    double available = 0; // 可用资金
    double commission = 0; // 今日手续费
    double margin = 0; // 保证金占用
    double closeProfit = 0; // 平仓盈亏
    double positionProfit = 0; // 持仓盈亏
    double deposit = 0; // 入金
    double withdraw = 0; // 出金

XPACK(M(id, group, tdStatus, mdStatus, balance, available, commission, margin, closeProfit, positionProfit));

};

struct Message {
    string requestId;//请求编号
    string acctId;
    string type;//消息类型
    string data;//报文字符串
    MSG_TYPE msgType;
    xpack::JsonData jsonData;
XPACK(M(type), O(requestId, acctId, data));
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
