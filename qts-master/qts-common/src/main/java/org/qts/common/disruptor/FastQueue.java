package org.qts.common.disruptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.disruptor.event.FastEventFactory;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;



//@PropertySource(value = { "classpath:rt-core.properties" })
@Slf4j
public class FastQueue {
	private final Map<EventHandler<FastEvent>, BatchEventProcessor<FastEvent>> handlerProcessorMap = new ConcurrentHashMap<>();

	private Disruptor<FastEvent> disruptor;

	private RingBuffer<FastEvent> ringBuffer;

	private List<EventHandler<FastEvent>> eventHandlers=new ArrayList<>();

	//@Value("${engine.event.FastEventEngine.WaitStrategy}")
	//private String waitStrategy;

	public FastQueue(String waitStrategy,EventHandler<FastEvent> eventEventHandler) {
		log.info("fastEventEngine start,{}",waitStrategy);
		if ("BusySpinWaitStrategy".equals(waitStrategy)) {
			disruptor = new Disruptor<FastEvent>(new FastEventFactory(), 65536, DaemonThreadFactory.INSTANCE,
					ProducerType.SINGLE, new BusySpinWaitStrategy());
		} else if ("SleepingWaitStrategy".equals(waitStrategy)) {
			disruptor = new Disruptor<FastEvent>(new FastEventFactory(), 65536, DaemonThreadFactory.INSTANCE,
					ProducerType.SINGLE, new SleepingWaitStrategy());
		} else if ("BlockingWaitStrategy".equals(waitStrategy)) {
			disruptor = new Disruptor<FastEvent>(new FastEventFactory(), 65536, DaemonThreadFactory.INSTANCE,
					ProducerType.SINGLE, new BlockingWaitStrategy());
		} else {
			disruptor = new Disruptor<FastEvent>(new FastEventFactory(), 65536, DaemonThreadFactory.INSTANCE,
					ProducerType.SINGLE, new YieldingWaitStrategy());
		}
		disruptor.handleEventsWith(eventEventHandler);
		ringBuffer = disruptor.start();
//		BatchEventProcessor<FastEvent> processor;
//		processor = new BatchEventProcessor<FastEvent>(ringBuffer, ringBuffer.newBarrier(), handler);
//		ringBuffer.addGatingSequences(processor.getSequence());
//		executor.execute(processor);
	}

	public  void  emitEvent(String eventType,Object data){
		long sequence = this.ringBuffer.next(); // Grab the next sequence
		try {
			FastEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
			fastEvent.setType(eventType);
			fastEvent.setData(data);
			ringBuffer.publish(sequence);

		} catch (Exception ex){
			log.error("ringBuffer publish error",ex);
		}

	}
	public void close(){
		this.disruptor.shutdown();
	}


}
