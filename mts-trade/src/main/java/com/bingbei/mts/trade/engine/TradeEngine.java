package com.bingbei.mts.trade.engine;

import com.alibaba.fastjson.JSON;
import com.bingbei.mts.common.entity.*;
import com.bingbei.mts.common.gateway.MdGateway;
import com.bingbei.mts.common.service.PersistSerivce;
import com.bingbei.mts.common.service.extend.event.EventConstant;
import com.bingbei.mts.common.service.extend.event.FastEvent;
import com.bingbei.mts.common.service.extend.event.FastEventDynamicHandlerAbstract;
import com.bingbei.mts.common.service.impl.FastEventEngineServiceImpl;
import com.bingbei.mts.common.utils.BarGenerator;
import com.bingbei.mts.common.utils.SpringUtils;
import com.bingbei.mts.trade.gateway.GatwayFactory;
import com.bingbei.mts.trade.strategy.StrategySetting;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易引擎
 */
@Slf4j
public class TradeEngine extends FastEventDynamicHandlerAbstract {
    private String id;
    private EngineContext engineContext;

    private MdInfo mdInfo;
    private MdGateway mdGateway;
    //account-tradeExecutor
    Map<String, TradeExecutor> tradeExecutorMap=new HashMap<>();
    public String getId(){
        return this.id;
    }

    private Map<String, BarGenerator> barGeneratorMap=new HashMap<>();

    public TradeEngine(String engineId, MdInfo mdInfo){
        this.id=engineId;
        this.engineContext=new EngineContext();
        this.engineContext.setPersistSerivce(SpringUtils.getBean(PersistSerivce.class));
        this.engineContext.setGatwayFactory(SpringUtils.getBean(GatwayFactory.class));
        String waitStrategy=SpringUtils.getContext().getEnvironment().getProperty("fastEventEngine.WaitStrategy");
        this.engineContext.setFastEventEngineService(new FastEventEngineServiceImpl(waitStrategy));
        this.engineContext.getFastEventEngineService().addHandler(this);
        subscribeEvent(EventConstant.EVENT_TICK);
        subscribeEvent(EventConstant.EVENT_TRADE);
        subscribeEvent(EventConstant.EVENT_ORDER);
        subscribeEvent(EventConstant.EVENT_POSITION);
        subscribeEvent(EventConstant.EVENT_ACCOUNT);
        subscribeEvent(EventConstant.EVENT_CONTRACT);
        subscribeEvent(EventConstant.EVENT_ERROR);
        subscribeEvent(EventConstant.EVENT_GATEWAY);

        this.changeMd(mdInfo);
        this.loadContract();
    }
    private void loadContract(){
        List<Contract> contracts=engineContext.getPersistSerivce().getContracts();
    }
    public void changeMd(MdInfo mdInfo){
        if(this.mdGateway!=null)
            this.mdGateway.close();
        this.mdInfo=mdInfo;
        this.mdGateway=engineContext.getGatwayFactory().createMdGateway(mdInfo,this.engineContext.getFastEventEngineService());
        this.mdGateway.connect();
        engineContext.setMdGateway(this.mdGateway);
    }
    public void createTradeExecutor(Account account){
        this.tradeExecutorMap.put(account.getId(),new TradeExecutor(account,this.engineContext));
        log.info("交易引擎添加账户{}成功",account.getId());

    }

    public void createStrategy(StrategySetting strategySetting){
        TradeExecutor tradeExecutor=this.tradeExecutorMap.get(strategySetting.getAccountId());
        tradeExecutor.createStrategy(strategySetting);
    }
    public void connect(String accountId){
        var tradeExecutor=this.tradeExecutorMap.get(accountId);
        if(tradeExecutor==null){
            log.warn("找不到账户交易执行器-{}",accountId);
            return;
        }
        tradeExecutor.connect();


    }
    public void discount(String accountId){
        var tradeExecutor=this.tradeExecutorMap.get(accountId);
        if(tradeExecutor==null){
            log.warn("找不到账户交易执行器-{}",accountId);
            return;
        }
        tradeExecutor.disconnect();

    }
    public void connnectMd(){
        this.mdGateway.connect();
    }
    public void disconnectMd(){
        this.mdGateway.close();
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
        //以下为账户相关事件
        TradeExecutor tradeExecutor=this.tradeExecutorMap.get(fastEvent.getAccountId());
        if(tradeExecutor==null)
            return;


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
        this.engineContext.getLastTickMap().put(tick.getSymbol(), tick);
        this.tradeExecutorMap.values().forEach(x->{
            x.onTick(tick);
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
