package org.qts.common.entity.trade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Tick implements Serializable {
	// 代码相关
	private String symbol; // 代码
	private String exchange; // 交易所代码
	private String tradingDay; // 交易日
	private String updateTime;
	private String date; //自然日
	private long  times;//yyyymmddHHMMss //行情时间戳

	// 成交数据
	private double lastPrice = 0d; // 最新成交价
	private long lastVolume = 0; // 最新成交量
	private long volume = 0; // 今天总成交量
	private double openInterest = 0d; // 持仓量

	private Long preOpenInterest = 0L;// 昨持仓
	private double preClosePrice = 0d; // 前收盘价
	private double preSettlePrice = 0d; // 昨结算

	// 常规行情
	private double openPrice = 0d; // 今日开盘价
	private double highPrice = 0d; // 今日最高价
	private double lowPrice = 0d; // 今日最低价

	private double upperLimit = 0d; // 涨停价
	private double lowerLimit = 0d; // 跌停价

	private double bidPrice1 = 0d;
	private double bidPrice2 = 0d;
	private double bidPrice3 = 0d;
	private double bidPrice4 = 0d;
	private double bidPrice5 = 0d;
//	private double bidPrice6 = 0d;
//	private double bidPrice7 = 0d;
//	private double bidPrice8 = 0d;
//	private double bidPrice9 = 0d;
//	private double bidPrice10 = 0d;

	private double askPrice1 = 0d;
	private double askPrice2 = 0d;
	private double askPrice3 = 0d;
	private double askPrice4 = 0d;
	private double askPrice5 = 0d;
//	private double askPrice6 = 0d;
//	private double askPrice7 = 0d;
//	private double askPrice8 = 0d;
//	private double askPrice9 = 0d;
//	private double askPrice10 = 0d;

	private long bidVolume1 = 0;
	private long bidVolume2 = 0;
	private long bidVolume3 = 0;
	private long bidVolume4 = 0;
	private long bidVolume5 = 0;
//	private long bidVolume6 = 0;
//	private long bidVolume7 = 0;
//	private long bidVolume8 = 0;
//	private long bidVolume9 = 0;
//	private long bidVolume10 = 0;

	private long askVolume1 = 0;
	private long askVolume2 = 0;
	private long askVolume3 = 0;
	private long askVolume4 = 0;
	private long askVolume5 = 0;
//	private long askVolume6 = 0;
//	private long askVolume7 = 0;
//	private long askVolume8 = 0;
//	private long askVolume9 = 0;
//	private long askVolume10 = 0;

	private double timeStampRecv;
	private double timeStampOnEvent;
}
