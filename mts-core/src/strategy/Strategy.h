#pragma once

#include <string>
#include "Data.h"
#include "define.h"
#include "engine/TradeExecutor.h"
class TradeExecutor;
class Strategy
{
protected:
    std::string strategyId;
    bool pauseOpen = false; //暂停开仓
    bool pauseClose = false;//暂停平仓
    StrategySetting* setting;
    TradeExecutor * tradeExecutor;
    map<string,AcctPosition *> posMap;
    void open();
    void closeTd();
    void closeYd();
    void close();
    void cancel();


public:
     virtual void init(TradeExecutor *tradeExecutor,StrategySetting *setting) {
         this->setting=setting;
         this->strategyId=setting->strategyId;
         this->tradeExecutor=tradeExecutor;
         //根据合约创建仓位
         for (const auto &item : this->setting->contracts){
             AcctPosition * longPos=new AcctPosition(item, POS_DIRECTION::LONG);
             AcctPosition * shortPos=new AcctPosition(item, POS_DIRECTION::SHORT);
             posMap[longPos->positionId] = longPos;
             posMap[shortPos->positionId] = shortPos;
         }

     };
     virtual void onTick(Tick * tick){
         //logi("base onTick:{}",tick->symbol);
     };
     virtual void onBar(Bar * bar) {};
     virtual void onOrder(Order * order){
         //更新持仓
        this->updatePosition(order);
     };
     virtual void onTrade(Trade * trade) {};

private:
     void updatePosition(Order * order){
         AcctPosition* position=posMap[order->positionId];
         if(order->finished){
             //刷新持仓
             position->onway=0;
             if(order->tradedVolume==0)
                 return;
             if(order->offset == OFFSET::OPEN){
                 position->tdPos+=order->tradedVolume;
             } else if(order->offset == OFFSET::CLOSETD){
                 position->tdPos-=order->tradedVolume;
             }else {
                 //优先减昨仓
                 if(position->ydPos>=order->tradedVolume)
                     position->ydPos -= order->tradedVolume;
                 else{
                     int tradedTdPos=order->tradedVolume-position->ydPos;
                     position->ydPos=0;
                     position->tdPos -=tradedTdPos;
                 }
             }
             logi("{} 更新持仓 symbol:{} direction:{} tdPos:{} ydPos:{}",strategyId,position->symbol,
                  position->direction,position->tdPos,position->ydPos);

         }else{
             //刷新在途
             if(order->offset == OFFSET::OPEN)
                 position->onway=order->totalVolume;
             else
                 position->onway=0-order->totalVolume;
         }
     };
     void order(string symbol,OFFSET offset,POS_DIRECTION posDirection,ORDER_TYPE orderType,double  price,int volume);

};
