//
// Created by Administrator on 2020/6/10.
//
#include "GoldStrategy.h"
#include "define.h"



void GoldStrategy::onBar(Bar * bar) {
    Strategy::onBar(bar);

}

void GoldStrategy::onTick(Tick * tick) {
    Strategy::onTick(tick);
    //logi("gold onTick: {}",tick->symbol);
}
