#pragma once
#include <iostream>
#include "UdsServer.h"
#include "Data.h"
#include "nanoLog/Cycles.h"

class EngineContext
{
public:
    ~EngineContext() {
        std::cout << "destructor context!" << std::endl;
    }
    EngineContext(const EngineContext&) = delete;
    EngineContext& operator=(const EngineContext&) = delete;
    static EngineContext& get() {
        static EngineContext instance;
        return instance;

    }
    //RedisUtil* redisUtil;
    string engineId;
    //高性能时间
    void   getTime(struct timeval *now){
        double timeStamp=PerfUtils::Cycles::toSeconds(PerfUtils::Cycles::rdtsc(),0);
        long us=(timeStamp-startTimeStamp)*1e6;
        now->tv_sec=startTime.tv_sec+us/1e6;
        now->tv_usec=startTime.tv_usec+ us%1000000;
    }
    void init(){}
private:
    struct timeval startTime;
    double startTimeStamp;
    EngineContext() {
        gettimeofday(&startTime, NULL) ;
        startTimeStamp = PerfUtils::Cycles::toSeconds(PerfUtils::Cycles::rdtsc(),0);
    }
};

