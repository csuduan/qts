package com.bingbei.mts.trade.engine;

import com.bingbei.mts.common.entity.Tick;
import com.bingbei.mts.common.gateway.MdGateway;
import com.bingbei.mts.common.service.FastEventEngineService;
import com.bingbei.mts.common.service.PersistSerivce;
import com.bingbei.mts.trade.gateway.GatwayFactory;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class EngineContext {
    private PersistSerivce persistSerivce;
    //private StrategyEngine strategyEngine;
    private GatwayFactory gatwayFactory;

    private FastEventEngineService fastEventEngineService;

    //最新行情
    private Map<String, Tick> lastTickMap = new HashMap<>();
    //

    private MdGateway mdGateway;
}
