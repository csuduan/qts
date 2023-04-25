//
// Created by 段晴 on 2022/2/28.
//

#ifndef MTS_CORE_RDTSC_H
#define MTS_CORE_RDTSC_H

inline unsigned long long rdtsc() {
    return __builtin_ia32_rdtsc();
}

inline unsigned long long rdtscp() {
    unsigned int dummy;
    return __builtin_ia32_rdtscp(&dummy);
}

#endif //MTS_CORE_RDTSC_H
