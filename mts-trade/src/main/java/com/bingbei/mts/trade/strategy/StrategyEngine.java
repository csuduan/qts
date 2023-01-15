package com.bingbei.mts.trade.strategy;

import com.bingbei.mts.common.entity.*;
import com.bingbei.mts.trade.entity.PositionDetail;

import java.time.LocalDateTime;
import java.util.List;

public interface StrategyEngine {
    /**
     * 获取引擎类型,区分实盘模拟盘
     *
     * @return
     */
    int getEngineType();

    /**
     *
     * @param orderReq
     * @return
     */
    void sendOrder(OrderReq orderReq);

    /**
     * 撤单
     * @param originalOrderID
     * @param operatorID
     */
    void cancelOrder(String originalOrderID,String operatorID);

    /**
     * 加载Tick数据,根据交易日向前推移,不包含交易日当天
     *
     * @param offsetDay
     * @return
     */
    List<Tick> loadTickDataByOffsetDay(String tradingDay, int offsetDay, String rtSymbol);

    /**
     * 加载Bar数据,根据交易日向前推移,不包含交易日当天
     *
     * @param offsetDay
     * @return
     */
    List<Bar> loadBarDataByOffsetDay(String tradingDay, int offsetDay, String rtSymbol);

    /**
     * 加载Tick数据,包含开始日期和结束日期
     *
     * @param
     * @param endDateTime
     * @return
     */
    List<Tick> loadTickData(LocalDateTime startDateTime, LocalDateTime endDateTime, String rtSymbol);

    /**
     * 加载Bar数据,包含开始日期和结束日期
     *
     * @param
     * @param endDateTime
     * @return
     */
    List<Bar> loadBarData(LocalDateTime startDateTime, LocalDateTime endDateTime, String rtSymbol);

    /**
     * 保存配置到数据库
     *
     * @param strategySetting
     */
    void asyncSaveStrategySetting(StrategySetting strategySetting);

    /**
     * 保存持仓到数据库
     *
     * @param
     */
    void asyncSavePositionDetail(List<PositionDetail> positionDetailList);

    /**
     * 获取合约最小变动价位
     *
     * @param rtSymbol
     * @param gatewayID
     * @return
     */
    double getPriceTick(String rtSymbol, String gatewayID);

    /**
     * 获取合约
     *
     * @param fuzzySymbol
     * @return
     */
    Contract getContractByFuzzySymbol(String fuzzySymbol);

    /**
     * 获取合约
     *
     * @param rtSymbol
     * @param gatewayID
     * @return
     */
    Contract getContract(String rtSymbol, String gatewayID);
}
