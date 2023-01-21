package com.bingbei.mts.common.entity;

import com.bingbei.mts.common.utils.CommonUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sun0x00@gmail.com
 */
@Data
public class Order implements Serializable {

	private static final long serialVersionUID = 7932302478961553376L;

	private static AtomicInteger orderRefGen = new AtomicInteger(CommonUtil.getRandom(10000, 555000)); // 订单编号

	private String accountID; // 账户代码
	// 代码编号相关
	private String symbol; // 代码
	private String exchange; // 交易所代码
	private String orderRef; // 订单编号
	// 报单相关
	private String direction; // 报单方向
	private String offset; // 报单开平仓
	private double price; // 报单价格
	private String priceType;//报单价格类型
	private int totalVolume; // 报单总数量
	private int tradedVolume; // 报单成交数量
	private String status; // 报单状态
	private String tradingDay;
	private String orderDate; // 发单日期
	private String orderTime; // 发单时间
	private String cancelTime; // 撤单时间
	private String activeTime; // 激活时间
	private String updateTime; // 最后修改时间

	// CTP/LTS相关
	private int frontID; // 前置机编号
	private int sessionID; // 连接编号

	public Order setAllValue(String accountID, String symbol, String exchange, String orderID,
			String direction, String offset, double price, int totalVolume, int tradedVolume,
			String status, String tradingDay, String orderDate, String orderTime, String cancelTime, String activeTime,
			String updateTime, int frontID, int sessionID) {
		this.accountID = accountID;
		this.symbol = symbol;
		this.exchange = exchange;
		this.orderRef = orderID;
		this.direction = direction;
		this.offset = offset;
		this.price = price;
		this.totalVolume = totalVolume;
		this.tradedVolume = tradedVolume;
		this.status = status;
		this.tradingDay = tradingDay;
		this.orderDate = orderDate;
		this.orderTime = orderTime;
		this.cancelTime = cancelTime;
		this.activeTime = activeTime;
		this.updateTime = updateTime;
		this.frontID = frontID;
		this.sessionID = sessionID;
		return this;
	}
	public Order(){
		this.orderRef = orderRefGen.incrementAndGet()+"";
	}

}
