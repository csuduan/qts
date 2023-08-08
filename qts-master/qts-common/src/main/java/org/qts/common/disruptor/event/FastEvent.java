package org.qts.common.disruptor.event;


import org.qts.common.entity.Contract;
import org.qts.common.entity.trade.Position;

import lombok.Data;
import org.qts.common.entity.trade.Bar;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.Tick;
import org.qts.common.entity.trade.Trade;

@Data
public class FastEvent {
	public final static String EVENT_TICK = "E_TICK";
	public final static String EVENT_TICKS = "E_TICKS";
	public final static String EVENT_TRADE = "E_TRADE";
	public final static String EVENT_ORDER = "E_ORDER";
	public final static String EVENT_POSITION = "E_POSITION";
	public final static String EVENT_ACCT = "E_ACCT";
	public final static String EVENT_CONTRACT = "E_CONTRACT";
	public final static String EVENT_ERROR = "E_ERROR";
	public final static String EVENT_GATEWAY = "E_GATEWAY";
	public final static String EVENT_LOG = "E_LOG";

	private String type;
	private Object data = null;

	public <T> T getData(Class claz){
		return (T)data;
	}

}
