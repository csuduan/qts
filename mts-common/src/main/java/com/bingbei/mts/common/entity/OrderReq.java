package com.bingbei.mts.common.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author sun0x00@gmail.com
 */
@Data
public class OrderReq implements Serializable {

	private static final long serialVersionUID = -8783647687127541104L;
	private String accountID; // 账户代码
	// 代码编号相关
	private String symbol; // 代码
	private String exchange; // 交易所代码
	private String stdSymbol; // 系统中的唯一代码,通常是 合约代码.交易所代码

	// 报单相关
	private double price; // 报单价格
	private int volume; // 报单总数量
	private String direction; // 报单方向
	private String offset; // 报单开平仓
	private String priceType; // 报单成交数量

	private String originalOrderID;// 原始ID
	private String operatorID;// 操作者ID

	// IB预留
	private String productClass; // 合约类型
	private String currency; // 合约货币
	private String expiry; // 到期日
	private double strikePrice; // 行权价
	private String optionType; // 期权类型
	private String lastTradeDateOrContractMonth; // 合约月,IB专用
	private String multiplier; // 乘数,IB专用
	
	public OrderReq() {
		this.originalOrderID = UUID.randomUUID().toString().replace("-", "").toLowerCase();
	}



}
