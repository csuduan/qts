//
// Created by 段晴 on 2022/2/27.
//

#ifndef MTS_CORE_OSTDATA_H
#define MTS_CORE_OSTDATA_H
#include "ost/UTApiStruct.h"
#include "Data.h"

class DataMapping{

    static map<TUTOrderStatusType, ORDER_STATUS> ostStatusMap;
    static map<TUTExchangeIDType,EXCHANGE> ostExgMap;
    static map<int, string> qryRetMsgMap;

};
map<TUTOrderStatusType, ORDER_STATUS> DataMapping::ostStatusMap = {
        {UT_OST_AllTraded,             ORDER_STATUS::ALLTRADED},
        {UT_OST_PartTradedQueueing,    ORDER_STATUS::QUEUEING},
        {UT_OST_PartTradedNotQueueing, ORDER_STATUS::NOTQUEUEING},
        {UT_OST_NoTradeQueueing,       ORDER_STATUS::QUEUEING},
        {UT_OST_NoTradeNotQueueing,    ORDER_STATUS::NOTQUEUEING},
        {UT_OST_Canceled,              ORDER_STATUS::CANCELLED},
        {UT_OST_Unknown,               ORDER_STATUS::UNKNOWN}
};
map<TUTExchangeIDType,EXCHANGE> DataMapping::ostExgMap ={
        {UT_EXG_SSE,EXCHANGE::SSE},
        {UT_EXG_SZSE,EXCHANGE::SZE},
        {UT_EXG_SHFE,EXCHANGE::SHFE},
        {UT_EXG_CFFEX,EXCHANGE::CFFEX},
        {UT_EXG_DCE,EXCHANGE::DCE},
        {UT_EXG_CZCE,EXCHANGE::CZCE},
        {UT_EXG_INE,EXCHANGE::INE},
        {UT_EXG_HKEX,EXCHANGE::HK}
};
map<int, string> DataMapping::qryRetMsgMap = {
        {0,  "成功"},
        {-1, "网络连接失败"},
        {-2, "未处理请求超过许可数"},
        {-3, "每秒发送请求数超过许可数"}
};

#endif //MTS_CORE_OSTDATA_H
