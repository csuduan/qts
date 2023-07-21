//
// Created by 段晴 on 2022/3/3.
//

#ifndef MTS_CORE_DATABUILDER_H
#define MTS_CORE_DATABUILDER_H

#include "data.h"
#include "config.h"
#include "common/util.hpp"
#include "trade/acct.h"

static StrategySetting *buildStrategySetting(config::StrategySetting &setting) {
    StrategySetting *strategySetting = new StrategySetting();
    strategySetting->strategyType = setting.className;
    strategySetting->barLevel = (BAR_LEVEL) setting.barLevel;
    strategySetting->strategyId = setting.strategyId;
    //strategySetting->contracts=setting.contracts;
    strategySetting->paramMap = setting.paramMap;
    return strategySetting;
}


template<class T>
static Message *buildMsg(MSG_TYPE msgType, T &data, string actId) {
    Message *msg = new Message;
    msg->type = enum_string(msgType);
    msg->data = xpack::json::encode(data);
    msg->acctId = actId;
    msg->requestId = "";
    //string json = xpack::json::encode(msg);
    return msg;
}


#endif //MTS_CORE_DATABUILDER_H
