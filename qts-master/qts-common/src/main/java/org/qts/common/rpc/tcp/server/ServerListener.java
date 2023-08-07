package org.qts.common.rpc.tcp.server;

import org.qts.common.entity.Message;

public interface ServerListener {
    Message onRequest(Message req);
    void onStatus(String acctId, Boolean connected);
}
