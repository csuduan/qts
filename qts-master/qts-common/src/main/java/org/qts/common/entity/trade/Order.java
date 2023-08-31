package org.qts.common.entity.trade;

import lombok.Data;
import org.qts.common.entity.Enums;
import org.qts.common.utils.CommonUtil;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 报单
 */
@Data
public class Order implements Serializable {

	//private static AtomicInteger orderRefGen = new AtomicInteger(CommonUtil.getRandom(10000, 555000)); // 订单编号
	private static AtomicInteger orderRefGen = new AtomicInteger(LocalTime.now().toSecondOfDay()); // 订单编号
	//代码编号相关
	private String symbol; // 代码
	private String exchange; // 交易所代码
	private String orderRef; // 订单编号
	private String OrderSysID;
	// 报单相关
	private Enums.TRADE_DIRECTION direction; // 报单方向
	private Enums.OFFSET offset; // 报单开平仓
	private Enums.PRICE_TYPE priceType;//报单价格类型
	private double price; // 报单价格
	private int totalVolume; // 报单总数量
	private int tradedVolume; // 报单成交数量
	private int lastTradedVolume = 0; // 上一次成交数量
	private Enums.ORDER_STATUS status; // 报单状态
	private String statusMsg;
	private String tradingDay;
	private String orderDate; // 发单日期
	private String orderTime; // 发单时间
	private String cancelTime; // 撤单时间
	private String activeTime; // 激活时间
	private String updateTime; // 最后修改时间
	private boolean canceling = false;//测单中

	// CTP/LTS相关
	private int frontID; // 前置机编号
	private int sessionID; // 连接编号
	private String acctId;

	public Order(String symbol, Enums.OFFSET offset, Enums.TRADE_DIRECTION direction,  int volume,double price){
		this.orderRef = orderRefGen.incrementAndGet()+"";
		this.symbol=symbol;
		this.offset=offset;
		this.direction=direction;
		this.price=price;
		this.totalVolume=volume;
		this.status = Enums.ORDER_STATUS.UNKNOWN;
	}

	public Order(){
	}

	public boolean isFinished(){
		if(Enums.STATUS_FINISHED.contains(this.getStatus()))
			return true;
		else
			return false;
	}

}
