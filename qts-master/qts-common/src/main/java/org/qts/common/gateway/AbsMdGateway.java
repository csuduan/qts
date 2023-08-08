package org.qts.common.gateway;


import lombok.extern.slf4j.Slf4j;
import org.qts.common.disruptor.FastEventService;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.entity.MdInfo;
import org.qts.common.entity.trade.Tick;

@Slf4j
public abstract class AbsMdGateway implements MdGateway{

    protected MdInfo mdInfo;
    protected FastEventService fastEventService;

    public AbsMdGateway(FastEventService fastEventEngineService, MdInfo mdInfo) {
        this.fastEventService = fastEventEngineService;
        this.mdInfo=mdInfo;
        log.info("行情"+this.mdInfo.getId() + "开始初始化");
    }

    @Override
    public MdInfo getMdInfo(){
        return this.mdInfo;
    }

    @Override
    public void emitTick(Tick tick) {
        fastEventService.emitEvent(FastEvent.EVENT_TICK,tick);
    }
}
