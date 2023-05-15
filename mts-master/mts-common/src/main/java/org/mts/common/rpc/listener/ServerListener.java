package org.mts.common.rpc.listener;

import org.mts.common.model.rpc.Message;

public interface ServerListener {
    Message onRequest(Message req);
}
