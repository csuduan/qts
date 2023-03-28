package com.bingbei.mts.admin.entity;
public interface Operate {
    enum Account{
        CONNECT,
        DISCONNECT

    }

    enum TradeEngine{
        CONNECT_MD,
        DISCONNECT_MD,
        START,
        STOP
    }

}
