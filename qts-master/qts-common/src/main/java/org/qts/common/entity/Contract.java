package org.qts.common.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 合约信息
 */
@Data
public class Contract implements Serializable {
	//通用
	private String id; // 合约编号,通常是 合约代码.交易所代码(如CU2201.SHF)
	private String type; //合约类型(期货-F、股票-S、期权-O)
	private String symbol; //合约代码
	private String exchange; //交易所代码


	//期货相关
	private int multiple = 1;//合约乘数(股票为1)
	private double priceTick; // 最小变动价位
	private double longMarginRatio; // 多头保证金率
	private double shortMarginRatio; // 空头保证金率

	// 期权相关
	private double strikePrice; // 期权行权价
	private String underlyingSymbol; // 标的物合约代码
	private String optionType; /// 期权类型
	private String expiryDate; // 到期日

	//股票相关
	public Contract(String symbol,String exchange){
		this.id=symbol.toUpperCase()+"."+exchange.toUpperCase();
		this.symbol=symbol;
		this.exchange=exchange;
	}
	public Contract(){
	}
}
