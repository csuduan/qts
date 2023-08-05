package org.qts.common.gateway;

import lombok.extern.slf4j.Slf4j;

import com.lmax.disruptor.RingBuffer;
import org.qts.common.disruptor.FastEventEngineService;
import org.qts.common.disruptor.event.EventConstant;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.entity.Contract;
import org.qts.common.entity.LoginInfo;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.acct.AcctPos;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.Trade;

/**
 * @author sun0x00@gmail.com
 */
@Slf4j
public abstract class TdGatewayAbstract implements TdGateway {

	protected LoginInfo loginInfo;
	protected FastEventEngineService fastEventEngineService;

	public TdGatewayAbstract(FastEventEngineService fastEventEngineService, LoginInfo loginInfo) {
		this.fastEventEngineService = fastEventEngineService;
		this.loginInfo=loginInfo;
		log.info(this.loginInfo.getAccoutId() + "开始初始化");
	}

	@Override
	public LoginInfo getLoginInfo() {
		return loginInfo;
	}

	@Override
	public void emitPosition(AcctPos position) {

		RingBuffer<FastEvent> ringBuffer = fastEventEngineService.getRingBuffer();
		long sequence = ringBuffer.next(); // Grab the next sequence
		try {
			FastEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
			fastEvent.setPosition(position);
			fastEvent.setEvent(EventConstant.EVENT_POSITION);
			fastEvent.setEventType(EventConstant.EVENT_POSITION);

		} finally {
			ringBuffer.publish(sequence);
		}
	}

	@Override
	public void emitAccount(AcctInfo account) {
		// 发送事件

		RingBuffer<FastEvent> ringBuffer = fastEventEngineService.getRingBuffer();
		long sequence = ringBuffer.next(); // Grab the next sequence
		try {
			FastEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
			fastEvent.setAcctInfo(account);
			fastEvent.setEvent(EventConstant.EVENT_ACCOUNT);
			fastEvent.setEventType(EventConstant.EVENT_ACCOUNT);
		} finally {
			ringBuffer.publish(sequence);
		}

	}

	@Override
	public void emitContract(Contract contract) {

		// 发送事件

		RingBuffer<FastEvent> ringBuffer = fastEventEngineService.getRingBuffer();
		long sequence = ringBuffer.next(); // Grab the next sequence
		try {
			FastEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
			fastEvent.setContract(contract);
			fastEvent.setEvent(EventConstant.EVENT_CONTRACT);
			fastEvent.setEventType(EventConstant.EVENT_CONTRACT);
		} finally {
			ringBuffer.publish(sequence);
		}

	}






	@Override
	public void emitTrade(Trade trade) {

		// 发送特定合约成交事件
		RingBuffer<FastEvent> ringBuffer = fastEventEngineService.getRingBuffer();
		long sequence = ringBuffer.next(); // Grab the next sequence
		try {
			FastEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
			fastEvent.setTrade(trade);
			fastEvent.setEvent(EventConstant.EVENT_TRADE);
			fastEvent.setEventType(EventConstant.EVENT_TRADE);

		} finally {
			ringBuffer.publish(sequence);
		}

	}



	@Override
	public void emitOrder(Order order) {
		// 发送带委托ID的事件
		RingBuffer<FastEvent> ringBuffer = fastEventEngineService.getRingBuffer();
		long sequence = ringBuffer.next(); // Grab the next sequence

		try {
			FastEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
			fastEvent.setOrder(order);
			fastEvent.setEvent(EventConstant.EVENT_ORDER);
			fastEvent.setEventType(EventConstant.EVENT_ORDER);

		} finally {
			ringBuffer.publish(sequence);
		}

	}

}
