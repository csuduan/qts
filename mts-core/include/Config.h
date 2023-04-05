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
    struct Account{
        string id;
        string name;
        string user;
        string tdAddress;
        string mdAddress;
        vector<string> sublist;
        XPACK(M(id,name,user,tdAddress,mdAddress),O(sublist));
    };

    struct StrategySetting{
        string accountId;
        string strategyId;
        string className;
        map<string,string> paramMap;
        set<string> contracts;
        XPACK(M(accountId,strategyId,className),O(paramMap,contracts));
    };
}

#endif //MTS_CORE_CONFIG_H
