//
// Created by 段晴 on 2022/2/8.
//
#include "strategy.h"

void Strategy::open() {

}

void Strategy::closeTd() {

}

void Strategy::closeYd() {

}

void Strategy::close() {

}

void Strategy::cancel() {

}

inline TRADE_DIRECTION getTradeDirection(OFFSET offset,POS_DIRECTION posDirection){
    if (posDirection== POS_DIRECTION::LONG)
        return offset== OFFSET::OPEN?TRADE_DIRECTION::BUY:TRADE_DIRECTION::SELL;
    else
        return offset== OFFSET::OPEN?TRADE_DIRECTION::SELL:TRADE_DIRECTION::BUY;
}

void Strategy::order(string symbol, OFFSET offset, POS_DIRECTION posDirection, ORDER_TYPE orderType, double price,
                     int volume) {
    string posId=symbol+"-"+ to_string(static_cast<int>(posDirection));
    auto pos=posMap[posId];
    if(pos->onway>0){
        logw("{} 仓位{} 存在在途交易",strategyId,pos->positionId);
        return;
    }
    Order* order=new Order;
    order->symbol=symbol;
    order->offset=offset;
    order->direction= getTradeDirection(offset,posDirection);
    order->orderType = orderType;
    order->price=price;
    order->totalVolume=volume;
    order->positionId=posId;
    this->updatePosition(order);
    this->tradeExecutor->insertOrder(order);
}
