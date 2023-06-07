package org.mts.common.model;

import java.util.HashSet;

public interface Enums {

    //账户状态
    enum ACCT_STATUS{
        UNKNOW, //未知
        CONNECTED,//已连接
        READY//就绪
    }
    //接口状态
    enum API_STATUS{
        UNKNOW,//未知
        ALL_DISCONN,//全部断开
        TD_DISCONN,//交易断开
        MD_DISCONN,//行情断开
        ALL_CONN,//全部已连接
    }

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

    enum ACCT_OPERATE {
        CONNECT,
        DISCONNECT,
        PAUSE_TRADE,
        RESUME_TRADE,
    }
    enum MSG_TYPE {
        RETURN,
        SYNC,
        PING,
        EXIT,
        RESTART,
        CONNECT,

        MD_SUBS,
        PAUSE_OPEN,
        PAUSE_CLOSE,
        ACT_ORDER,
        ACT_CANCEL,
        QRY_TRADE,
        QRY_ORDER,
        QRY_POSITION,
        QRY_ACCT,
        QRY_CONF,


        //推送消息
        ON_STATUS,
        ON_LOG,
        ON_POSITION,
        ON_TRADE,
        ON_ORDER,
        ON_CONTRACT,
        ON_BAR,
        ON_ACCT,
        ON_SERVER,
    }

}
