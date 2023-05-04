package org.mts.admin.utils;

import com.lmax.disruptor.RingBuffer;
import org.mts.admin.service.FastEventEngineService;
import org.mts.admin.service.extend.event.EventConstant;
import org.mts.admin.service.extend.event.FastEvent;
import org.mts.admin.service.impl.FastEventEngineServiceImpl;


public class CoreUtil {
	private static FastEventEngineService fastEventEngineService;

	public static void setFastEventEngineService(FastEventEngineServiceImpl fastEventEngineService) {
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
			fastEvent.getLogData().setTimestamp(timestmap);
			fastEvent.getLogData().setLevel(level);
			fastEvent.getLogData().setThreadName(threadName);
			fastEvent.getLogData().setClassName(className);
			fastEvent.getLogData().setContent(content);
		} finally {
			ringBuffer.publish(sequence);
		}
	}

}
