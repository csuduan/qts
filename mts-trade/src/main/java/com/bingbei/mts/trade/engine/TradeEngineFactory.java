package com.bingbei.mts.trade.engine;

import com.bingbei.mts.common.entity.Account;
import com.bingbei.mts.common.entity.MdInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class TradeEngineFactory {
    private Map<String,TradeEngine> tradeEngineMap=new HashMap<>();
    private Map<String,TradeEngine> accountEngineMap=new HashMap<>();


    public TradeEngine createTradeEngine(String engineId,MdInfo mdInfo){
        TradeEngine tradeEngine=null;
        if(tradeEngineMap.containsKey(engineId)){
            tradeEngine=tradeEngineMap.get(engineId);
            tradeEngine.changeMd(mdInfo);
        }else{
            tradeEngine=new TradeEngine(engineId,mdInfo);
            tradeEngineMap.put(engineId,tradeEngine);
        }
        log.info("创建交易引擎-{}",engineId);
        return tradeEngine;
    }
    public void createAccount(String engineId, Account account){
        if(accountEngineMap.containsKey(account.getId())){
            log.error("账户{}已存在",account.getId());
            return;
        }
        if(!tradeEngineMap.containsKey(engineId)){
            log.error("交易引擎{}不在在",engineId);
        }
        TradeEngine tradeEngine=tradeEngineMap.get(engineId);
        tradeEngine.createTradeExecutor(account);
        accountEngineMap.put(account.getId(), tradeEngine);
    }
    public TradeEngine getTradeEngine(String engineId){
        return tradeEngineMap.get(engineId);
    }
    public TradeEngine getTradeEngineByAccount(String accountId){
        return accountEngineMap.get(accountId);
    }
}
