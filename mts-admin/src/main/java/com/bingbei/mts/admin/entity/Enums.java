package com.bingbei.mts.admin.entity;
public interface Enums {
    enum MSG {
        EXIT,
        MD_CONNECT,
        MD_DISCOUN,
        MD_SUBS,
        ACT_CONNECT,
        ACT_DISCONN,
        ACT_PAUSE_OPEN,
        ACT_PAUSE_CLOSE,
        ACT_ORDER,
        ACT_CANCEL,

        QRY_POSITION,
        QRY_TRADE,
        QRY_ORDER,
        QRY_CONTRACT,

        RSP_LOG,
        RSP_ORDER,
        RSP_POSITION,
        RSP_TRADE,
        RSP_CONTRACT,
        RSP_BAR
    }

}
