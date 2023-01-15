package com.bingbei.mts.common.gateway;


import com.bingbei.mts.common.entity.Account;
import com.bingbei.mts.common.entity.MdInfo;
import com.bingbei.mts.common.entity.Tick;
import com.bingbei.mts.common.service.FastEventEngineService;
import com.bingbei.mts.common.service.extend.event.EventConstant;
import com.bingbei.mts.common.service.extend.event.FastEvent;
import com.lmax.disruptor.RingBuffer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class MdGatewayAbstract implements MdGateway{

    protected MdInfo mdInfo;
    protected FastEventEngineService fastEventEngineService;

    public MdGatewayAbstract(FastEventEngineService fastEventEngineService, MdInfo mdInfo) {
        this.fastEventEngineService = fastEventEngineService;
        this.mdInfo=mdInfo;
        log.info("行情"+this.mdInfo.getId() + "开始初始化");
    }

    @Override
    public MdInfo getMdInfo(){
        return this.mdInfo;
    }

    @Override
    public void emitTick(Tick tick) {

        RingBuffer<FastEvent> ringBuffer = fastEventEngineService.getRingBuffer();
        long sequence = ringBuffer.next(); // Grab the next sequence
        try {
            FastEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
            fastEvent.setTick(tick);
            fastEvent.setEvent(EventConstant.EVENT_TICK);
            fastEvent.setEventType(EventConstant.EVENT_TICK);

        } finally {
            ringBuffer.publish(sequence);
        }

    }
}
