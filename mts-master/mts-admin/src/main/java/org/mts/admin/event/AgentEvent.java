package org.mts.admin.event;

import com.lmax.disruptor.AggregateEventHandler;
import lombok.Data;
import org.mts.admin.entity.sys.Agent;
import org.springframework.context.ApplicationEvent;

@Data
public class AgentEvent extends ApplicationEvent {
    private Agent agent;
    public AgentEvent(Object source, Agent agent) {
        super(source);
        this.agent=agent;
    }

}
