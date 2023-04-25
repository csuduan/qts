//
// Created by 段晴 on 2022/1/24.
//

#ifndef MTS_CORE_CONFIG_H
#define MTS_CORE_CONFIG_H

#include <string>
#include <vector>
#include "xpack/json.h"
using namespace  std;

namespace config{


    struct Quote{
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
        string dataPath;
        int tcpPort;
        vector<Quote> quoteGroups;
        XPACK(M(dataPath,tcpPort,quoteGroups))
    };

    struct Account{
        string id;
        string name;
        string user;
        string tdAddress;
        vector<string> quotes;
        string dbPath;
        int cpuNumTd;
        int cpuNumEvent;
        bool autoStart;
        int queueSize=1<<10;
    XPACK(M(id,name,user,tdAddress),O(quotes,dbPath,autoStart,queueSize,cpuNumTd,cpuNumEvent));
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
