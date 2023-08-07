package org.qts.trader.engine;

import org.qts.common.disruptor.FastEventEngineService;
import org.qts.common.entity.trade.Tick;
import org.qts.common.gateway.MdGateway;
import lombok.Data;
import org.qts.gateway.GatwayFactory;

import java.util.HashMap;
import java.util.Map;

@Data
public class EngineContext {
    //private StrategyEngine strategyEngine;
    private GatwayFactory gatwayFactory;

    private FastEventEngineService fastEventEngineService;

    //最新行情
    private Map<String, Tick> lastTickMap = new HashMap<>();
    //

    private MdGateway mdGateway;
}
