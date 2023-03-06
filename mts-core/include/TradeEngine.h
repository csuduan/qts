//
// Created by 段晴 on 2022/1/22.
//

#ifndef MTS_CORE_TRADEENGINE_H
#define MTS_CORE_TRADEENGINE_H


#include "Data.h"
#include "Strategy.h"
#include "TradeExecutor.h"
#include "Gateway.h"
#include "LockFreeQueue.h"


class TradeEngine {
private:
    string engineId;
    map<string,TradeExecutor*> tradeExecutorMap;
    MdGateway *mdGateway;
    LockFreeQueue<Event>* queue;

public:
    TradeEngine(std::string engineId);
    void start();
    void init();
    void close();

    /**
     * 创建行情
     * @param account
     */
    void createMd(MdInfo mdInfo);
    /**
     * 创建账户交易执行器
     * @param account
     */
    void createTradeExecutor(Account account);
    /**
     * 创建策略
     * @param strategySetting
     */
    void createStrategy(StrategySetting strategySetting);
    /**
     * 连接账户(交易接口)
     * @param accountId
     */
    void connectAccount(std::string accountId);
    /**
     * 断开账户(交易接口)
     * @param accoundId
     */
    void disconnectAccount(std::string accoundId);
    /**
     * 连接行情
     */
    void connectMd();
    /**
     * 断开行情
     */
    void disconnectMd();

    void eventHanle();


};


#endif //MTS_CORE_TRADEENGINE_H
