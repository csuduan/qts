package org.qts.common.disruptor.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;
import org.qts.common.disruptor.FastEventService;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.disruptor.event.FastEventHandler;
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
public class FastEventServiceImpl implements FastEventService {
	private static ExecutorService executor = Executors.newCachedThreadPool(DaemonThreadFactory.INSTANCE);

	private final Map<EventHandler<FastEvent>, BatchEventProcessor<FastEvent>> handlerProcessorMap = new ConcurrentHashMap<>();

	private Disruptor<FastEvent> disruptor;

	private RingBuffer<FastEvent> ringBuffer;

	//@Value("${engine.event.FastEventEngine.WaitStrategy}")
	//private String waitStrategy;

	public FastEventServiceImpl(String waitStrategy) {
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
		ringBuffer = disruptor.start();
	}

	@Override
	public synchronized BatchEventProcessor<FastEvent> addHandler(FastEventHandler handler) {
		BatchEventProcessor<FastEvent> processor;
		processor = new BatchEventProcessor<FastEvent>(ringBuffer, ringBuffer.newBarrier(), handler);
		ringBuffer.addGatingSequences(processor.getSequence());
		executor.execute(processor);
		handlerProcessorMap.put(handler, processor);
		return processor;
	}

	@Override
	public void removeHandler(FastEventHandler handler) {
		if (handlerProcessorMap.containsKey(handler)) {
			BatchEventProcessor<FastEvent> processor = handlerProcessorMap.get(handler);
			// Remove a processor.
			// Stop the processor
			processor.halt();
			// Wait for shutdown the complete
			try {
				handler.awaitShutdown();
			} catch (InterruptedException e) {
				e.printStackTrace();
				log.error("关闭时发生异常", e);
			}
			// Remove the gating sequence from the ring buffer
			ringBuffer.removeGatingSequence(processor.getSequence());
			handlerProcessorMap.remove(handler);
		} else {
			log.warn("未找到Processor,无法移除");
		}

	}

	@Override
	public RingBuffer<FastEvent> getRingBuffer() {
		return ringBuffer;
	}
	@Override
	public  void  emitEvent(String eventType,Object data){
		RingBuffer<FastEvent> ringBuffer = getRingBuffer();
		long sequence = ringBuffer.next(); // Grab the next sequence
		try {
			FastEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
			fastEvent.setType(eventType);
			fastEvent.setData(data);
		} finally {
			ringBuffer.publish(sequence);
		}

	}


}