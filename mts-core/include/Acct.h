//
// Created by 段晴 on 2022/5/12.
//

#ifndef MTS_CORE_ACCT_H
#define MTS_CORE_ACCT_H
#include "Data.h"
#include "Message.h"

class Acct{
public:
    AcctConf *acctConf;
    AcctInfo *acctInfo;
    string id;
    string name;
    int cpuNumTd;
    int cpuNumEvent;
    bool autoConnect;

    LockFreeQueue<Event> * tdQueue;
    LockFreeQueue<Event> * mdQueue;


    //账户持仓(用于校验)
    map<string, Position *> accoPositionMap;
    //合约信息
    map<string, Contract *> contractMap;
    //报单信息
    map<int, Order *> orderMap;

    Position * getPosition(string symbol,POS_DIRECTION direction){
        string key = symbol + "-" + std::to_string(direction);
        if (!accoPositionMap.count(key)>0) {
            accoPositionMap[key] = new Position(symbol, direction);
        }
        return accoPositionMap[key];
    }
};
#endif //MTS_CORE_ACCT_H
