//
// Created by 段晴 on 2022/2/1.
//

#ifndef MTS_CORE_GOLDSTRATEGY_H
#define MTS_CORE_GOLDSTRATEGY_H

#include "strategy/Strategy.h"

#include "Data.h"
#include "StrategyFactory.hpp"

class GoldStrategy: public Strategy{
public:
    GoldStrategy(){
    }
    void onTick(Tick * tick) override;
};
REGISTER_STRATEGY(GoldStrategy, "GoldStrategy");

#endif //MTS_CORE_GOLDSTRATEGY_H
