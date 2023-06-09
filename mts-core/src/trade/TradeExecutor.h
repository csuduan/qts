//
// Created by 段晴 on 2022/1/22.
//

#ifndef MTS_CORE_TRADEEXECUTOR_H
#define MTS_CORE_TRADEEXECUTOR_H


#include <atomic>
#include "Data.h"
#include "Message.h"
#include "gateway/Gateway.h"
#include "strategy/Strategy.h"
#include "LockFreeQueue.hpp"
#include "BarGenerator.hpp"
#include "common/SocketServer.h"
#include "SqliteHelper.hpp"
#include "MsgListener.h"
#include <limits>
#include <mutex>
#include <condition_variable>



class Strategy;
class TradeExecutor : public MsgListener{
private:
    string id;
    bool stopFlag=false;
    bool  configed =false;
    AcctConf acctConf;
    Acct* acct;
    vector<Quote*> quotes;
    TdGateway* tdGateway;
    //SocketClient* agentClient;
    SocketServer * udsServer;//对内提供服务


    map<string,vector<BarGenerator *>*> barGeneratorMap;
    map<string,MdGateway*> mdGatewayMap; //支持多路行情
    LockFreeQueue<Event> msgQueue ={1 << 20};//系统消息队列


    //合约订阅列表
    std::map<string, std::set<Strategy*>> subsMap;

    //tick信息
    std::map<string, Tick*> lastTickMap;
    //策略信息
    std::map<string, Strategy *> strategyMap;
    //报单-策略 映射map
    std::map<int,Strategy *> strategyOrderMap ;
    //成交信息
    std::map<string, Trade *> tradeMap;

    //报单队列(用于自成交校验)
    std::map<string,vector<Order *>> workingMap;
    std::vector<Order *> removeList;

    std::atomic<long> orderRefNum = 0;

    SqliteHelper * sqliteHelper =NULL;

    std::mutex mut;
    std::condition_variable cv;

    //Shm<MemTick>* shm=NULL;
public:
    TradeExecutor(string acctId);
    void init();
    void start();
    void connect();
    void disconnect();
    void subContract(set<string> contracts, Strategy * strategy);
    BarGenerator* getBarGenerator(string symbol,BAR_LEVEL level);
    void onTick(Tick *tick);
    void onOrder(Order *order);
    void onTrade(Trade *trade);
    void createStrategy(StrategySetting *setting);
    /// 报单
    /// \param order
    bool insertOrder(Order * order);
    ///报单
    void insertOrder(OrderReq * orderReq);
    /// 撤单
    /// \param orderRef
    void cancelorder(CancelReq & req);
    ///清理（非close）
    void clear();
    //连接行情
    void attachQuote(string name,int size);

    void fastEventHandler();
    void msgHandler();
    void reward(Message *msg);
    void processMessage(Message * msg);

    Message* onRequest(Message* request) override;

    ///优雅停机
    void shutdown();
    int signalHanler(int signo);
    AcctInfo getAcctInfo();

};


#endif //MTS_CORE_TRADEEXECUTOR_H
