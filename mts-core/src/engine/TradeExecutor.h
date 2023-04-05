//
// Created by 段晴 on 2022/1/22.
//

#ifndef MTS_CORE_TRADEEXECUTOR_H
#define MTS_CORE_TRADEEXECUTOR_H


#include <atomic>
#include "Data.h"
#include "gateway/Gateway.h"
#include "strategy/Strategy.h"
#include "LockFreeQueue.hpp"
#include "message.h"
#include "BarGenerator.hpp"
#include "util/UdsServer.h"


class Strategy;
class TradeExecutor {
private:
    string id;
    Account* account;
    TdGateway* tdGateway;
    UdsServer* server;

    map<string,BarGenerator *> barGeneratorMap;
    MdGateway *mdGateway;
    std::queue<msg::Message>  msgQueue;//消息队列

    //合约订阅列表
    std::map<string, std::set<Strategy*>> subsMap;

    //tick信息
    std::map<string, Tick*> lastTickMap;
    //策略信息
    std::map<string, Strategy *> strategyMap;
    //报单信息
    std::map<string, Order *> workingOrderMap ;
    //报单-策略 映射map
    std::map<string,Strategy *> strategyOrderMap ;
    //成交信息
    std::map<string, Trade *> tradeMap;

    std::vector<Order *> removeList;

    std::atomic<long> orderRefNum = 0;
public:
    TradeExecutor(string acctId);
    void init();
    void start();
    void connect();
    void disconnect();
    void subContract(set<string> contracts, Strategy * strategy);
    void onTick(Tick *tick);
    void onOrder(Order *order);
    void createStrategy(StrategySetting *setting);
    /// 报单
    /// \param order
    void insertOrder(Order * order);
    ///报单
    void insertOrder(msg::OrderReq * orderReq);
    /// 撤单
    /// \param orderRef
    void cancelorder(string orderRef);
    ///清理（非close）
    void clear();

    [[noreturn]] void fastEventHandler();
    void msgHandler();
    void clearWork();
    void addMsgQueue(msg::Message msg);

};


#endif //MTS_CORE_TRADEEXECUTOR_H
