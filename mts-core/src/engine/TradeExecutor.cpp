//
// Created by 段晴 on 2022/1/22.
//

#include "TradeExecutor.h"
#include "GatewayFactory.h"

TradeExecutor::TradeExecutor(Account &account,LockFreeQueue<Event> * queue):account(&account) ,queue(queue){
    this->ctpTdApi=GatewayFactory::createTdGateway(account,queue);
}
void TradeExecutor::subContract(string contract, Strategy strategy) {

}

