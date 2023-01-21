package com.bingbei.mts.common.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sun0x00@gmail.com
 */
@Data
public class Contract implements Serializable {

	private static final long serialVersionUID = -2126532217233428316L;

	private String accountId;
	//通用
	private String name;// 合约中文名
	private String type; // 合约类型
	private String symbol; // 合约代码
	private String exchange; // 交易所代码
	private String stdSymbol; // 标准代码,通常是 合约代码.交易所代码(大写，如CU2201.SHF)


	//期货相关
	private double priceTick; // 最小变动价位
	private double longMarginRatio; // 多头保证金率
	private double shortMarginRatio; // 空头保证金率
	private boolean maxMarginSideAlgorithm; // 最大单边保证金算法
	private int multiple;//合约乘数

	// 期权相关
	private double strikePrice; // 期权行权价
	private String underlyingSymbol; // 标的物合约代码
	private String optionType; /// 期权类型
	private String expiryDate; // 到期日

	//股票相关

	
}
