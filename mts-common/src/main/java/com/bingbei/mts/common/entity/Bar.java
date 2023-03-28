package com.bingbei.mts.common.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author sun0x00@gmail.com
 */
@Data
public class Bar implements Serializable {

	private static final long serialVersionUID = 9166305799616198661L;

	// 代码相关
	private String level;//级别M1,M5
	private String symbol; // 代码
	private String tradingDay; // 交易日
	private String actionDay; // 业务发生日
	private long   barTime; // 时间(HHmmss)
	private double actionTime;//最新时间(HHmmss.SSS)
	//private LocalDateTime dateTime;
	private double open = 0d;
	private double high = 0d;
	private double low = 0d;
	private double close = 0d;

	private int volume = 0; // 成交量
	private double openInterest = 0d; // 持仓量

}
