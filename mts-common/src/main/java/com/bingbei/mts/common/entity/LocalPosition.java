package com.bingbei.mts.common.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashMap;

import static com.bingbei.mts.common.entity.Enums.POS_DIRECTION.*;


/**
 * 本地仓位
 */
@Data
@Slf4j
public class LocalPosition implements Serializable {

	private static final long serialVersionUID = 3912578572233290138L;

	public LocalPosition(String accountID,String symbol) {
	}


	private String accountID;
	private String symbol;
	private int multiple;//合约乘数


	private int longPos;
	private int longYd;
	private int longTd;
	private int longPosFrozen;
	private int longYdFrozen;
	private int longTdFrozen;
	private double longProfit;
	private double longPrice;

	private int shortPos;
	private int shortYd;
	private int shortTd;
	private int shortPosFrozen;
	private int shortYdFrozen;
	private int shortTdFrozen;
	private double shortProfit;
	private double shortPrice;
	private double lastPrice;

	private HashMap<String, Order> workingOrderMap = new HashMap<>();

	/**
	 * 成交更新
	 * 
	 * @param trade
	 */
	public void updateTrade(Trade trade) {
		if (LONG == trade.getPosDirection()) {
			// 多头
			switch (trade.getOffset()){
				case OPEN ->{
					// 开仓
					longTd += trade.getVolume();
				}
				case CLOSETD -> {
					// 平今
					longTd -= trade.getVolume();
				}
				case CLOSEYD -> {
					// 平昨
					longYd -= trade.getVolume();
				}
				case CLOSE -> {
					if (Constant.EXCHANGE_SHFE.equals(trade.getExchange())) {
						// 上期所等同于平昨
						longYd -= trade.getVolume();
					} else {
						// 非上期所,优先平今
						longTd -= trade.getVolume();
						if (longTd < 0) {
							longYd += longTd;
							longTd = 0;
						}
					}
				}
			}

		} else {
			// 空头
			switch (trade.getOffset()){
				case OPEN ->{
					// 开仓
					shortTd += trade.getVolume();
				}
				case CLOSETD -> {
					// 平今
					shortTd -= trade.getVolume();
				}
				case CLOSEYD -> {
					// 平昨
					shortYd -= trade.getVolume();
				}
				case CLOSE -> {
					if (Constant.EXCHANGE_SHFE.equals(trade.getExchange())) {
						// 上期所等同于平昨
						shortYd -= trade.getVolume();
					} else {
						// 非上期所,优先平今
						shortTd -= trade.getVolume();
						if (shortTd < 0) {
							shortYd += shortTd;
							shortTd = 0;
						}
					}
				}
			}
		}
		// 汇总今昨
		calculatePrice(trade);
		calculatePosition();
		calculateProfit();
	}

	/**
	 * 委托更新
	 * 
	 * @param order
	 */
	public void updateOrder(Order order) {
		// 将活动委托缓存下来
		if (order.isFinished()==false) {
			workingOrderMap.put(order.getOrderRef(), order);

		} else {
			// 移除缓存中已经完成的委托
			if (workingOrderMap.containsKey(order.getOrderRef())) {
				workingOrderMap.remove(order.getOrderRef());
			}
		}
		// 计算冻结
		calculateFrozen();
	}


	/**
	 * 价格更新
	 * 
	 * @param lastPrice
	 */
	public void updateLastPrice(double lastPrice) {
		this.lastPrice = lastPrice;
		calculateProfit();
	}

	/**
	 * 计算持仓盈亏
	 */
	public void calculateProfit() {
		longProfit = longPos * (lastPrice - longPrice) * multiple;
		shortProfit = shortPos * (shortPrice - lastPrice) * multiple;
	}

	/**
	 * 计算持仓均价（基于成交数据）
	 * 
	 * @param trade
	 */
	public void calculatePrice(Trade trade) {
		// 只有开仓会影响持仓均价
		if (Constant.OFFSET_OPEN.equals(trade.getOffset())) {
			double cost = 0;
			int newPos = 0;
			if (Constant.DIRECTION_LONG.equals(trade.getDirection())) {
				cost = longPrice * longPos;
				cost += trade.getVolume() * trade.getPrice();
				newPos = longPos + trade.getVolume();
				if (newPos > 0) {
					longPrice = cost / newPos;
				} else {
					longPrice = 0;
				}
			} else {
				cost = shortPrice * shortPos;
				cost += trade.getVolume() * trade.getPrice();
				newPos = shortPos + trade.getVolume();
				if (newPos > 0) {
					shortPrice = cost / newPos;
				} else {
					shortPrice = 0;
				}
			}
		}

	}

	public void calculatePosition() {
		longPos = longTd + longYd;
		shortPos = shortTd + shortYd;
	}

	/**
	 * 计算冻结
	 */
	public void calculateFrozen() {
		// 清空冻结数据
		longPosFrozen = 0;
		longYdFrozen = 0;
		longTdFrozen = 0;

		shortPosFrozen = 0;
		shortYdFrozen = 0;
		shortTdFrozen = 0;

		int frozenVolume = 0;

		// 遍历统计
		for (Order order : workingOrderMap.values()) {
			// 计算剩余冻结量
			frozenVolume = order.getTotalVolume() - order.getTradedVolume();
			if (Constant.DIRECTION_LONG.equals(order.getDirection())) {// 多头委托
				if (Constant.OFFSET_CLOSETODAY.equals(order.getOffset())) {// 平今
					shortTdFrozen += frozenVolume;
				} else if (Constant.OFFSET_CLOSEYESTERDAY.equals(order.getOffset())) {// 平昨
					shortYdFrozen += frozenVolume;
				} else if (Constant.OFFSET_CLOSE.equals(order.getOffset())) {// 平仓
					shortTdFrozen += frozenVolume;
					if (shortTdFrozen > shortTd) {
						shortYdFrozen += (shortTdFrozen - shortTd);
						shortTdFrozen = shortTd;
					}
				}
			} else if (Constant.DIRECTION_SHORT.equals(order.getDirection())) {// 空头委托
				if (Constant.OFFSET_CLOSETODAY.equals(order.getOffset())) { // 平今
					longTdFrozen += frozenVolume;
				} else if (Constant.OFFSET_CLOSEYESTERDAY.equals(order.getOffset())) { // 平昨
					longYdFrozen += frozenVolume;
				} else if (Constant.OFFSET_CLOSE.equals(order.getOffset())) {// 平仓
					longTdFrozen += frozenVolume;
					if (longTdFrozen > longTd) {
						longYdFrozen += (longTdFrozen - longTd);
						longTdFrozen = longTd;
					}
				}
			}
			// 汇总今昨冻结
			longPosFrozen = longYdFrozen + longTdFrozen;
			shortPosFrozen = shortYdFrozen + shortTdFrozen;
		}

	}

	/**
	 * 通过Position推送更新
	 * 
	 * @param position
	 */
	public void updatePosition(AccoPosition position) {
		if (Constant.DIRECTION_LONG.equals(position.getDirection())) {
			longPos = position.getPosition();
			longYd = position.getYdPosition();
			longTd = longPos - longYd;
			longProfit = position.getPositionProfit();
			longPrice = position.getAvgPrice();

		} else if (Constant.DIRECTION_SHORT.equals(position.getDirection())) {
			shortPos = position.getPosition();
			shortYd = position.getYdPosition();
			shortTd = shortPos - shortYd;
			shortProfit = position.getPositionProfit();
			shortPrice = position.getAvgPrice();
		}

	}

}
