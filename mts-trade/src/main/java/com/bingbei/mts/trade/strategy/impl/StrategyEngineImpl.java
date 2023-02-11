package com.bingbei.mts.trade.strategy.impl;

import com.bingbei.mts.common.entity.Bar;
import com.bingbei.mts.common.entity.Contract;
import com.bingbei.mts.common.entity.Tick;
import com.bingbei.mts.trade.entity.PositionDetail;
import com.bingbei.mts.trade.strategy.StrategyEngine;
import com.bingbei.mts.trade.strategy.StrategySetting;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StrategyEngineImpl implements StrategyEngine {
    @Override
    public int getEngineType() {
        return 0;
    }

    @Override
    public void sendOrder(OrderReq orderReq) {

    }

    @Override
    public void cancelOrder(String originalOrderID, String operatorID) {

    }

    @Override
    public List<Tick> loadTickDataByOffsetDay(String tradingDay, int offsetDay, String rtSymbol) {
        return null;
    }

    @Override
    public List<Bar> loadBarDataByOffsetDay(String tradingDay, int offsetDay, String rtSymbol) {
        return null;
    }

    @Override
    public List<Tick> loadTickData(LocalDateTime startDateTime, LocalDateTime endDateTime, String rtSymbol) {
        return null;
    }

    @Override
    public List<Bar> loadBarData(LocalDateTime startDateTime, LocalDateTime endDateTime, String rtSymbol) {
        return null;
    }

    @Override
    public void asyncSaveStrategySetting(StrategySetting strategySetting) {

    }

    @Override
    public void asyncSavePositionDetail(List<PositionDetail> positionDetailList) {

    }

    @Override
    public double getPriceTick(String rtSymbol, String gatewayID) {
        return 0;
    }

    @Override
    public Contract getContractByFuzzySymbol(String fuzzySymbol) {
        return null;
    }

    @Override
    public Contract getContract(String rtSymbol, String gatewayID) {
        return null;
    }
}
