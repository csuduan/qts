package com.bingbei.mts.common.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * @author sun0x00@gmail.com
 */
@Data
public class Trade implements Serializable {

	private static final long serialVersionUID = -6691915458395088529L;

	// 账号代码相关
	private String accountID; // 账户代码
	// 代码编号相关
	private String symbol; // 代码
	private String exchange; // 交易所代码
	private String stdSymbol; // 系统中的唯一代码,通常是 合约代码.交易所代码

	private String tradeID; // 成交编号
	private String orderID; // 订单编号
	private String originalOrderID; // 原始订单编号

	// 成交相关
	private String direction; // 成交方向
	private String offset; // 成交开平仓
	private double price; // 成交价格
	private int volume; // 成交数量

	private String tradingDay; // 交易日
	private String tradeDate; // 业务发生日
	private String tradeTime; // 时间(HHMMSSmmm)
	private LocalDateTime dateTime;

	public Trade setAllValue(String accountID,
			String symbol, String exchange, String tradeID,
			String orderID, String direction, String offset, double price,
			int volume, String tradingDay, String tradeDate, String tradeTime, LocalDateTime dateTime) {
		this.accountID = accountID;
		this.symbol = symbol;
		this.exchange = exchange;
		this.tradeID = tradeID;
		this.orderID = orderID;
		this.direction = direction;
		this.offset = offset;
		this.price = price;
		this.volume = volume;
		this.tradingDay = tradingDay;
		this.tradeDate = tradeDate;
		this.tradeTime = tradeTime;
		this.dateTime = dateTime;
		return this;
	}

}
