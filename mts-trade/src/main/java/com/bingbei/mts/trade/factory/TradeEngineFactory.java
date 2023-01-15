package com.bingbei.mts.trade.factory;

import com.bingbei.mts.common.entity.MdInfo;
import com.bingbei.mts.common.service.FastEventEngineService;
import com.bingbei.mts.trade.engine.TradeEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TradeEngineFactory {
    @Autowired
    private FastEventEngineService fastEventEngineService;
    @Autowired
    private GatwayFactory gatwayFactory;

    private Map<String,TradeEngine> tradeEngineMap=new HashMap<>();

    public TradeEngine createTradeEngine(String engineId,MdInfo mdInfo){
        TradeEngine tradeEngine=null;
        if(tradeEngineMap.containsKey(engineId)){
            tradeEngine=tradeEngineMap.get(engineId);
            tradeEngine.changeMd(mdInfo);
        }else{
            tradeEngine=new TradeEngine(engineId,gatwayFactory,fastEventEngineService,mdInfo);
            tradeEngineMap.put(engineId,tradeEngine);
        }
        log.info("创建交易引擎-{}",engineId);
        return tradeEngine;
    }
    public TradeEngine getTradeEngine(String engineId){
        return tradeEngineMap.get(engineId);
    }
}
