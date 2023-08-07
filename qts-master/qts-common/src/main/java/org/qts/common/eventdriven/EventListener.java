package org.qts.common.eventdriven;

public interface EventListener<E> {
    public void handleEvent(E event);
}
