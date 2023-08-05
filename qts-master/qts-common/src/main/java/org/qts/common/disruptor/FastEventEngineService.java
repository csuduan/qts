package org.qts.common.disruptor;


import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;
import org.fts.common.disruptor.event.*;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.disruptor.event.FastEventDynamicHandler;


public interface FastEventEngineService {

	BatchEventProcessor<FastEvent> addHandler(FastEventDynamicHandler handler);

	void removeHandler(FastEventDynamicHandler handler);

	RingBuffer<FastEvent> getRingBuffer();

	void emitSimpleEvent(String eventType, String event, Object commonObj);

}