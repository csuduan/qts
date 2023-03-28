package com.bingbei.mts.common.service.extend.event;


import com.bingbei.mts.common.entity.*;
import lombok.Data;
import lombok.Getter;

@Data
public class FastEvent {

	private String eventType;
	private String event;
	private String accountId;


	// 提前new对象主要是为了性能考虑
	private Tick tick = new Tick();
	private Trade trade = new Trade();
	private Account account = new Account();
	private Bar bar = new Bar();
	private LogData logData = new LogData();
	private Contract contract = new Contract();
	private AccoPosition position = new AccoPosition();
	private Order order = new Order();
	private Object commonObj = null;
}
