package org.qts.trader.engine;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.qts.common.disruptor.FastEventEngineService;
import org.qts.common.disruptor.event.EventConstant;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.disruptor.event.FastEventDynamicHandlerAbstract;
import org.qts.common.disruptor.impl.FastEventEngineServiceImpl;
import org.qts.common.entity.*;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.trade.*;
import org.qts.common.gateway.MdGateway;
import org.qts.common.gateway.TdGateway;
import org.qts.common.utils.BarGenerator;
import org.qts.common.utils.SpringUtils;
import org.qts.gateway.GatwayFactory;
import org.qts.trader.strategy.Strategy;
import org.qts.trader.strategy.StrategySetting;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 交易引擎
 */
@Slf4j
@Data
public class TradeEngine extends FastEventDynamicHandlerAbstract {
    private GatwayFactory gatwayFactory;
    private FastEventEngineService fastEventEngineService;
    //最新行情
    private Map<String, Tick> lastTickMap = new HashMap<>();
    //策略信息
    private Map<String, Strategy> stringStrategyMap=new HashMap<>();
    //报单信息
    private Map<String, Order> workingOrderMap = new HashMap<>();
    //报单-策略 映射map
    private Map<String,Strategy> strategyOrderMap =new HashMap<>();
    //成交信息
    private Map<String, Trade> tradeMap = new HashMap<>();
    //订阅列表
    private Set<String> subsSet=new HashSet<>();


    private MdGateway mdGateway;
    private TdGateway tdGateway;

    private MdInfo mdInfo;
    //account-tradeExecutor

    private Map<String, BarGenerator> barGeneratorMap=new HashMap<>();

    public TradeEngine(AcctDetail acct){
        String waitStrategy=SpringUtils.getContext().getEnvironment().getProperty("fastEventEngine.WaitStrategy");
        this.fastEventEngineService=new FastEventEngineServiceImpl(waitStrategy);
        this.fastEventEngineService.addHandler(this);
        subscribeEvent(EventConstant.EVENT_TICK);
        subscribeEvent(EventConstant.EVENT_TRADE);
        subscribeEvent(EventConstant.EVENT_ORDER);
        subscribeEvent(EventConstant.EVENT_POSITION);
        subscribeEvent(EventConstant.EVENT_ACCOUNT);
        subscribeEvent(EventConstant.EVENT_CONTRACT);
        subscribeEvent(EventConstant.EVENT_ERROR);
        subscribeEvent(EventConstant.EVENT_GATEWAY);


        this.tdGateway=this.gatwayFactory.createTdGateway(acct,this.fastEventEngineService);
        this.mdGateway= this.gatwayFactory.createMdGateway(mdInfo,this.fastEventEngineService);


        //this.loadContract();
    }

    public void connect(){
        if(this.tdGateway!=null)
            this.tdGateway.connect();
        if(this.mdGateway!=null)
            this.mdGateway.connect();;

    }
    public void discount(){
        if(this.tdGateway!=null)
            this.tdGateway.close();
        if(this.mdGateway!=null)
            this.mdGateway.close();;
    }


    @Override
    public void onEvent(final FastEvent fastEvent, final long sequence, final boolean endOfBatch) throws Exception {
        if (!subscribedEventSet.contains(fastEvent.getEvent())) {
            return;
        }
        if(EventConstant.EVENT_TICK.equals(fastEvent.getEventType())){
            Tick tick = fastEvent.getTick();
            tick.setTimeStampOnEvent(System.nanoTime());
            //log.info("{}",tick);
            onTick(tick);
        }

        switch (fastEvent.getEventType()){
            case EventConstant.EVENT_TRADE ->{
                try {
                    Trade trade = fastEvent.getTrade();
                    tradeExecutor.onTrade(trade);
                } catch (Exception e) {
                    log.error("onTrade发生异常", e);
                }
            }
            case EventConstant.EVENT_ORDER ->{
                try {
                    Order order = fastEvent.getOrder();
                    tradeExecutor.onOrder(order);
                } catch (Exception e) {
                    log.error("onOrder发生异常", e);
                }
            }
            case EventConstant.EVENT_CONTRACT ->{
                try {
                    Contract contract = fastEvent.getContract();
                    tradeExecutor.onContract(contract);
                } catch (Exception e) {
                    log.error("onContract发生异常", e);
                }
            }
            case EventConstant.EVENT_POSITION ->{
                try {
                    AccoPosition position = fastEvent.getPosition();
                    tradeExecutor.onAccoPosition(position);
                } catch (Exception e) {
                    log.error("onPosition发生异常", e);
                }
            }
            case EventConstant.EVENT_ACCOUNT ->{
                try {
                    Account account = fastEvent.getAccount();
                    //onAccount(account);
                } catch (Exception e) {
                    log.error("onAccount发生异常", e);
                }
            }
            default -> {
                log.warn("未能识别的事件数据类型{}", JSON.toJSONString(fastEvent.getEvent()));
            }
        }
    }

    private void onTick(Tick tick) {
        if(!this.subsSet.contains(tick.getSymbol()))
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

        BarGenerator barGenerator;
        if(!barGeneratorMap.containsKey(tick.getSymbol())){
            //创建bar
            barGenerator = new BarGenerator(new BarGenerator.CommonBarCallBack() {
                @Override
                public void call(Bar bar) {
                    tradeExecutorMap.values().forEach(x->{
                        x.onBar(bar);
                    });
                }
            });
            barGeneratorMap.put(tick.getSymbol(), barGenerator);
        }else
            barGenerator = barGeneratorMap.get(tick.getSymbol());
        //更新bar
        barGenerator.updateTick(tick);
    }
}
