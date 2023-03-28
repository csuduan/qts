package com.bingbei.mts.trade.strategy;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.bingbei.mts.common.entity.*;
import com.bingbei.mts.common.utils.BarGenerator;
import com.bingbei.mts.trade.engine.TradeExecutor;
import lombok.extern.slf4j.Slf4j;


/**
 * 策略基本实现抽象类
 * 
 * @author sun0x00@gmail.com
 *
 */
@Slf4j
public abstract class StrategyAbstract implements Strategy {
	protected String logStr;
	protected Map<String, String> varMap = new HashMap<>(); // 运行时可变参数字典
	protected boolean pauseOpen = false;//暂停开仓
	protected boolean pauseClose = false;//暂停平仓
	protected boolean tradingStatus = false;

	// 交易执行器
	protected TradeExecutor tradeExecutor;
	// 策略配置
	protected StrategySetting strategySetting;
	// 策略仓位
	protected Map<String, LocalPosition> positionMap = new HashMap<>();
	/**
	 * 必须使用有参构造方法
	 * 
	 * @param
	 * @param strategySetting
	 */
	public StrategyAbstract(TradeExecutor tradeExecutor,StrategySetting strategySetting) {
		this.strategySetting = strategySetting;
		this.tradeExecutor = tradeExecutor;
		this.logStr = "策略ID-[" + strategySetting.getStrategyId() + "] >>> ";
	}

	/**
	 * 初始化策略
	 */
	@Override
	public void init() {
		log.info("初始化！");
		log.info("=================ParamMap=============================");
		log.info(JSON.toJSONString(strategySetting.getParamMap()));
		log.info("======================================================");
		this.positionMap.clear();
		//订阅合约行情
		this.strategySetting.getContracts().forEach(x->{
			this.tradeExecutor.subContract(x,this);
		});

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


	public void sendOrder(String symbol, Enums.OFFSET offset, Enums.POS_DIRECTION direction, Enums.ORDER_TYPE priceType, double price, int volume) {

		Order orderReq = new Order(this.strategySetting.getAccountId(),symbol,offset,direction,priceType,price,volume);
		this.tradeExecutor.insertOrder(orderReq);
		if (!positionMap.containsKey(symbol)) {
			positionMap.put(symbol,new LocalPosition(this.strategySetting.getAccountId(),symbol));
		}

		LocalPosition position=positionMap.get(symbol);
		position.updateOrder(orderReq);
		this.tradeExecutor.insertOrder(orderReq);
		//报单后需要立即刷新一次仓位
		position.updateOrder(orderReq);
	}
	public void open(Enums.POS_DIRECTION direction, String symbol, Enums.ORDER_TYPE priceType, double price, int volume){
		this.sendOrder(symbol,Enums.OFFSET.OPEN,direction,priceType,price,volume);
	}
	public void close(Enums.POS_DIRECTION direction, String symbol, Enums.ORDER_TYPE priceType, double price, int volume){
		this.sendOrder(symbol,Enums.OFFSET.CLOSE,direction,priceType,price,volume);

	}
	public  void closeTd(Enums.POS_DIRECTION direction, String symbol, Enums.ORDER_TYPE priceType, double price, int volume){
		this.sendOrder(symbol,Enums.OFFSET.CLOSETD,direction,priceType,price,volume);
	}

	public void cancelOrder(String orderRef) {
		this.tradeExecutor.cancelOrder(orderRef);

	}

	private Map<String, BarGenerator> barGeneratorMap = new HashMap<>();

	/**
	 * 保存持仓
	 */
	public void savePosition() {

	}

	@Override
	public void onTick(Tick tick) throws Exception{
		// 刷新本地持仓盈亏
		for (LocalPosition localPositionDetail : this.positionMap.values()) {
			if (localPositionDetail.getSymbol().equals(tick.getSymbol())) {
				localPositionDetail.updateLastPrice(tick.getLastPrice());
			}
		}
	}

	@Override
	public void onTrade(Trade trade) throws  Exception{
		if (positionMap.containsKey(trade.getSymbol())) {
			LocalPosition contractPositionDetail = positionMap.get(trade.getSymbol());
			contractPositionDetail.updateTrade(trade);
			savePosition();
		}else {
			log.warn("{} 合约[{}]的预配置不存在,不会更新数据库持仓！", logStr, trade.getSymbol());
		}
	}

	@Override
	public void onOrder(Order order) throws  Exception{
		if (positionMap.containsKey(order.getSymbol())) {
			LocalPosition contractPositionDetail = positionMap.get(order.getSymbol());
			contractPositionDetail.updateOrder(order);
		}else {
			log.warn("{} 合约[{}]的预配置不存在,不会更新数据库持仓！", logStr, order.getSymbol());
		}
		//onOrder(order);
	}

	@Override
	public void pauseOpen() {

	}

	@Override
	public void pasueClose() {

	}

	@Override
	public void startTrading() {

	}

	@Override
	public void stopTrading() {

	}

	@Override
	public Map<String, LocalPosition> getPositionMap() {
		return this.positionMap;
	}




}
