#pragma once

#include<string>
#include <map>
#include <set>
#include <list>
#include "Enums.h"
#include "LockFreeQueue.hpp"

using namespace std;

struct AcctPosition;
struct MdInfo{
    std::string id;
    std::string type;
    std::string mdAddress;
};

struct Contract{
    std::string accountId;
    //通用
    std::string name;// 合约中文名
    std::string type; // 合约类型
    std::string symbol; // 合约代码
    std::string exchange; // 交易所代码
    std::string stdSymbol; // 标准代码,通常是 合约代码.交易所代码(大写，如CU2201.SHF)
    std::string posDateType;//持仓日期类型（可用于判断是否区分今昨仓）


    //期货相关
    double priceTick; // 最小变动价位
    double longMarginRatio; // 多头保证金率
    double shortMarginRatio; // 空头保证金率
    bool maxMarginSideAlgorithm; // 最大单边保证金算法
    int multiple;//合约乘数

    // 期权相关
    double strikePrice; // 期权行权价
    std::string underlyingSymbol; // 标的物合约代码
    std::string optionType; /// 期权类型
    std::string expiryDate; // 到期日
};

struct LoginInfo {
    std::string tdType;
    std::string tdAddress;
    std::string mdType;
    std::string mdAddress;
    std::string brokerId;
    std::string userId;
    std::string useName;
    std::string password;
    std::string authCode;
    std::string appId;
};

struct Account {
    std::string id;
    std::string name;
    LoginInfo loginInfo;

    double preBalance; // 昨日账户结算净值
    double balance; // 账户净值
    double available; // 可用资金
    double commission; // 今日手续费
    double margin; // 保证金占用
    double closeProfit; // 平仓盈亏
    double positionProfit; // 持仓盈亏
    double deposit; // 入金
    double withdraw; // 出金

    //账户持仓(用于校验)
    std::map<std::string, AcctPosition*> accoPositionMap;
    LockFreeQueue<Event> eventQueue ={1 << 10};
    //合约信息
    std::map<string, Contract *> contractMap;
};


struct AcctPosition {
    // 账号代码相关
    std::string positionId;//持仓编号（合约代码-方向）
    // 代码编号相关
    std::string symbol; // 代码
    POS_DIRECTION direction; // 持仓方向

    std::string exchange; // 交易所代码
    int multiple;//合约乘数
    // 持仓相关
    int pos; // 持仓量
    int onway; // 在途数量(>0在途开仓，<0在途平仓)
    int ydPos; // 昨仓（=pos-tdPos）
    int tdPos; // 今仓
    int ydPosition;//昨仓(静态)

    double lastPrice; // 计算盈亏使用的行情最后价格
    double avgPrice; // 持仓均价
    double openPrice; // 开仓均价
    double positionProfit; // 持仓盈亏
    double openPositionProfit; // 开仓盈亏

    double useMargin; // 占用的保证金
    double exchangeMargin; // 交易所的保证金
    double contractValue; // 最新合约价值
    AcctPosition(string symbol, POS_DIRECTION direction){
        this->direction=direction;
        this->positionId=symbol+"-"+ to_string(static_cast<int>(direction));
    }
};


/**
 * 数据结构
 */

struct ShareMemoryIndex {
    int orderStart;
    int orderEnd;
    int tradeStart;
    int tradeEnd;
    int cmdStart;
    int cmdEnd;
};

struct Order {
    std::string accountID; // 账户代码
    // 代码编号相关
    std::string symbol; // 代码
    std::string exchange; // 交易所代码
    std::string orderRef; // 订单编号
    std::string orderSysId;//交易所报单编号
    // 报单相关
    POS_DIRECTION posDirection; // 持仓方向
    TRADE_DIRECTION direction;//报单方向
    string positionId;//持仓代码（合约-方向）
    OFFSET offset; // 报单开平仓
    ORDER_TYPE orderType;//报单方式
    double price; // 报单价格
    int totalVolume; // 报单总数量
    int tradedVolume; // 报单成交数量
    ORDER_STATUS status; // 报单状态
    std::string statusMsg;
    std::string tradingDay;
    std::string orderDate; // 发单日期
    std::string orderTime; // 发单时间
    std::string cancelTime; // 撤单时间
    std::string activeTime; // 激活时间
    std::string updateTime; // 最后修改时间
    bool canceling = false;//测单中
    bool finished = false;

    // CTP/LTS相关
    int frontID; // 前置机编号
    int sessionID; // 连接编号
};

struct Trade {
    // 代码编号相关
    std::string symbol; // 代码
    std::string exchange; // 交易所代码
    std::string tradeID; // 成交编号
    std::string orderRef;

    // 成交相关
    TRADE_DIRECTION direction; //成交方向
    OFFSET offset; // 成交开平仓
    double price; // 成交价格
    int volume; // 成交数量

    std::string tradingDay; // 交易日
    std::string tradeDate; // 业务发生日
    std::string tradeTime; // 时间(HHMMSSmmm)

    POS_DIRECTION getPosDirection() {
        if (offset == OFFSET::OPEN)
            return direction == TRADE_DIRECTION::BUY ? POS_DIRECTION::LONG : POS_DIRECTION::SHORT;
        else
            return direction == TRADE_DIRECTION::SELL ? POS_DIRECTION::LONG : POS_DIRECTION::SHORT;
    }


};

struct Cmd {

};

/**
 * TICK行情
 */
struct Tick {
    // 代码相关
    string symbol; // 代码
    string exchange; // 交易所代码
    string tradingDay; // 交易日
    string actionDay; // 业务发生日
    string actionTime;
    timespec timeStampRecv;
    timespec timeStampOnEvent;

    std::string source;//来源

    // 成交数据
    double lastPrice = 0; // 最新成交价
    int lastVolume = 0; // 最新成交量
    int volume = 0; // 今天总成交量
    double openInterest = 0; // 持仓量

    long preOpenInterest = 0L;// 昨持仓
    double preClosePrice = 0; // 前收盘价
    double preSettlePrice = 0; // 昨结算

    // 常规行情
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

};

struct Bar {
    std::string level;//级别M1,M5
    std::string symbol; // 代码
    std::string tradingDay; // 交易日
    std::string actionDay; // 业务发生日
    long barTime; // 时间(HHmmss)
    double actionTime;//最新时间(HHmmss.SSS)
    // LocalDateTime dateTime;
    double open = 0;
    double high = 0;
    double low = 0;
    double close = 0;

    int volume = 0; // 成交量
    double openInterest = 0; // 持仓量

};

struct StrategySetting{
    string accountId;
    string strategyId;
    string className;
    map<string,string> paramMap;
    set<string> contracts;
};




