package com.bingbei.mts.common.entity;

import com.bingbei.mts.common.utils.CommonUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sun0x00@gmail.com
 */
@Data
public class Order implements Serializable {

	private static final long serialVersionUID = 7932302478961553376L;

	private static AtomicInteger orderRefGen = new AtomicInteger(CommonUtil.getRandom(10000, 555000)); // 订单编号

	private String accountID; // 账户代码
	// 代码编号相关
	private String symbol; // 代码
	private String exchange; // 交易所代码
	private String orderRef; // 订单编号
	// 报单相关
	private Enums.POS_DIRECTION direction; // 报单方向
	private Enums.OFFSET offset; // 报单开平仓
	private double price; // 报单价格
	private Enums.ORDER_TYPE priceType;//报单价格类型
	private int totalVolume; // 报单总数量
	private int tradedVolume; // 报单成交数量
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

	public Order(String accountID, String symbol, Enums.OFFSET offset, Enums.POS_DIRECTION direction, Enums.ORDER_TYPE priceType, double price, int volume){
		this.orderRef = orderRefGen.incrementAndGet()+"";
		this.accountID=accountID;
		this.symbol=symbol;
		this.offset=offset;
		this.direction=direction;
		this.price=price;
		this.totalVolume=volume;
		this.priceType=priceType;
	}

	public Order(){
		this.orderRef = orderRefGen.incrementAndGet()+"";
	}

	public boolean isFinished(){
		if(Enums.STATUS_FINISHED.contains(this.getStatus()))
			return true;
		else
			return false;
	}

}
