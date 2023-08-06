package org.qts.common.utils;

import org.qts.common.disruptor.FastEventEngineService;
import org.qts.common.disruptor.event.EventConstant;
import org.qts.common.disruptor.event.FastEvent;
import com.lmax.disruptor.RingBuffer;


public class CoreUtil {
	private static FastEventEngineService fastEventEngineService;

	public static void setFastEventEngineService(FastEventEngineService fastEventEngineService) {
		CoreUtil.fastEventEngineService = fastEventEngineService;
	}

	/**
	 * 发出日志事件
	 *
	 * @param content
	 */
	public static void emitLogBase(long timestmap, String event, String level, String threadName, String className, String content) {
		if (fastEventEngineService == null) {
			// 事件服务可能尚未启动
			// nop 丢弃
			return;
		}
		RingBuffer<FastEvent> ringBuffer = fastEventEngineService.getRingBuffer();
		long sequence = ringBuffer.next(); // Grab the next sequence
		try {
			FastEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
			fastEvent.setEvent(event);
			fastEvent.setEventType(EventConstant.EVENT_LOG);
//			fastEvent.getLogData().setTimestamp(timestmap);
//			fastEvent.getLogData().setLevel(level);
//			fastEvent.getLogData().setThreadName(threadName);
//			fastEvent.getLogData().setClassName(className);
//			fastEvent.getLogData().setContent(content);
		} finally {
			ringBuffer.publish(sequence);
		}
	}

}