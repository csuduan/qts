package org.qts.common.rpc.future;

import org.qts.common.entity.Message;

import java.util.concurrent.Future;

public interface WriteFuture<T> extends Future<T> {

    Throwable cause();

    void setCause(Throwable cause);

    boolean isWriteSuccess();

    void setWriteResult(boolean result);

    String requestId();

    T response();

    void setResponse(Message response);

    boolean isTimeout();


}

