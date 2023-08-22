package org.qts.common.rpc.tcp.server;

import org.qts.common.entity.Message;

public interface MsgHandler {
    Message onRequest(Message req);
}
