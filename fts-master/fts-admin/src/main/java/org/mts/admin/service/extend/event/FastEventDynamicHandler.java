package org.mts.admin.service.extend.event;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;

import java.util.List;
import java.util.Set;

public interface FastEventDynamicHandler extends EventHandler<FastEvent>, LifecycleAware {
	void awaitShutdown() throws InterruptedException;

	List<String> getSubscribedEventList();

	Set<String> getSubscribedEventSet();

	void subscribeEvent(String event);

	void unsubscribeEvent(String event);
}
