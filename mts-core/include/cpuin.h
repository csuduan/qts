//
// Created by 段晴 on 2022/2/28.
//

#ifndef MTS_CORE_CPUIN_H
#define MTS_CORE_CPUIN_H

#include <sched.h>
#include <iostream>

bool cpupin(int cpuid) {
    cpu_set_t my_set;
    CPU_ZERO(&my_set);
    CPU_SET(cpuid, &my_set);
    if (sched_setaffinity(0, sizeof(cpu_set_t), &my_set)) {
        //std::cout << "sched_setaffinity error: " << strerror(errno) << std::endl;
        return false;
    }

    return true;
}

#endif //MTS_CORE_CPUIN_H
