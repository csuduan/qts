package org.qts.common.entity.trade;

import lombok.Data;
import org.qts.common.entity.Enums;

import java.io.Serializable;


/**
 * @author sun0x00@gmail.com
 */
@Data
public class Trade implements Serializable {
	// 代码编号相关
	private String symbol; // 代码
	private String exchange; // 交易所代码
	private String tradeID; // 成交编号
	private String orderRef;
	private String orderSysID;

	// 成交相关
	private Enums.TRADE_DIRECTION direction; //成交方向
	private Enums.OFFSET offset; // 成交开平仓
	private double price; // 成交价格
	private int volume; // 成交数量

	private String tradingDay; // 交易日
	private String tradeDate; // 业务发生日
	private String tradeTime; // 时间(HHMMSSmmm)

	public Enums.POS_DIRECTION getPosDirection(){
		if(offset == Enums.OFFSET.OPEN)
			return direction== Enums.TRADE_DIRECTION.BUY ? Enums.POS_DIRECTION.LONG: Enums.POS_DIRECTION.SHORT;
		else
			return direction== Enums.TRADE_DIRECTION.SELL ? Enums.POS_DIRECTION.LONG: Enums.POS_DIRECTION.SHORT;

	}
}
