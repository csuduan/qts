//
// Created by 段晴 on 2022/5/12.
//

#ifndef MTS_CORE_ACCT_H
#define MTS_CORE_ACCT_H

#include <atomic>
#include "data.h"
#include "message.h"
#include "gateway/gateway.h"

class Acct{
public:
    AcctConf *acctConf;
    AcctInfo *acctInfo;
    string id;
    string name;
    int cpuNumTd;
    int cpuNumEvent;
    bool autoConnect;

    std::atomic<long> orderRefNum = 0;


    LockFreeQueue<Event> * fastQueue;  //急速队列
    LockFreeQueue<Event> * msgQueue; //一般队列


    TdGateway *tdGateway;
    MdGateway *mdGateway;

    //所有合约信息
    map<string, Contract *> contractMap;

    //tick信息
    std::map<string, Tick *> lastTickMap;

    //持仓列表
    map<string, Position *> accoPositionMap;
    //成交列表
    std::map<string, Trade *> tradeMap;
    //挂单列表(用于检索)
    map<int, Order *> orderMap;
    //挂单队列(用于自成交校验)
    std::map<string, vector<Order *>> workingMap;


    inline Position * getPosition(string symbol,POS_DIRECTION direction){
        string key = symbol + "-" + (string)enum_string(direction);
        if (accoPositionMap.count(key)==0) {
            accoPositionMap[key] = new Position(symbol, direction);
        }
        return accoPositionMap[key];
    }

    void onTrade(Trade *trade);

    void onOrder(Order * order);

    void onPosition(Position * position);


    void connect(){
        this->tdGateway->connect();
        this->mdGateway->connect();
    }

    void disconnect(){
        this->tdGateway->disconnect();
        this->mdGateway->disconnect();
    }

    bool  init();

    void subscribe(set<string> &contracts){
        this->mdGateway->subscribe(contracts);
    }

    //交易相关API
    /// 报单
    /// \param order
    bool insertOrder(Order *order);

    ///报单
    void insertOrder(OrderReq *orderReq);

    /// 撤单
    /// \param orderRef
    void cancelorder(CancelReq &req);

};
#endif //MTS_CORE_ACCT_H
