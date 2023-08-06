package org.qts.common.disruptor.event;


import org.qts.common.entity.*;
import org.qts.common.entity.Contract;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.acct.AcctPos;
import org.qts.common.entity.trade.*;

import lombok.Data;
import org.qts.common.entity.trade.Bar;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.Tick;
import org.qts.common.entity.trade.Trade;

@Data
public class FastEvent {

	private String eventType;
	private String event;
	private String accountId;


	// 提前new对象主要是为了性能考虑
	private AcctInfo acctInfo =new AcctInfo();
	private Tick tick = new Tick();
	private Trade trade = new Trade();
	private Bar bar = new Bar();
	private Contract contract = new Contract();
	private AcctPos position = new AcctPos();
	private Order order = new Order();
	private Object commonObj = null;
}
