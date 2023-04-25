#pragma once

/**
 * 数据结构
 *
 */

#include <string>
#include <map>
#include <set>
#include <list>
#include <vector>
#include <tuple>
#include "Enums.h"
#include "LockFreeQueue.hpp"
#include "xpack/json.h"

using std::string;
using std::map;
using std::vector;

struct Event {
public:
    EvType type;
    long tsc;
    void *data;
};


struct Position;
struct Order;

struct Contract {
    string accountId;
    string name;// 合约中文名
    string type; // 合约类型
    string symbol; // 合约代码
    string exchange; // 交易所代码
    //string posDateType;//持仓日期类型（可用于判断是否区分今昨仓）

    //期货相关
    double priceTick; // 最小变动价位
    double longMarginRatio; // 多头保证金率
    double shortMarginRatio; // 空头保证金率
    bool maxMarginSideAlgorithm; // 最大单边保证金算法
    int multiple;//合约乘数

    // 期权相关
    double strikePrice; // 期权行权价
    string underlyingSymbol; // 标的物合约代码
    string optionType; /// 期权类型
    string expiryDate; // 到期日
XPACK(M(name, type, symbol, exchange), O(priceTick, longMarginRatio, multiple))
};

struct LoginInfo {
    string id;
    string tdType;
    string tdAddress;
    string brokerId;
    string userId;
    string useName;
    string password;
    string authCode;
    string appId;
XPACK(M(id,tdType,tdAddress,brokerId,userId,password,authCode,appId),O(useName))
};

struct Account {
    string id;
    string name;
    LoginInfo loginInfo;
    int cpuNumTd;
    int cpuNumEvent;

    double preBalance; // 昨日账户结算净值
    double balance; // 账户净值
    double available; // 可用资金
    double commission; // 今日手续费
    double margin; // 保证金占用
    double closeProfit; // 平仓盈亏
    double positionProfit; // 持仓盈亏
    double deposit; // 入金
    double withdraw; // 出金

    LockFreeQueue<Event> *queue;

    //账户持仓(用于校验)
    map<string, Position *> accoPositionMap;
    //合约信息
    map<string, Contract *> contractMap;
    //报单信息
    map<int, Order *> orderMap;

XPACK(M(id,name,loginInfo,preBalance,balance,available,commission,margin,closeProfit,positionProfit))
};

struct Quote {
    string name;
    string quoteType;//行情 TICK,ORDER,TRADE
    string type;//网关类型
    string address;//地址
    string userId;
    string password;
    set<string> subList;
    string dumpPath;
    map<string, Contract *> *contractMap;
    LockFreeQueue<Event> *queue;

XPACK(M(name,quoteType,type,address))

};


struct Position {
    string tradingDay;
    string positionId;//持仓编号（合约代码-方向）
    string symbol; // 代码
    POS_DIRECTION direction; // 持仓方向
    string direction_s;

    string exchange; // 交易所代码
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
    Position(string symbol, POS_DIRECTION direction) {
        this->symbol = symbol;
        this->direction = direction;
        this->direction_s = enum_string(direction);
        this->positionId = symbol + "-" + std::to_string(static_cast<int>(direction));
    }

XPACK(M(tradingDay,symbol,exchange,direction_s,pos,ydPos,tdPos,avgPrice));

};


struct Order {
    // 代码编号相关
    string symbol; // 代码
    string exchange; // 交易所代码
    int orderRef; // 订单编号
    string orderSysId;//交易所报单编号
    // 报单相关
    TRADE_DIRECTION direction;//报单方向
    string direction_s;
    string positionId;//持仓代码（合约-方向）
    OFFSET offset; // 报单开平仓
    string offset_s;
    ORDER_TYPE orderType;//报单方式
    double price; // 报单价格
    int totalVolume; // 报单总数量
    int tradedVolume; // 报单成交数量
    ORDER_STATUS status; // 报单状态
    string status_s;
    string statusMsg;
    string tradingDay;//交易日期
    //string actionDate; // 自然日期
    //string orderTime; // 发单时间
    string updateTime; // 最后修改时间
    long updateTsc;
    bool canceling = false;//测单中
    bool finished = false;

    // CTP/LTS相关
    int frontID; // 前置机编号
    int sessionID; // 连接编号

XPACK(M(tradingDay,orderRef,symbol,exchange,offset_s,direction_s,price, totalVolume,tradedVolume,status_s,statusMsg,updateTime));

};


struct Action{
    int orderRef;
    int frontId;
    long long sessionId;
};

struct Trade {
    // 代码编号相关
    string symbol; // 代码
    string exchange; // 交易所代码
    string tradeId; // 成交编号
    int orderRef;

    // 成交相关
    TRADE_DIRECTION direction; //成交方向
    string direction_s;
    OFFSET offset; // 成交开平仓
    string offset_s;
    double price; // 成交价格
    int volume; // 成交数量

    string tradingDay; // 交易日
    string tradeDate; // 业务发生日
    string tradeTime; // 时间(HHMMSSmmm)

    long updateTsc;

    POS_DIRECTION getPosDirection() {
        if (offset == OFFSET::OPEN)
            return direction == TRADE_DIRECTION::BUY ? POS_DIRECTION::LONG : POS_DIRECTION::SHORT;
        else
            return direction == TRADE_DIRECTION::SELL ? POS_DIRECTION::LONG : POS_DIRECTION::SHORT;
    }

XPACK(M(tradingDay,tradeId,orderRef,symbol,exchange,offset_s,direction_s,price, volume,tradeTime));

};

/**
 * TICK行情
 */
struct Tick {
    string symbol; // 合约代码
    string exchange; // 交易所代码
    string tradingDay; //交易日
    string actionDay;  //自然日
    float updateTime; //时间 格式:091230.500

    string source;//来源

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

    //用于性能统计的相关字段
    long recvTsc;  //接收时间  tsc
    long eventTsc;//事件触发时间 tsc
};

struct Bar {
    BAR_LEVEL level;//级别M1,M5
    string level_s;
    string symbol; // 代码
    string exchange;
    string tradingDay; // 交易日
    string actionDay; // 业务发生日
    int barTime; // 时间(HHmmss)
    float updateTime;//最新时间(HHmmss.SSS)
    // LocalDateTime dateTime;
    double open = 0;
    double high = 0;
    double low = 0;
    double close = 0;

    int volume = 0; // 成交量
    double openInterest = 0; // 持仓量

    int tickCount = 0;

    bool saved = false;//是否持久化

XPACK(M(level,symbol,exchange,tradingDay,actionDay,barTime,updateTime,open,high,low,close,volume,openInterest,tickCount));


};

struct StrategySetting {
    string accountId;
    string strategyId;
    string className;
    BAR_LEVEL barLevel;
    map<string, string> paramMap;
    set<string> contracts;
};

struct OrderReq{
    string symbol;
    string offset;
    string direct;
    double price;
    int volume;
XPACK(M(symbol,offset,direct,price, volume));
};


struct CancelReq{
    int orderRef;
    int frontId;
    long long sessionId;
XPACK(M(orderRef));
};



struct Message{
    int no=0;//序号
    string   type;//消息类型
    string   actId;//账户代码
    string   data;//报文字符串
    MSG_TYPE msgType;
XPACK(M(no,type),O(actId,data));
};

struct CommReq{
    string param;
XPACK(O(param));
};






