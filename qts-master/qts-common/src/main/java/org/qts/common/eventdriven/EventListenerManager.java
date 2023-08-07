package org.qts.common.eventdriven;

public class EventListenerManager extends AbsEventListenerManager{
    /**
     * 在构造器中调用父类的initEventListener，完成下方被注解修饰的所有事件监听器自动注册到EventDispatcher
     */
    public EventListenerManager() {
        super.initEventListener();
    }

    /**
     * 通过@EventAnnotation定义该事件监听器感兴趣的事件类型
     */
    //@EventAnnotation(eventType=EventType.LOGIN)
    //public ExampleEventListener exampleEvent;

    //这里继续添加其他事件监听器
    //@Evt(eventType=EventType.EXIT)
    //public ExampleEventListener exampleEvent2;
}
