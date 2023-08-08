package org.qts.common.entity.trade;

import lombok.Data;
import org.qts.common.entity.Enums.*;

import java.io.Serializable;

/**
 * 账户仓位
 */
@Data
public class Position implements Serializable {
	private String id;//持仓编号（合约代码-方向）
	private String acctId; //所属账户

	private String symbol; // 代码
	private String exchange; // 交易所代码
	private int multiple;//合约乘数

	// 持仓相关
	private POS_DIRECTION direction; // 持仓方向
	private int position; // 持仓量
	private int frozen; // 冻结数量
	private int ydPosition; //昨持仓
	private int ydFrozen; // 冻结数量
	private int tdPosition; // 今持仓
	private int tdFrozen; // 冻结数量
	private double useMargin; // 占用的保证金
	private double exchangeMargin; // 交易所的保证金
	private double contractValue; // 最新合约价值

	//收益相关
	private double lastPrice; // 计算盈亏使用的行情最后价格
	private double avgPrice; // 持仓均价
	private double priceDiff; // 持仓价格差
	private double openPrice; // 开仓均价
	private double openPriceDiff; // 开仓价格差
	private double positionProfit; // 持仓盈亏
	private double positionProfitRatio; // 持仓盈亏率
	private double openPositionProfit; // 开仓盈亏
	private double openPositionProfitRatio; // 开仓盈亏率
}
