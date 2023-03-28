package com.bingbei.mts.trade.engine;

import com.bingbei.mts.common.entity.*;
import com.bingbei.mts.common.gateway.TdGateway;
import com.bingbei.mts.trade.strategy.Strategy;
import com.bingbei.mts.trade.strategy.StrategySetting;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * 账户交易执行器
 */
@Slf4j
public class TradeExecutor {
    private Account account;
    private EngineContext context;
    private TdGateway tdGateway;

    //合约订阅列表
    private Map<String, Set<Strategy>> subsMap = new HashMap<>();
    //合约信息
    private Map<String, Contract> contractMap = new HashMap<>();
    //tick信息
    private Map<String, Tick> lastTickMap=new HashMap<>();
    //策略信息
    private Map<String, Strategy> stringStrategyMap=new HashMap<>();
    //报单信息
    private Map<String, Order> workingOrderMap = new HashMap<>();
    //报单-策略 映射map
    private Map<String,Strategy> strategyOrderMap =new HashMap<>();
    //成交信息
    private Map<String, Trade> tradeMap = new HashMap<>();
    //本地仓位(区别与账户仓位)
    //private Map<String, LocalPosition> localPositionMap = new HashMap<>();

    public TradeExecutor(Account account, EngineContext engineContext){
        this.account=account;
        this.context=engineContext;
        this.tdGateway=engineContext.getGatwayFactory().createTdGateway(account,this.context.getFastEventEngineService());

    }

    /**
     * 订阅合约
     * @param symbol
     * @param strategy
     */
    public void subContract(String symbol,Strategy strategy){
        if(!this.subsMap.containsKey(symbol))
            subsMap.put(symbol,new HashSet<>());

        Set<Strategy> set=subsMap.get(symbol);
        set.add(strategy);
        this.context.getMdGateway().subscribe(symbol);

    }

    /**
     * 报单
     */
    public void insertOrder(Order order){
        String exchange=this.contractMap.get(order.getSymbol()).getExchange();
        //补充报单信息
        order.setExchange(exchange);
        Tick lastTick=this.lastTickMap.get(order.getSymbol());
        if(order.getPrice()==0 && lastTick!=null){
            //todo 以对手价报单
            order.setPrice(lastTick.getLastPrice());
        }
        //todo 自成交校验
        this.tdGateway.insertOrder(order);
    }

    /**
     * 撤单
     * @param orderRef
     */
    public void cancelOrder(String orderRef){
        if (workingOrderMap.containsKey(orderRef)) {
            Order order=workingOrderMap.get(orderRef);
            //todo
            CancelOrderReq cancelOrderReq=new CancelOrderReq();
            cancelOrderReq.setOrderID(orderRef);
            this.tdGateway.cancelOrder(cancelOrderReq);
        }
    }

    public void createStrategy(StrategySetting strategySetting) {
        try {
            if(this.stringStrategyMap.containsKey(strategySetting.getStrategyId())){
                log.warn("已存在策略{}",strategySetting.getStrategyId());
                return;
            }
            Class<?> clazz = Class.forName(strategySetting.getClassName());
            Constructor<?> c = clazz.getConstructor(TradeExecutor.class,StrategySetting.class);
            Strategy strategy = (Strategy) c.newInstance(this, strategySetting);
            strategy.init();
            this.stringStrategyMap.put(strategySetting.getStrategyId(),strategy);
            log.info("创建策略{}成功",strategySetting.getStrategyId());
        }catch (Exception ex){
            log.error("创建策略失败",ex);
        }

    }

    public void connect(){
        this.tdGateway.connect();
    }

    public void disconnect(){
        this.tdGateway.close();
    }

    public void onTick(Tick tick){
        if(!this.subsMap.containsKey(tick.getSymbol()))
            return;

        this.lastTickMap.put(tick.getSymbol(),tick);

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
    public void onOrder(Order order) throws  Exception{
        if(!this.workingOrderMap.containsKey(order.getOrderRef()))
            return;//过滤非本地报单

        if (order.isFinished()) {
            if (this.workingOrderMap.containsKey(order.getOrderRef())) {
                this.workingOrderMap.remove(order.getOrderRef());
            }
        } else {
            this.workingOrderMap.put(order.getOrderRef(), order);
        }

        Strategy strategy=strategyOrderMap.get(order.getOrderRef());
        if(strategy!=null)
            strategy.onOrder(order);

    }
    public void onTrade(Trade trade) throws Exception{
        this.tradeMap.put(trade.getTradeID(),trade);
        Strategy strategy=strategyOrderMap.get(trade.getOrderRef());
        if(strategy!=null)
            strategy.onTrade(trade);
    }
    public void onContract(Contract contract){
        this.contractMap.put(contract.getSymbol(),contract);

    }
    public void onAccoPosition(AccoPosition accoPosition){
        account.getAccoPositionMap().put(accoPosition.getPositionID(), accoPosition);
    }


}
