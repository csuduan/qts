//
// Created by 段晴 on 2022/5/23.
//
#include "acct.h"
#include "gateway/gatewayFactory.hpp"
#include "dataBuilder.h"


bool Acct::init() {

    //创建Gateway
    this->tdGateway = GatewayFactory::createTdGateway(this);
    this->mdGateway = GatewayFactory::createMdGateway(this);

    //初始化报单序号
    time_t now = time(nullptr);
    auto tt = localtime(&now);
    int totolSecs = tt->tm_hour * 3600 + tt->tm_min * 60 + tt->tm_sec;
    this->orderRefNum = totolSecs * 1e4; //5位秒数+4位0
    logi("start orderRef:{}", this->orderRefNum);
    return true;
}


bool Acct::insertOrder(Order *order) {
    if (!this->tdGateway->isConnected()) {
        order->status = ORDER_STATUS::ERROR;
        order->statusMsg = "交易已断开";
        loge("td not connected !!!");
        return false;
    }
    //生成ordref;
    try {
        order->orderRef = this->orderRefNum++;
        auto contract = this->contractMap[order->symbol];
        if (contract == nullptr) {
            loge("{},insertOrder fial,can not find contract info[{}]", this->id, order->symbol);
            return false;
        }
        order->exchange = contract->exchange;
        auto lastTick = this->lastTickMap[order->symbol];
        if (order->price == 0 && lastTick != nullptr) {
            order->price = order->direction == TRADE_DIRECTION::BUY ? lastTick->askPrice1 : lastTick->bidPrice1;
        }
        if (order->price == 0 && lastTick != nullptr) {
            order->price = lastTick->lastPrice;
        }
        order->offsetStr = enum_string(order->offset);
        order->directionStr = enum_string(order->direction);
        //账户持仓量检查
        if (order->offset != OPEN) {
            auto position = this->getPosition(order->symbol, order->getPosDirection());
            if (position->pos < order->totalVolume) {
                //持仓量不足
                order->status = ORDER_STATUS::ERROR;
                order->statusMsg = "持仓不足";
                loge("Order {} check fail,positon not enough", order->orderRef);
                return false;
            }
        }
        //自成交检查
        auto vec = workingMap[order->symbol];
        if (vec.size() > 0) {
            auto it = find_if(vec.begin(), vec.end(), [order](Order *existOrder) {
                //存在交易方向相反，且未结束的报单
                return order->direction != existOrder->direction && !existOrder->finished;
            });
            if (it != vec.end()) {
                order->status = ORDER_STATUS::ERROR;
                order->statusMsg = "自成交风险";
                loge("Order {} check fail, exist self trading with:{}", order->orderRef, (*it)->orderRef);
                return false;
            }
        }
        //插入报单队列中
        this->orderMap[order->orderRef] = order;
        //开始报单
        bool ret = this->tdGateway->insertOrder(order);
        if (ret) {
            vec.push_back(order);
        }
        this->onOrder(order);

    } catch (exception ex) {
        loge("{} insert order err,{}", this->id, ex.what());
    }

}


void Acct::insertOrder(OrderReq *orderReq) {
    Order *order = new Order();
    order->symbol = orderReq->symbol;
    order->direction = orderReq->direct;
    order->offset = orderReq->offset;
    order->price = orderReq->price;
    order->totalVolume = orderReq->volume;
    this->insertOrder(order);
}

void Acct::cancelorder(CancelReq &req) {
    if (!this->tdGateway->isConnected()) {
        loge("td not connected !!!");
        return;
    }
    Action action = {0};
    action.orderRef = req.orderRef;
    action.sessionId = req.sessionId;
    action.frontId = req.frontId;
    this->tdGateway->cancelOrder(&action);
}

void Acct::onTrade(Trade *trade) {
    //更新持仓
    Position *position = this->getPosition(trade->symbol, trade->getPosDirection());
    if (trade->tradeType == OPEN)
        position->tdPos += trade->tradedVolume;
    else {
        if (trade->tradeType == CLOSETD)
            position->tdPos -= trade->tradedVolume;
        else {
            //平仓和平昨，都优先平昨
            if (position->ydPos >= trade->tradedVolume)
                position->ydPos -= trade->tradedVolume;
            else {
                position->tdPos -= trade->tradedVolume - position->ydPos;
                position->ydPos = 0;
            }
        }
    }

    //更新成交列表
    this->tradeMap[trade->tradeId] = trade;
    auto msg = buildMsg(MSG_TYPE::ON_TRADE, *trade, this->id);
    this->msgQueue->push(Event{EvType::MSG, 0, msg});

    //更新报单队列
    if(this->orderMap.count(trade->orderRef)>0)
        this->onOrder(this->orderMap[trade->orderRef]);

}

void Acct::onOrder(Order* order) {
    if (STATUS_FINISHED.count(order->status) > 0 && order->tradedVolume == order->realTradedVolume)
        order->finished = true;
    //auto startegy = this->strategyOrderMap[order->orderRef];
    auto msg = buildMsg(MSG_TYPE::ON_ORDER, *order, this->id);
    this->msgQueue->push(Event{EvType::MSG, 0, msg});

    if (order->finished) {
        //清除完结的对象
        this->orderMap.erase(order->orderRef);
        auto vec = this->workingMap[order->symbol];
        std::remove_if(vec.begin(), vec.end(), [order](Order *existOrder) {
            return order->orderRef == existOrder->orderRef;
        });
        delete order;
    }


}

void Acct::onPosition(Position *position) {

}




