package org.qts.common.entity.trade;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderCancelReq implements Serializable {
	// 代码编号相关
	private String symbol; // 代码
	private String exchange; // 交易所代码

	//可以用orderSysId或者frontId+sessionId+orderRef;
	private String orderSysID; // 报单号

	private String orderRef;
	private int frontId;
	private int sessionId;
}
