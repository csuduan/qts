#pragma once
#include <set>
#include <map>
#include "magic_enum.hpp"
using std::set;
using std::string;
using std::map;

#define enum_string(A) magic_enum::enum_name(A)


enum EvType {
    TICK,
    ORDER,
    TRADE,
    POSITON,
    CONTRACT,
    MSG,
};

//交易所
enum EXCHANGE{
    SSE,//上证
    SZE,//深圳
    SHFE,//上期
    CFFEX,//中金
    DCE,//大商
    CZCE,//郑商
    INE,//能源
    HK//港交
};

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
    M1=1,
    M5=5,
    M15=15,
    H1=60
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

enum MSG_TYPE{
    //请求消息
    PING,
    EXIT,
    RESTART,
    CONNECT,

    MD_SUBS,
    ACT_PAUSE_OPEN,
    ACT_PAUSE_CLOSE,
    ACT_ORDER,
    ACT_CANCEL,
    ACT_QRY_TRADE,
    ACT_QRY_ORDER,
    ACT_QRY_POSITION,
    ACT_QRY_ACCOUNT,


    //推送消息
    ON_STATUS=100,
    ON_LOG,
    ON_POSITION,
    ON_TRADE,
    ON_ORDER,
    ON_CONTRACT,
    ON_BAR,
    ON_ACCOUNT,

};
