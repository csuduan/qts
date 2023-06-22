#pragma once
#include "data.h"
#include <functional>
#include <cmath>

/**
 * 取值[09:00-10:14] [10:30-11:29] [13:30-15:00]   [21:00-]
 * 小节结束时刻如10:15:00,11:30:00,15:00:00等的数据不单独生成一个bar，暂时丢弃
 */

class BarGenerator{
private:
    Tick * lastTick = nullptr;
    Bar  * bar = nullptr;
    function<void(Bar *)> callBack;
    int toBarTime(int tm,int level){
        int minute= tm/100;
        int hour=minute/100;
        int min=minute%100;
        int barMinute=hour*60+min;
        barMinute=(int)(barMinute/level)*level;
        int barTime=((int)(barMinute/60)*100+barMinute%60)*100;
        return barTime;
    }
public:
    string symbol;
    BAR_LEVEL level;
    int offset;
    vector<Bar *> history;

    BarGenerator(string symbol,BAR_LEVEL level,int offset=0,function<void(Bar *)> callBack= nullptr):symbol(symbol),level(level),offset(offset),callBack(callBack){
        logi("new BarGenerator {} {}",symbol,level);
    }

    void onTick(Tick * tick){
        //计算tick对应barTime
        int barTime= toBarTime(tick->updateTime,level);
        bool newMinute= false;
        if(bar== nullptr){
            bar =new Bar();
            newMinute=true;
        }else if(bar->barTime!=barTime){
            logi("end bar:{} {} {} tm:{}",bar->symbol,bar->level,bar->barTime,bar->updateTime);
            if(bar->tickCount>=4){
                //tickCount>=4的bar才保留
                if(callBack!= nullptr)
                    callBack(bar);
                history.push_back(bar);
            }
            bar=new Bar();
            newMinute=true;
        }
        if(newMinute){
            bar->symbol=this->symbol;
            bar->level=this->level;
            bar->barTime=barTime;

            bar->tradingDay=tick->tradingDay;
            bar->actionDay=tick->actionDay;
            bar->open=tick->lastPrice;
            bar->high=tick->lastPrice;
            bar->low=tick->lastPrice;
            bar->close=tick->lastPrice;
            bar->updateTime=tick->updateTime;
            bar->openInterest=tick->openInterest;
            logi("start bar:{} {} {} tm:{}",bar->symbol,bar->level,bar->barTime,bar->updateTime);
        }else{
            bar->high=max(bar->high, tick->lastPrice);
            bar->low=min(bar->low, tick->lastPrice);
            bar->close=tick->lastPrice;
        }
        bar->tickCount++;

        if(lastTick!= nullptr){
            bar->volume +=tick->volume=lastTick->volume;
        }
        lastTick=tick;
    }
//    bool operator==(const BarGenerator &bg){
//        return this->symbol==bg.symbol && this->level ==bg.level;
//    }
};