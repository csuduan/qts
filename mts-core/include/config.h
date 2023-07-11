//
// Created by 段晴 on 2022/1/24.
//

#ifndef MTS_CORE_CONFIG_H
#define MTS_CORE_CONFIG_H

#include <string>
#include <vector>
#include "xpack/json.h"
#include "xpack/xml.h"
using namespace  std;

namespace config{


    struct TradeSetting{
        string db;
        string dataPath;
        string logPath;
        int port;
        vector<AcctConf> accts;
    XPACK(M(dataPath,logPath,db,port,accts))
    };



    struct QuoteSetting{
        string db;
        string dataPath;
        string logPath;
        int port;
        vector<QuoteInfo> quotes;
    XPACK(M(dataPath,logPath,db,quotes),O(port))

    };

    struct StrRef{
        string id;
        string volptt;
        string trgid;
        string vol;
    XPACK(O(id,volptt,trgid,vol))
    };
    struct StrStrategy{
        string id;
        StrRef ref;
    XPACK(O(id,ref))

    };
    struct StrConfig {
        vector<StrStrategy> strategy;
        XPACK(M(strategy))
    };

    struct StrategySetting{
        string accountId;
        string strategyId;
        string className;
        int barLevel;
        map<string,string> paramMap;
        set<string> contracts;
    XPACK(M(accountId,strategyId,className),O(paramMap,contracts,barLevel));
    };

}

#endif //MTS_CORE_CONFIG_H
