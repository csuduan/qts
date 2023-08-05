package org.qts.common.disruptor.event;

import com.lmax.disruptor.EventFactory;

public class FastEventFactory implements EventFactory<FastEvent> {

	@Override
	public FastEvent newInstance() {
		return new FastEvent();
	}

}
