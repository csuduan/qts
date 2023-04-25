//
// Created by 段晴 on 2022/2/25.
//

#ifndef MTS_CORE_QUOTE_H
#define MTS_CORE_QUOTE_H
#include <string>

struct MemTick{
    long seq;
    char source[5];//来源
    char symbol[31]; // 合约代码
    char exchange[9]; // 交易所代码
    char tradingDay[9]; //交易日
    float updateTime; //时间 格式:091230.500

    double lastPrice = 0; // 最新成交价
    int lastVolume = 0; // 最新成交量
    int volume = 0; // 今天总成交量
    double openInterest = 0; // 持仓量
    long preOpenInterest = 0L;// 昨持仓
    double preClosePrice = 0; // 前收盘价
    double preSettlePrice = 0; // 昨结算

    double openPrice = 0; // 今日开盘价
    double highPrice = 0; // 今日最高价
    double lowPrice = 0; // 今日最低价
    double upperLimit = 0; // 涨停价
    double lowerLimit = 0; // 跌停价

    double bidPrice1 = 0;
    double bidPrice2 = 0;
    double bidPrice3 = 0;
    double bidPrice4 = 0;
    double bidPrice5 = 0;
    double askPrice1 = 0;
    double askPrice2 = 0;
    double askPrice3 = 0;
    double askPrice4 = 0;
    double askPrice5 = 0;
    int bidVolume1 = 0;
    int bidVolume2 = 0;
    int bidVolume3 = 0;
    int bidVolume4 = 0;
    int bidVolume5 = 0;
    int askVolume1 = 0;
    int askVolume2 = 0;
    int askVolume3 = 0;
    int askVolume4 = 0;
    int askVolume5 = 0;
    long recvTime=0;//tsc,非ns
};

///逐笔委托
struct MemOrder{
    int seq;

};

///逐笔成交
struct MemTrade{
    int seq;

};

#endif //MTS_CORE_QUOTE_H

