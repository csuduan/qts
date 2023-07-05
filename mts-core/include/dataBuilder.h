//
// Created by 段晴 on 2022/3/3.
//

#ifndef MTS_CORE_DATABUILDER_H
#define MTS_CORE_DATABUILDER_H

#include "data.h"
#include "config.h"
#include "common/util.hpp"
#include "trade/acct.h"

static Quote* buildQuote(QuoteConf & quoteConf){
    Quote *quote=new Quote();
    quote->id=quoteConf.id;
    quote->quoteType=quoteConf.type;
    vector<string> tmp;
    Util::split(quoteConf.address,tmp,"|");
    quote->type=tmp[0];
    quote->address=tmp[1];
    tmp.clear();
    Util::split(quoteConf.subList,tmp,",");
    for(auto &item : tmp)
        quote->subList.insert(item);
    tmp.clear();
    Util::split(quoteConf.user,tmp,"|");
    if(tmp.size()>0){
        quote->userId=tmp[0];
        quote->password=tmp[1];
    }
    quote->queue=new LockFreeQueue<Event>(1<<20);
    return quote;
}

static Acct * buildAccount(AcctConf * actConf){
    Acct * account=new Acct();
    account->id=actConf->id;
    account->acctConf=actConf;
    account->acctInfo=new AcctInfo;
    account->acctInfo->id=actConf->id;
    account->acctInfo->group=actConf->group;
    //account->cpuNumEvent=actConf.cpuNumEvent;
    //account->cpuNumTd=actConf.cpuNumTd;
    account->fastQueue=new LockFreeQueue<Event>(10240);
    account->msgQueue=new LockFreeQueue<Event>(10240);
    vector<string> tmp;
    Util::split(actConf->subList,tmp,",");
    for(auto &item : tmp)
        actConf->subSet.insert(item);
    tmp.clear();
    //account->autoConnect=actConf.autoConnect;
    return account;
}

static StrategySetting * buildStrategySetting(config::StrategySetting &setting){
    StrategySetting * strategySetting=new StrategySetting();
    strategySetting->strategyType=setting.className;
    strategySetting->barLevel=(BAR_LEVEL)setting.barLevel;
    strategySetting->strategyId=setting.strategyId;
    strategySetting->acctId=setting.accountId;
    //strategySetting->contracts=setting.contracts;
    strategySetting->paramMap=setting.paramMap;
    return strategySetting;
}



template<class T>
static Message* buildMsg(MSG_TYPE msgType, T& data,string actId){
    Message *msg =new Message;
    msg->type= enum_string(msgType);
    msg->data=xpack::json::encode(data);
    msg->acctId=actId;
    msg->requestId="";
    //string json = xpack::json::encode(msg);
    return msg;
}




#endif //MTS_CORE_DATABUILDER_H
