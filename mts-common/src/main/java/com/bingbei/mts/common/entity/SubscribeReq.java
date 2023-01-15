package com.bingbei.mts.common.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author sun0x00@gmail.com
 */
@Data
public class SubscribeReq implements Serializable {

	private static final long serialVersionUID = -8669237992027524217L;

	// 代码编号相关
	private String symbol; // 代码
	private String exchange; // 交易所代码
	private String rtSymbol; // 系统中的唯一代码,通常是 合约代码.交易所代码

	// 以下为IB相关
	private String productClass; // 合约类型
	private String currency; // 合约货币
	private String expiry; // 到期日
	private double strikePrice; // 行权价
	private String optionType; // 期权类型

}
