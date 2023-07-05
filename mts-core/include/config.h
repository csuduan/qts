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
    struct QuoteConf{
        string name;
        string type;
        string address;
        set<string> subList;
        bool enable;
        string user;
        int queueSize=1<<10;
        XPACK(M(name,type,address,enable),O(subList,user,queueSize))
    };
    struct Setting{
        string db;
        string dataPath;
        string logPath;
        int port;
        XPACK(M(dataPath,logPath,db,port))
    };

    struct AcctConf{
        string agent;
        string id;
        string name;
        string user;
        string tdAddress;
        vector<string> quotes;
        int cpuNumTd;
        int cpuNumEvent;
        bool autoConnect;
        int queueSize=1<<10;
    XPACK(M(agent,id,name,user,tdAddress),O(quotes,autoConnect,queueSize,cpuNumTd,cpuNumEvent));
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
