package org.qts.trader.strategy;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.qts.common.entity.Enums;
import org.qts.common.entity.trade.*;
import org.qts.trader.core.AcctExecutor;
import org.qts.trader.core.StrategyEngine;


/**
 * 策略基本实现抽象类
 *
 *
 */
@Slf4j
public abstract class AbsStrategy implements Strategy {
	protected Map<String, String> varMap = new HashMap<>(); // 运行时可变参数字典
	protected boolean pauseOpen = false;//暂停开仓
	protected boolean pauseClose = false;//暂停平仓
	protected boolean tradingStatus = false;

	// 策略配置
	protected StrategySetting strategySetting;
	protected  StrategyEngine strategyEngine;
	// 策略仓位
	protected Map<String, Position> positionMap = new HashMap<>();

	@Override
	public void init(StrategyEngine strategyEngine,StrategySetting strategySetting) {
		this.strategySetting = strategySetting;
		this.strategyEngine = strategyEngine;
		log.info("Strategy-{} init ...",strategySetting.getStrategyId());
		log.info("Strategy-{} parmas: {}",strategySetting.getParamMap());
		this.strategyEngine.subs(this.strategySetting.getContracts());
	}

	/**
	 * 销毁通知，一般用于重新加载策略
	 */
	@Override
	public void destroy() {
	}


	@Override
	public boolean isTrading() {
		return tradingStatus;
	}

	@Override
	public StrategySetting getStrategySetting() {
		return strategySetting;
	}


	@Override
	public void setVarValue(String key, String value) {
		this.varMap.put(key, value);
	}


	public void sendOrder(String symbol, Enums.OFFSET offset, Enums.TRADE_DIRECTION direction,  double price, int volume) {
		Order orderReq = new Order(symbol,offset,direction,volume,price);
		Enums.POS_DIRECTION posDir=orderReq.getPosDirection();
		if (offset== Enums.OFFSET.OPEN && !positionMap.containsKey(symbol+posDir) ) {
			Position pos =new Position(symbol,posDir);
			positionMap.put(pos.getId(),pos);
		}
		Position pos = positionMap.get(symbol+posDir);
		this.strategyEngine.insertOrder(orderReq);
		//报单后需要立即刷新一次仓位
		pos.update(orderReq);
	}

	public boolean open(String symbol,  Enums.TRADE_DIRECTION direction,  double price, int volume){
		Order orderReq = new Order(symbol, Enums.OFFSET.OPEN,direction,volume,price);
		Enums.POS_DIRECTION posDir=orderReq.getPosDirection();
		if (positionMap.containsKey(symbol+posDir) ) {
			Position pos =new Position(symbol,posDir);
			positionMap.put(pos.getId(),pos);
		}
		Position pos = positionMap.get(symbol+posDir);
		this.strategyEngine.insertOrder(orderReq);
		//报单后需要立即刷新一次仓位
		pos.update(orderReq);
		return true;
	}

	public boolean close(String symbol,  Enums.TRADE_DIRECTION direction,  double price, int volume){
		Order orderReq = new Order(symbol, Enums.OFFSET.CLOSE,direction,volume,price);
		Enums.POS_DIRECTION posDir=orderReq.getPosDirection();
		Position pos = positionMap.get(symbol+posDir);
		if(pos!=null){
			this.strategyEngine.insertOrder(orderReq);
			//报单后需要立即刷新一次仓位
			pos.update(orderReq);
			return true;
		}else{
			log.info("找不到对应的持仓,{} {}",symbol,posDir);
			return false;
		}
	}

	public void cancelOrder(Order order) {
		this.strategyEngine.cancel(order);
	}


	@Override
	public void onTrade(Trade trade) throws  Exception{
		if (positionMap.containsKey(trade.getSymbol()+trade.getPosDirection())) {
			Position pos = positionMap.get(trade.getSymbol()+trade.getPosDirection());
			pos.getTradeList().add(trade);
		}
	}

	@Override
	public void onOrder(Order order) throws  Exception{
		if (positionMap.containsKey(order.getSymbol()+order.getPosDirection())) {
			Position pos = positionMap.get(order.getSymbol()+order.getPosDirection());
			pos.update(order);
		}
	}

	@Override
	public void pauseOpen(boolean enable) {
		this.pauseOpen = enable;
	}

	@Override
	public void pasueClose(boolean enable) {
		this.pauseClose = enable;
	}

	@Override
	public void startTrading() {

	}

	@Override
	public void stopTrading() {

	}

	@Override
	public Map<String, Position> getPositionMap() {
		return this.positionMap;
	}


	public Object getParam(String key){
		return strategySetting.getParamMap().get(key);
	}

	@Override
	public void onBar(Bar bar) throws Exception {

	}


}
