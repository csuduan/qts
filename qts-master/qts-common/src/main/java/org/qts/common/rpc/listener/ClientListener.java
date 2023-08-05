package org.qts.common.rpc.listener;

import org.qts.common.entity.Message;
import org.springframework.scheduling.annotation.Async;

public interface ClientListener {
    //状态变化
    @Async
    void onStatus(String id,boolean status);
    //推送消息
    void onMessage(Message msg);
}
