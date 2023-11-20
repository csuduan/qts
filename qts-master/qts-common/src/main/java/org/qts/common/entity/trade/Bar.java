package org.qts.common.entity.trade;

import lombok.Data;

import java.io.Serializable;

@Data
public class Bar implements Serializable {

	// 代码相关
	private String level;//级别M1,M5
	private String symbol; // 代码
	private String tradingDay; // 交易日
	private String date; // 业务发生日
	private String barTime; // 时间(HHmmss)
	private String updateTime;//最新时间(HHmmss.SSS)

	private double open = 0d;
	private double high = 0d;
	private double low = 0d;
	private double close = 0d;
	private int volume = 0; // 成交量
	private double openInterest = 0d; // 持仓量

}
