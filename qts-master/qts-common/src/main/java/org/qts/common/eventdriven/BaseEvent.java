package org.qts.common.eventdriven;

public class BaseEvent {
    //是否在消息主线程同步执行
    private boolean sync = true;

    //事件类型
    private final EventType evtType;

    public BaseEvent (EventType evtType) {
        this.evtType = evtType;
    }

    public BaseEvent (EventType evtType,boolean sync) {
        this.evtType = evtType;
        this.sync = sync;
    }

    public EventType getEvtType() {
        return evtType;
    }

    /**
     * 是否在消息主线程同步执行
     * @return
     */
    public boolean isSync() {
        return sync;
    }

    public void setSync (boolean sync) {
        this.sync = sync;
    }

}
