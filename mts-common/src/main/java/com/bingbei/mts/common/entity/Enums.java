package com.bingbei.mts.common.entity;

import java.util.HashSet;

public interface Enums {
    //头寸方向
    enum POS_DIRECTION {
        LONG,
        SHORT,
        NET,
        NONE,
    }

    //交易方向
    enum TRADE_DIRECTION{
        BUY,
        SELL,
        NONE
    }

    //交易类型
    enum OFFSET{
        OPEN,
        CLOSE,
        CLOSETD,
        CLOSEYD,
        NONE,
    }

    //BAR级别
    enum BAR_LEVEL{
        M1,
        M5,
        M10,
        D1
    }
    //网关类型
    enum GATEWAY_TYPE{
        CTP,
        OST,
        SIM,
        REM
    }
    //报单类型
    enum ORDER_TYPE {
        FAK,
        FOK,
    }
    //价格类型
    enum PRICE_TYPE{
        LIMIT,
        MARKET
    }
    //状态
    enum ORDER_STATUS{
        UNKNOWN, //未知
        NOTTRADED ,//未成交
        PARTTRADED,//部分成交
        ALLTRADED ,//全部成交
        CANCELLED ,//已撤单
        ERROR//错单
    }

    HashSet<ORDER_STATUS> STATUS_FINISHED = new HashSet<ORDER_STATUS>() {
        {
            add(ORDER_STATUS.ERROR);
            add(ORDER_STATUS.ALLTRADED);
            add(ORDER_STATUS.CANCELLED);
        }
    };
}
