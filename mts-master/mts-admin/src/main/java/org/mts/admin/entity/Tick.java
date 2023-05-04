package org.mts.admin.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sun0x00@gmail.com
 */
@Data
public class Tick implements Serializable {

	private static final long serialVersionUID = -2066668386737336931L;

	// 代码相关
	private String symbol; // 代码
	private String exchange; // 交易所代码
	private String tradingDay; // 交易日
	private String actionDay; // 业务发生日
	private double actionTime; // 时间(HHMMSSmmm.sss)
	private double timeStampRecv;
	private double timeStampOnEvent;

	private int status; // 状态
	private String source;//来源

	// 成交数据
	private double lastPrice = 0d; // 最新成交价
	private int lastVolume = 0; // 最新成交量
	private int volume = 0; // 今天总成交量
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
	private double bidPrice6 = 0d;
	private double bidPrice7 = 0d;
	private double bidPrice8 = 0d;
	private double bidPrice9 = 0d;
	private double bidPrice10 = 0d;

	private double askPrice1 = 0d;
	private double askPrice2 = 0d;
	private double askPrice3 = 0d;
	private double askPrice4 = 0d;
	private double askPrice5 = 0d;
	private double askPrice6 = 0d;
	private double askPrice7 = 0d;
	private double askPrice8 = 0d;
	private double askPrice9 = 0d;
	private double askPrice10 = 0d;

	private int bidVolume1 = 0;
	private int bidVolume2 = 0;
	private int bidVolume3 = 0;
	private int bidVolume4 = 0;
	private int bidVolume5 = 0;
	private int bidVolume6 = 0;
	private int bidVolume7 = 0;
	private int bidVolume8 = 0;
	private int bidVolume9 = 0;
	private int bidVolume10 = 0;

	private int askVolume1 = 0;
	private int askVolume2 = 0;
	private int askVolume3 = 0;
	private int askVolume4 = 0;
	private int askVolume5 = 0;
	private int askVolume6 = 0;
	private int askVolume7 = 0;
	private int askVolume8 = 0;
	private int askVolume9 = 0;
	private int askVolume10 = 0;

	public Tick setAllValue(String symbol, String exchange,
			String tradingDay, String actionDay,
			Double actionTime, int status, double lastPrice, int lastVolume, int volume,
			double openInterest, Long preOpenInterest, double preClosePrice, double preSettlePrice, double openPrice,
			double highPrice, double lowPrice, double upperLimit, double lowerLimit, double bidPrice1, double bidPrice2,
			double bidPrice3, double bidPrice4, double bidPrice5, double bidPrice6, double bidPrice7, double bidPrice8,
			double bidPrice9, double bidPrice10, double askPrice1, double askPrice2, double askPrice3, double askPrice4,
			double askPrice5, double askPrice6, double askPrice7, double askPrice8, double askPrice9, double askPrice10,
			int bidVolume1, int bidVolume2, int bidVolume3, int bidVolume4, int bidVolume5, int bidVolume6,
			int bidVolume7, int bidVolume8, int bidVolume9, int bidVolume10, int askVolume1, int askVolume2,
			int askVolume3, int askVolume4, int askVolume5, int askVolume6, int askVolume7, int askVolume8,
			int askVolume9, int askVolume10) {
		this.symbol = symbol;
		this.exchange = exchange;
		this.tradingDay = tradingDay;
		this.actionDay = actionDay;
		this.actionTime = actionTime;
		this.status = status;
		this.lastPrice = lastPrice;
		this.lastVolume = lastVolume;
		this.volume = volume;
		this.openInterest = openInterest;
		this.preOpenInterest = preOpenInterest;
		this.preClosePrice = preClosePrice;
		this.preSettlePrice = preSettlePrice;
		this.openPrice = openPrice;
		this.highPrice = highPrice;
		this.lowPrice = lowPrice;
		this.upperLimit = upperLimit;
		this.lowerLimit = lowerLimit;
		this.bidPrice1 = bidPrice1;
		this.bidPrice2 = bidPrice2;
		this.bidPrice3 = bidPrice3;
		this.bidPrice4 = bidPrice4;
		this.bidPrice5 = bidPrice5;
		this.bidPrice6 = bidPrice6;
		this.bidPrice7 = bidPrice7;
		this.bidPrice8 = bidPrice8;
		this.bidPrice9 = bidPrice9;
		this.bidPrice10 = bidPrice10;
		this.askPrice1 = askPrice1;
		this.askPrice2 = askPrice2;
		this.askPrice3 = askPrice3;
		this.askPrice4 = askPrice4;
		this.askPrice5 = askPrice5;
		this.askPrice6 = askPrice6;
		this.askPrice7 = askPrice7;
		this.askPrice8 = askPrice8;
		this.askPrice9 = askPrice9;
		this.askPrice10 = askPrice10;
		this.bidVolume1 = bidVolume1;
		this.bidVolume2 = bidVolume2;
		this.bidVolume3 = bidVolume3;
		this.bidVolume4 = bidVolume4;
		this.bidVolume5 = bidVolume5;
		this.bidVolume6 = bidVolume6;
		this.bidVolume7 = bidVolume7;
		this.bidVolume8 = bidVolume8;
		this.bidVolume9 = bidVolume9;
		this.bidVolume10 = bidVolume10;
		this.askVolume1 = askVolume1;
		this.askVolume2 = askVolume2;
		this.askVolume3 = askVolume3;
		this.askVolume4 = askVolume4;
		this.askVolume5 = askVolume5;
		this.askVolume6 = askVolume6;
		this.askVolume7 = askVolume7;
		this.askVolume8 = askVolume8;
		this.askVolume9 = askVolume9;
		this.askVolume10 = askVolume10;

		return this;
	}



}
