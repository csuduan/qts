package org.mts.admin.service.impl;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.mts.admin.service.FastEventEngineService;
import org.mts.admin.service.extend.event.FastEvent;
import org.mts.admin.service.extend.event.FastEventDynamicHandler;
import org.mts.admin.service.extend.event.FastEventFactory;
import org.mts.admin.utils.CoreUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



//@PropertySource(value = { "classpath:rt-core.properties" })
public class FastEventEngineServiceImpl implements FastEventEngineService {

	private static Logger log = LoggerFactory.getLogger(FastEventEngineServiceImpl.class);

	private static ExecutorService executor = Executors.newCachedThreadPool(DaemonThreadFactory.INSTANCE);

	private final Map<EventHandler<FastEvent>, BatchEventProcessor<FastEvent>> handlerProcessorMap = new ConcurrentHashMap<>();

	private Disruptor<FastEvent> disruptor;

	private RingBuffer<FastEvent> ringBuffer;

	//@Value("${engine.event.FastEventEngine.WaitStrategy}")
	//private String waitStrategy;

	public  FastEventEngineServiceImpl(String waitStrategy) {
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

		// 这一步无法自动注入
		CoreUtil.setFastEventEngineService(this);
	}

	@Override
	public synchronized BatchEventProcessor<FastEvent> addHandler(FastEventDynamicHandler handler) {
		BatchEventProcessor<FastEvent> processor;
		processor = new BatchEventProcessor<FastEvent>(ringBuffer, ringBuffer.newBarrier(), handler);
		ringBuffer.addGatingSequences(processor.getSequence());
		executor.execute(processor);
		handlerProcessorMap.put(handler, processor);
		return processor;
	}

	@Override
	public void removeHandler(FastEventDynamicHandler handler) {
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
	public void emitSimpleEvent(String eventType, String event, Object commonObj) {
		RingBuffer<FastEvent> ringBuffer = getRingBuffer();
		long sequence = ringBuffer.next(); // Grab the next sequence
		try {
			FastEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
			fastEvent.setEventType(eventType);
			fastEvent.setEvent(event);
			fastEvent.setCommonObj(commonObj);

		} finally {
			ringBuffer.publish(sequence);
		}
	}

}
