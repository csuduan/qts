package org.qts.common.disruptor.event;


import lombok.Data;

@Data
public class FastEvent {
	public final static String EV_TICK = "E_TICK";
	public final static String EV_TRADE = "E_TRADE";
	public final static String EV_ORDER = "E_ORDER";
	public final static String EV_POSITION = "E_POSITION";
	public final static String EV_ACCT = "E_ACCT";
	public final static String EV_CONTRACT = "E_CONTRACT";
	public final static String EV_ERROR = "E_ERROR";

	private String type;
	private Object data = null;

	public <T> T getData(Class claz){
		return (T)data;
	}

}
