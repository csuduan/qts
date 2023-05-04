package org.mts.admin.service;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;
import org.mts.admin.service.extend.event.FastEvent;
import org.mts.admin.service.extend.event.FastEventDynamicHandler;


public interface FastEventEngineService {

	BatchEventProcessor<FastEvent> addHandler(FastEventDynamicHandler handler);

	void removeHandler(FastEventDynamicHandler handler);

	RingBuffer<FastEvent> getRingBuffer();

	void emitSimpleEvent(String eventType, String event, Object commonObj);

}