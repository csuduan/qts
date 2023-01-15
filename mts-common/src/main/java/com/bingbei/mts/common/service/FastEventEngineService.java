package com.bingbei.mts.common.service;

import com.bingbei.mts.common.service.extend.event.FastEvent;
import com.bingbei.mts.common.service.extend.event.FastEventDynamicHandler;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;



public interface FastEventEngineService {

	BatchEventProcessor<FastEvent> addHandler(FastEventDynamicHandler handler);

	void removeHandler(FastEventDynamicHandler handler);

	RingBuffer<FastEvent> getRingBuffer();

	void emitSimpleEvent(String eventType, String event, Object commonObj);

}