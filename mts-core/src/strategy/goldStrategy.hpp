//
// Created by 段晴 on 2022/2/1.
//

#ifndef MTS_CORE_GOLDSTRATEGY_HPP
#define MTS_CORE_GOLDSTRATEGY_HPP

#include "strategy/strategy.h"

#include "data.h"
#include "strategyFactory.hpp"

class GoldStrategy : public Strategy {
public:
    GoldStrategy() {
    }


    void onTick(Tick *tick) override {
        Strategy::onTick(tick);
        logi("gold onTick: {} {}", tick->symbol, tick->updateTime);
        //拷贝到lastTick;
        //memccpy(&lastTick, tick, 0, sizeof(Tick));

        int lenLong = 60;
        int lenShort = 30;

        if (tick->symbol != this->setting->trgSymbol)
            return;

        Tick lastTick{0};
        memccpy(&lastTick, tick, 0, sizeof(Tick));
        trgTickList.emplace_back(std::move(lastTick));
        if (trgTickList.size() > lenLong) {
            auto el = trgTickList.erase(trgTickList.begin());
        }
        double avgLong = getAvgPrice(lenLong);
        double avgShort = getAvgPrice(lenShort);

        logi("strategy: avgLong:{},avgShort:{}", avgLong, avgShort);
        if (avgLong > avgShort && lastAvgLong < lastAvgShort) {
            //开多
            logi("open long...");
        }

        if (avgLong < avgShort && lastAvgLong > lastAvgShort) {
            //平多
            logi("close long...");

        }


    }

private:
    Tick *lastRefTick;
    Tick *lastTrgTick;

    double lastAvgLong;
    double lastAvgShort;

    std::vector<Tick> trgTickList;


    double inline getWprice(Tick *tick) {
        double wprice = tick->bidPrice1 +
                        (tick->askPrice1 - tick->bidPrice1) * (tick->bidVolume1) /
                        (tick->bidVolume1 + tick->askVolume1);
        return wprice;
    }

    double getAvgPrice(int len) {
        if (trgTickList.size() < len)
            return 0;
        int start = trgTickList.size() - len;

        int totalPrice = 0;
        for (int i = start; i < trgTickList.size() - 1; i++) {
            totalPrice += trgTickList[i].lastPrice;
        }
        return totalPrice / len;
    }

};

REGISTER_STRATEGY(GoldStrategy, "strgold");

#endif //MTS_CORE_GOLDSTRATEGY_HPP
