package com.bingbei.mts.admin.entity;
public interface Operate {
    enum Cmd {
        EXIT,
        MD_CONNECT,
        MD_DISCOUNT,
        MD_SUBS,
        ACT_CONNECT,
        ACT_DISCOUNT,
        ACT_PAUSE_OPEN,
        ACT_PAUSE_CLOSE,
        ACT_ORDER,
        ACT_CANCEL
    }

}
