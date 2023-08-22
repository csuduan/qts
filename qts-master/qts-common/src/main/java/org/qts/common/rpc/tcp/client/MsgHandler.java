package org.qts.common.rpc.tcp.client;

import org.qts.common.entity.Message;
import org.springframework.scheduling.annotation.Async;

public interface MsgHandler {
    //推送消息
    void onMessage(Message msg);
}
