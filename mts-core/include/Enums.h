#pragma once
#include <set>
using std::set;
using std::string;
using std::map;
//头寸方向
enum  POS_DIRECTION {
    LONG,
    SHORT,
    NET,
};

static map<string,POS_DIRECTION> POS_DIRECTION_MAP={
        {"LONG",POS_DIRECTION::LONG},
        {"SHORT",POS_DIRECTION::SHORT},
        {"NET",POS_DIRECTION::NET},
};

//交易方向
enum TRADE_DIRECTION{
    BUY,
    SELL
};

static map<string,TRADE_DIRECTION> TRADE_DIRECTION_MAP={
        {"BUY",BUY},
        {"SELL",SELL},
};


//交易类型
enum OFFSET{
    OPEN,
    CLOSE,
    CLOSETD,
    CLOSEYD
};

static map<string,OFFSET> OFFSET_MAP={
        {"OPEN",OPEN},
        {"CLOSE",CLOSE},
        {"CLOSETD",CLOSETD},
        {"CLOSEYD",CLOSEYD},

};


//BAR级别
enum BAR_LEVEL{
    M1,
    M5,
    M10,
    D1
};
//网关类型
enum GATEWAY_TYPE{
    CTP,
    OST,
    SIM,
    REM
};
//报单类型
enum ORDER_TYPE {
    NOR,
    FAK,
    FOK

};
//价格类型
enum PRICE_TYPE{
    LIMIT,
    MARKET
};
//状态
enum ORDER_STATUS{
    UNKNOWN, //未知
    QUEUEING,//还在队列(部分成交或未成交)
    NOTQUEUEING ,//不在队列(部分成交或未成交)
    ALLTRADED ,//全部成交
    CANCELLED ,//已撤单
    ERROR//错单
};


static set<ORDER_STATUS> STATUS_FINISHED ={
        ORDER_STATUS::ALLTRADED,
        ORDER_STATUS::NOTQUEUEING,
        ORDER_STATUS::CANCELLED,
        ORDER_STATUS::ERROR
};

template<typename T>
typename std::underlying_type<T>::type PrintEnum(T const value) {
    return static_cast<typename std::underlying_type<T>::type>(value);
}
