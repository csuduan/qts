package org.qts.trader.core;

import lombok.extern.slf4j.Slf4j;
import org.qts.common.entity.trade.*;
import org.qts.trader.strategy.Strategy;
import org.qts.trader.strategy.StrategySetting;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class StrategyEngine {

    //策略列表(strategyId-strategy)
    private Map<String,Strategy> strategyMap =new HashMap<>();
    //策略报单映射表(orderRef-strategy)
    private Map<String,Strategy> strategyOrderMap =new HashMap<>();
    //策略订阅合约列表
    private Map<String, Set<Strategy>> subsMap = new HashMap<>();
    private AcctInst acctInst;

    public  StrategyEngine(AcctInst acctInst){
        this.acctInst = acctInst;
    }

    public void createStrategy(StrategySetting strategySetting) {
        try {
            if(this.strategyMap.containsKey(strategySetting.getStrategyId())){
                log.warn("已存在策略{}",strategySetting.getStrategyId());
                return;
            }
            Class<?> clazz = Class.forName(strategySetting.getClassName());
            Constructor<?> c = clazz.getConstructor(AcctExecutor.class,StrategySetting.class);
            Strategy strategy = (Strategy) c.newInstance(this, strategySetting);
            strategy.init(this,strategySetting);
            this.strategyMap.put(strategySetting.getStrategyId(),strategy);
            log.info("创建策略{}成功",strategySetting.getStrategyId());
        }catch (Exception ex){
            log.error("创建策略失败",ex);
        }

    }
    public void onTick(Tick tick){
        Set<Strategy> strategySet=this.subsMap.get(tick.getSymbol());
        strategySet.forEach(x->{
            try {
                x.onTick(tick);
            }catch (Exception ex){
                log.error("策略{}处理Tick {} 失败",x.getStrategySetting().getStrategyId(),tick.getSymbol(),ex);
            }

        });
    }
    public void onBar(Bar bar){

    }
    public void onOrder(Order order){
        Strategy strategy=strategyOrderMap.get(order.getOrderRef());
        if(strategy!=null)
            try {
                strategy.onOrder(order);
            }catch (Exception ex){
                log.error("onOrder error",ex);
            }

    }
    public void onTrade(Trade trade){
        Strategy strategy=strategyOrderMap.get(trade.getOrderRef());
        if(strategy!=null)
            try {
                strategy.onTrade(trade);
            }catch (Exception ex){
                log.error("onOrder error",ex);
            }
    }

    public void subs(List<String> symbols){
        this.acctInst.subContract(symbols);
    }

    public void insertOrder(Order order){
        this.acctInst.insertOrder(order);
    }

    public void cancel(Order order){
        String sysId = order.getOrderSysID();
        if(StringUtils.hasLength(sysId) && order.canCancel()){
            OrderCancelReq req=new OrderCancelReq();
            req.setOrderSysID(sysId);
            this.acctInst.cancelOrder(req);
        }
    }

}
