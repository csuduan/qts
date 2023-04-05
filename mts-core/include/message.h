//
// Created by 段晴 on 2022/2/11.
//

#ifndef MTS_CORE_REQUEST_H
#define MTS_CORE_REQUEST_H


#include "xpack/json.h"

using namespace std;

namespace msg{
    enum MSG_TYPE{
        EXIT=0,
        MD_CONNECT,
        MD_DISCOUN,
        MD_SUBS,
        ACT_CONNECT,
        ACT_DISCONN,
        ACT_PAUSE_OPEN,
        ACT_PAUSE_CLOSE,
        ACT_ORDER,
        ACT_CANCEL,
        ACT_QRY_POSITION,
        ACT_QRY_TRADE,
        ACT_QRY_ORDER
    };

    static std::map<std::string,MSG_TYPE> msgTypeMap={
            {"EXIT", EXIT},
            {"MD_CONNECT",MD_CONNECT},
            {"MD_DISCOUN",MD_DISCOUN},
            {"MD_SUBS",MD_SUBS},
            {"ACT_CONNECT",ACT_CONNECT},
            {"ACT_DISCONN",ACT_DISCONN},
            {"ACT_PAUSE_OPEN",ACT_PAUSE_OPEN},
            {"ACT_PAUSE_CLOSE",ACT_PAUSE_CLOSE},
            {"ACT_ORDER",ACT_ORDER},
            {"ACT_CANCEL",ACT_CANCEL},
            };

    struct Message{
        int no;//序号
        string type;//消息类型
        string  data;//报文
        XPACK(M(no,type),O(data));
    };

    struct CommReq{
        string param;
        XPACK(O(param));
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
        string orderRef;
        XPACK(M(orderRef));
    };
}


#endif //MTS_CORE_REQUEST_H
