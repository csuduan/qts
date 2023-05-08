package org.mts.common.ipc.handler;

public interface CustomHandler {
    void onStatus(boolean status);
    void onData(Object data);
}
