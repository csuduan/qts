package org.qts.common.disruptor;


import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.disruptor.event.FastEventHandler;


public interface FastEventService {

	BatchEventProcessor<FastEvent> addHandler(FastEventHandler handler);

	void removeHandler(FastEventHandler handler);

	RingBuffer<FastEvent> getRingBuffer();


	void emitEvent(String eventType,Object data);
}