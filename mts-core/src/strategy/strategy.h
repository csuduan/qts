#pragma once

#include <string>
#include "data.h"
#include "define.h"
#include "trade/acct.h"
class Strategy
{
protected:
    std::string strategyId;
    bool pauseOpen = false; //暂停开仓
    bool pauseClose = false;//暂停平仓
    StrategySetting* setting;
    Acct * acct;
    map<string,Position *> posMap;
    void open();
    void closeTd();
    void closeYd();
    void close();
    void cancel();


public:
     virtual void init(Acct* acct, StrategySetting *setting) {
         this->setting=setting;
         this->strategyId=setting->strategyId;
         this->acct=acct;
         //根据合约创建仓位
         if (!setting->trgSymbol.empty()){
             Position * longPos=new Position(setting->trgSymbol, POS_DIRECTION::LONG);
             Position * shortPos=new Position(setting->trgSymbol, POS_DIRECTION::SHORT);
             posMap[longPos->id] = longPos;
             posMap[shortPos->id] = shortPos;
         }
         if (!setting->refSymbol.empty()){
             Position * longPos=new Position(setting->refSymbol, POS_DIRECTION::LONG);
             Position * shortPos=new Position(setting->refSymbol, POS_DIRECTION::SHORT);
             posMap[longPos->id] = longPos;
             posMap[shortPos->id] = shortPos;
         }

     };
     virtual void onTick(Tick * tick){
         //logi("base onTick:{}",tick->symbol);
     };
     virtual void onOrder(Order * order){
         //更新持仓
        this->updatePosition(order);
     };
     virtual void onTrade(Trade * trade) {};

     StrategySetting* getSetting(){
         return this->setting;
     }

private:
     void updatePosition(Order * order){
         Position* position=posMap[order->positionId];
         if(order->finished){
             //刷新持仓
             position->onway=0;
             if(order->tradedVolume==0)
                 return;
             if(order->offset == TRADE_TYPE::OPEN){
                 position->tdPos+=order->tradedVolume;
             } else if(order->offset == TRADE_TYPE::CLOSETD){
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
             logi("{} 更新持仓 symbol:{} direction:{} tdPos:{} ydPos:{}", strategyId, position->symbol,
                  position->direction, position->tdPos, position->ydPos);

         }else{
             //刷新在途
             if(order->offset == TRADE_TYPE::OPEN)
                 position->onway=order->totalVolume;
             else
                 position->onway=0-order->totalVolume;
         }
     };
     void order(string symbol, TRADE_TYPE offset, POS_DIRECTION posDirection, ORDER_TYPE orderType, double  price, int volume);

};
