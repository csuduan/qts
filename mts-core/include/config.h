//
// Created by 段晴 on 2022/1/24.
//

#ifndef MTS_CORE_CONFIG_H
#define MTS_CORE_CONFIG_H

#include <string>
#include <vector>
#include "xpack/json.h"
#include "xpack/xml.h"
#include "message.h"
using namespace  std;

namespace config{
    struct Setting{
        string db;
        string dataPath;
        bool log2File;
        int port;
        vector<QuoteInfo> quotes;
        vector<AcctConf> accts;
    XPACK(M(dataPath,db),O(port,log2File,quotes,accts))

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
