//
// Created by 段晴 on 2022/1/22.
//

#ifndef MTS_CORE_TRADEEXECUTOR_H
#define MTS_CORE_TRADEEXECUTOR_H


#include "Data.h"
#include "Gateway.h"
#include "Strategy.h"
#include "LockFreeQueue.h"

class TradeExecutor {
private:
    Account* account;
    TdGateway* ctpTdApi;
    LockFreeQueue<Event> * queue;
    //合约订阅列表
    std::map<string, std::set<string>> subsMap;
    //合约信息
    std::map<string, Contract> contractMap;
    //tick信息
    std::map<string, Tick> lastTickMap;
    //策略信息
    std::map<string, Strategy> stringStrategyMap;
    //报单信息
    std::map<string, Order> workingOrderMap ;
    //报单-策略 映射map
    std::map<string,Strategy> strategyOrderMap ;
    //成交信息
    std::map<string, Trade> tradeMap;
public:
    TradeExecutor(Account & account,LockFreeQueue<Event> * queue);
    void subContract(string contract, Strategy strategy);

};


#endif //MTS_CORE_TRADEEXECUTOR_H
