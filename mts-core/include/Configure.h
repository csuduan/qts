//
// Created by 段晴 on 2022/1/24.
//

#ifndef MTS_CORE_CONFIGURE_H
#define MTS_CORE_CONFIGURE_H

#include <string>
using namespace  std;
class Configure {
public:
    static void load(string engineId,string file="./config.json");
};



#endif //MTS_CORE_CONFIGURE_H
