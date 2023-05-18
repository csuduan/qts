//
// Created by 段晴 on 2022/3/3.
//

#ifndef MTS_CORE_DATABUILDER_H
#define MTS_CORE_DATABUILDER_H

#include "Data.h"
#include "Config.h"
#include "Util.h"

Quote* buildQuote(config::QuoteConf & quoteConf){
    Quote *quote=new Quote();
    quote->name=quoteConf.name;
    quote->quoteType=quoteConf.type;
    vector<string> tmp;
    Util::split(quoteConf.address,tmp,"|");
    quote->type=tmp[0];
    quote->address=tmp[1];
    quote->subList=quoteConf.subList;
    tmp.clear();
    Util::split(quoteConf.user,tmp,"|");
    if(tmp.size()>0){
        quote->userId=tmp[0];
        quote->password=tmp[1];
    }
    quote->queue=new LockFreeQueue<Event>(quoteConf.queueSize);
    return quote;
}

Account * buildAccount(config::AcctConf & actConf){
    vector<string> tmp;
    Util::split(actConf.user,tmp,"|");
    string userId=tmp[0];
    string passwd=tmp[1];
    tmp.clear();
    Util::split(actConf.tdAddress,tmp,"|");
    string tdType=tmp[0];
    string tdAddress=tmp[1];
    string brokerId;
    string appId;
    string authCode;
    if(tmp.size()>=3){
        string authInfo=tmp[2];
        tmp.clear();
        Util::split(authInfo,tmp,":");
        brokerId=tmp[0];
        appId=tmp[1];
        authCode=tmp[2];
    }
    tmp.clear();

    Account * account=new Account();
    account->id=actConf.id;
    account->name=actConf.name;
    account->cpuNumEvent=actConf.cpuNumEvent;
    account->cpuNumTd=actConf.cpuNumTd;
    account->loginInfo.id=actConf.id;
    account->loginInfo.userId=userId;
    account->loginInfo.password=passwd;
    account->loginInfo.tdType=tdType;
    account->loginInfo.tdAddress=tdAddress;
    account->loginInfo.brokerId=brokerId;
    account->loginInfo.appId=appId;
    account->loginInfo.authCode=authCode;
    account->queue=new LockFreeQueue<Event>(actConf.queueSize);
    account->autoConnect=actConf.autoConnect;
    account->agent=actConf.agent;
    return account;
}

StrategySetting * buildStrategySetting(config::StrategySetting &setting){
    StrategySetting * strategySetting=new StrategySetting();
    strategySetting->className=setting.className;
    strategySetting->barLevel=(BAR_LEVEL)setting.barLevel;
    strategySetting->strategyId=setting.strategyId;
    strategySetting->accountId=setting.accountId;
    strategySetting->contracts=setting.contracts;
    strategySetting->paramMap=setting.paramMap;
    return strategySetting;
}



template<class T>
static string buildMsg(MSG_TYPE msgType, T & data,string actId){
    MessageS<T> msg ={0};
    msg.type= enum_string(msgType);
    msg.data=data;
    msg.sid=actId;
    string json = xpack::json::encode(msg);
    return json;
}



#endif //MTS_CORE_DATABUILDER_H
