package org.qts.trader.strategy;

import org.qts.common.entity.trade.*;
import org.qts.trader.core.AcctInst;
import org.qts.trader.core.StrategyEngine;

import java.util.Map;


/**
 * @author sun0x00@gmail.com
 */
public interface Strategy {
	/**
	 * 初始化
	 */
	void init(StrategyEngine strategyEngine,StrategySetting strategySetting);
	/**
	 * 销毁
	 */
	void destroy();

	/**
	 * 暂停开仓
	 */
	void pauseOpen(boolean enable);

	/**
	 * 暂停平仓
	 */
	void pasueClose(boolean enable);

	/**
	 * 启动交易
	 */
	void startTrading();

	/**
	 * 停止交易
	 */
	void stopTrading();



	/**
	 * 是否处于交易状态
	 * 
	 * @return
	 */
	boolean isTrading();


	//推送数据
	void onTick(Tick tick) throws Exception;
	void onBar(Bar bar) throws Exception;
	void onOrder(Order order) throws Exception;
	void onTrade(Trade trade) throws Exception;


	/**
	 * 获取策略设置
	 * 
	 * @return
	 */
	StrategySetting getStrategySetting();

	/**
	 * 获取持仓Map
	 * 
	 * @return
	 */
	Map<String, Position> getPositionMap();


	/**
	 * 设置变量值
	 */
	void setVarValue(String key, String value);
}