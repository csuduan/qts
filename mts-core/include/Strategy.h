#pragma once

#include <string>

class Strategy
{
private:
    std::string strategyId;

public:
    void init();
    void onTick();
    void onBar();
    void onOrder();
    void onTrade();
};

