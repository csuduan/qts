package com.bingbei.mts.common.gateway;

import com.bingbei.mts.common.entity.*;
import com.bingbei.mts.common.service.FastEventEngineService;
import com.bingbei.mts.common.service.extend.event.*;
import lombok.extern.slf4j.Slf4j;

import com.lmax.disruptor.RingBuffer;


import java.time.LocalDateTime;

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
	public void emitPosition(Position position) {

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
	public void emitAccount(Account account) {
		// 发送事件

		RingBuffer<FastEvent> ringBuffer = fastEventEngineService.getRingBuffer();
		long sequence = ringBuffer.next(); // Grab the next sequence
		try {
			FastEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
			fastEvent.setAccount(account);
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
