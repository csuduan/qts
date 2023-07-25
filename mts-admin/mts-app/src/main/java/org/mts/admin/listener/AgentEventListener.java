package org.mts.admin.listener;

public interface AgentEventListener {
    void onAgentEvent(String eventType,Object eventData);
}
