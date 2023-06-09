//
// Created by 段晴 on 2022/3/3.
//

#ifndef MTS_CORE_DATABUILDER_H
#define MTS_CORE_DATABUILDER_H

#include "Data.h"
#include "Config.h"
#include "Util.h"

Quote* buildQuote(QuoteConf & quoteConf){
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

Acct * buildAccount(AcctConf & actConf){
    string userId=actConf.user;
    string passwd=actConf.pwd;
    vector<string> tmp;

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

    Acct * account=new Acct();
    account->id=actConf.id;
    //account->cpuNumEvent=actConf.cpuNumEvent;
    //account->cpuNumTd=actConf.cpuNumTd;
    account->loginInfo.id=actConf.id;
    account->loginInfo.userId=userId;
    account->loginInfo.password=passwd;
    account->loginInfo.tdType=tdType;
    account->loginInfo.tdAddress=tdAddress;
    account->loginInfo.brokerId=brokerId;
    account->loginInfo.appId=appId;
    account->loginInfo.authCode=authCode;
    account->queue=new LockFreeQueue<Event>(1024);
    //account->autoConnect=actConf.autoConnect;
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
static Message* buildMsg(MSG_TYPE msgType, T& data,string actId){
    Message *msg =new Message;
    msg->type= enum_string(msgType);
    msg->data=xpack::json::encode(data);
    msg->acctId=actId;
    //string json = xpack::json::encode(msg);
    return msg;
}




#endif //MTS_CORE_DATABUILDER_H
