//
// Created by 段晴 on 2022/2/1.
//

#ifndef MTS_CORE_GOLDSTRATEGY_HPP
#define MTS_CORE_GOLDSTRATEGY_HPP

#include "strategy/strategy.h"

#include "data.h"
#include "strategyFactory.hpp"

class GoldStrategy: public Strategy{
public:
    GoldStrategy(){
    }
    void onTick(Tick * tick) override{
        Strategy::onTick(tick);
        //logi("gold onTick: {}",tick->symbol);
    }
};
REGISTER_STRATEGY(GoldStrategy, "GoldStrategy");

#endif //MTS_CORE_GOLDSTRATEGY_HPP
