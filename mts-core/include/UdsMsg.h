//
// Created by 段晴 on 2022/1/24.
//

#ifndef MTS_CORE_UDSMSG_H
#define MTS_CORE_UDSMSG_H
#include <string>


struct UdsMsg {
    char cmd[8];
    char date[1024];
};


#endif //MTS_CORE_UDSMSG_H
