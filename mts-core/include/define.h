//
// Created by 段晴 on 2022/1/26.
//

#ifndef MTS_CORE_DEFINE_H
#define MTS_CORE_DEFINE_H

//#define USE_LOCK
//开启spinlock锁，多生产者多消费者场景
#define USE_MB
//开启Memory Barrier
#define USE_POT
//开启队列大小的2的幂对齐

#include <vector>
#include <string>
#include <queue>
#include "Data.h"
#include "Enums.h"
#include "fmtlog/fmtlog.h"
#include "util/Util.h"



#endif //MTS_CORE_DEFINE_H

