package com.bingbei.mts.trade.engine;

import com.alibaba.fastjson.JSON;
import com.bingbei.mts.common.entity.*;
import com.bingbei.mts.common.gateway.MdGateway;
import com.bingbei.mts.common.gateway.TdGateway;
import com.bingbei.mts.common.service.FastEventEngineService;
import com.bingbei.mts.common.service.extend.event.EventConstant;
import com.bingbei.mts.common.service.extend.event.FastEvent;
import com.bingbei.mts.common.service.extend.event.FastEventDynamicHandlerAbstract;
import com.bingbei.mts.trade.factory.GatwayFactory;
import com.bingbei.mts.trade.strategy.StrategyEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TradeEngine extends FastEventDynamicHandlerAbstract {
    private String id;
    private MdInfo mdInfo;
    private MdGateway mdGateway;
    //一个交易引擎支持多个账户的交易
    private Map<String, Account> accountMap=new HashMap<>();
    private Map<String, Contract> contractMap = new HashMap<>();
    private Map<String, Tick> tickMap = new HashMap<>();


    private StrategyEngine strategyEngine;
    private GatwayFactory gatwayFactory;

    //private FastEventEngineService fastEventEngineService;
    public String getId(){
        return this.id;
    }

    public TradeEngine(String engineId,GatwayFactory gatwayFactory,FastEventEngineService fastEventEngineService, MdInfo mdInfo){
        this.id=engineId;
        this.gatwayFactory=gatwayFactory;
        this.changeMd(mdInfo);
        fastEventEngineService.addHandler(this);
        subscribeEvent(EventConstant.EVENT_TICK);
        subscribeEvent(EventConstant.EVENT_TRADE);
        subscribeEvent(EventConstant.EVENT_ORDER);
        subscribeEvent(EventConstant.EVENT_POSITION);
        subscribeEvent(EventConstant.EVENT_ACCOUNT);
        subscribeEvent(EventConstant.EVENT_CONTRACT);
        subscribeEvent(EventConstant.EVENT_ERROR);
        subscribeEvent(EventConstant.EVENT_GATEWAY);
    }
    public void changeMd(MdInfo mdInfo){
        if(this.mdGateway!=null)
            this.mdGateway.close();
        this.mdInfo=mdInfo;
        this.mdGateway=this.gatwayFactory.createMdGateway(mdInfo);
    }
    public void addAccount(Account account){
        this.accountMap.put(account.getId(),account);
        var tdGateway=gatwayFactory.createTdGateway(account);
        account.setTdGateway(tdGateway);
        log.info("交易引擎添加账户{}成功",account.getId());

    }
    public void connect(String accountId){
        Account account=accountMap.get(accountId);
        if(account==null){
            log.warn("找不到账户-{}",accountId);
            return;
        }

        account.getTdGateway().connect();


    }
    public void close(String accountId){

    }


    @Override
    public void onEvent(final FastEvent fastEvent, final long sequence, final boolean endOfBatch) throws Exception {
        if (!subscribedEventSet.contains(fastEvent.getEvent())) {
            return;
        }
        switch (fastEvent.getEventType()){
            case EventConstant.EVENT_TICK -> {
                try {
                    Tick tick = fastEvent.getTick();
                    onTick(tick);
                } catch (Exception e) {
                    log.error("onTick发生异常", e);
                }
            }
            case EventConstant.EVENT_TRADE ->{
                try {
                    Trade trade = fastEvent.getTrade();
                    onTrade(trade);
                } catch (Exception e) {
                    log.error("onTrade发生异常", e);
                }
            }
            case EventConstant.EVENT_ORDER ->{
                try {
                    Order order = fastEvent.getOrder();
                    onOrder(order);
                } catch (Exception e) {
                    log.error("onOrder发生异常", e);
                }
            }
            case EventConstant.EVENT_CONTRACT ->{
                try {
                    Contract contract = fastEvent.getContract();
                    onContract(contract);
                } catch (Exception e) {
                    log.error("onContract发生异常", e);
                }
            }
            case EventConstant.EVENT_POSITION ->{
                try {
                    Position position = fastEvent.getPosition();
                    onPosition(position);
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
    private void onContract(Contract contract) {
        contractMap.put(contract.getSymbol(), contract);
        // CTP重连时Trade可能先于Contract到达,在此处重新赋值
//        String exchange = contract.getExchange();
//        String symbol = contract.getSymbol();
//        LocalPositionDetail localPositionDetail = localPositionDetailMap.get(contract.getSymbol());
//        if (localPositionDetail != null) {
//            localPositionDetail.setExchange(exchange);
//            localPositionDetail.setSymbol(symbol);
//            localPositionDetail.setMultiple(contract.getMultiple());
//        }

    }
    private void onOrder(Order order) {
        Account account=accountMap.get(order.getAccountID());
        if(account==null)
            return;
        account.getOrderMap().put(order.getOrderID(), order);
        if (RtConstant.STATUS_FINISHED.contains(order.getStatus())) {
            if (account.getWorkingOrderMap().containsKey(order.getOrderID())) {
                account.getWorkingOrderMap().remove(order.getOrderID());
            }
        } else {
            account.getWorkingOrderMap().put(order.getOrderID(), order);
        }

        LocalPositionDetail localPositionDetail;
        if (account.getLocalPositionDetailMap().containsKey(order.getOrderID())) {
            localPositionDetail = account.getLocalPositionDetailMap().get(order.getOrderID());
        } else {
            localPositionDetail = account.createLocalPositionDetail(order.getAccountID(),order.getSymbol());
        }

        localPositionDetail.updateOrder(order);
    }

    private void onTrade(Trade trade) {
        Account account=accountMap.get(trade.getAccountID());
        if(account==null)
            return;
        account.getTradeMap().put(trade.getTradeID(), trade);
        LocalPositionDetail localPositionDetail;
        if (account.getLocalPositionDetailMap().containsKey(trade.getSymbol())) {
            localPositionDetail = account.getLocalPositionDetailMap().get(trade.getSymbol());
        } else {
            localPositionDetail = account.createLocalPositionDetail(trade.getAccountID(),trade.getSymbol());
        }

        localPositionDetail.updateTrade(trade);
    }

    private void onPosition(Position position) {
        Account account=accountMap.get(position.getAccountID());
        if(account==null)
            return;

        account.getPositionMap().put(position.getPositionID(), position);
        //todo
//        LocalPositionDetail localPositionDetail;
//        String positionDetailKey = position.getRtSymbol() + "." + position.getRtAccountID();
//        if (localPositionDetailMap.containsKey(positionDetailKey)) {
//            localPositionDetail = localPositionDetailMap.get(positionDetailKey);
//        } else {
//            localPositionDetail = createLocalPositionDetail(position.getGatewayID(), position.getGatewayDisplayName(),
//                    position.getAccountID(), position.getRtAccountID(), position.getExchange(), position.getRtSymbol(),
//                    position.getSymbol());
//        }
//
//        localPositionDetail.updatePosition(position);

    }

    private void onTick(Tick tick) {
        tickMap.put(tick.getSymbol(), tick);
        //推送给策略
        //this.strategyEngine.


        for(Account account:accountMap.values()){
            // 刷新持仓盈亏
            for (LocalPositionDetail localPositionDetail : account.getLocalPositionDetailMap().values()) {
                if (localPositionDetail.getSymbol().equals(tick.getSymbol())) {
                    localPositionDetail.updateLastPrice(tick.getLastPrice());
                }
            }
        }


    }


}
