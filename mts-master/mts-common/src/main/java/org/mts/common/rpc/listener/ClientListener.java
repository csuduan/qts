package org.mts.common.rpc.listener;

import org.mts.common.model.rpc.Message;
import org.springframework.scheduling.annotation.Async;

public interface ClientListener {
    //状态变化
    @Async
    void onStatus(String id,boolean status);
    //推送消息
    void onMessage(Message msg);
}
