package org.mts.admin.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sun0x00@gmail.com
 */
@Data
public class CancelOrderReq implements Serializable {
	private static final long serialVersionUID = -8268383961926962032L;

	// 代码编号相关
	private String symbol; // 代码
	private String exchange; // 交易所代码
	private String stdSymbol; // 系统中的唯一代码,通常是 合约代码.交易所代码

	private String operatorID;// 操作者ID
	private String orderID; // 报单号

	// CTP LTS网关相关
	private int frontID;
	private int sessionID;
}
