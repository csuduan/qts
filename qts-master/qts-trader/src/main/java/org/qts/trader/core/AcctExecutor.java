package org.qts.trader.core;

import com.alibaba.fastjson.JSON;
import org.qts.common.dao.AcctMapper;
import org.qts.common.disruptor.FastQueue;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.disruptor.event.FastEventHandlerAbstract;
import org.qts.common.entity.*;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.trade.*;
import org.qts.trader.gateway.MdGateway;
import org.qts.trader.gateway.TdGateway;
import org.qts.common.utils.BarGenerator;
import org.qts.common.utils.SpringUtils;
import org.qts.trader.gateway.GatwayFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 账户执行器
 */
@Slf4j
@Component
public class AcctExecutor extends FastEventHandlerAbstract {
    @Value("${acctId}")
    private String acctId;
    @Autowired
    private AcctMapper acctMapper;

    private AcctInfo acctInfo;
    private MdGateway mdGateway;
    private TdGateway tdGateway;
    private StrategyEngine strategyEngine;

    private Set<String> contractSubSet = new HashSet<>();

    //最新行情
    private Map<String, Tick> lastTickMap = new HashMap<>();
    //合约信息(合约代码-合约信息)
    private Map<String, Contract> contractMap = new HashMap<>();
    //报单列表(orderRef-order)
    private Map<String, Order> workingOrderMap = new HashMap<>();

    private Map<String, BarGenerator> barGeneratorMap=new HashMap<>();


    @PostConstruct
    public void init(){
        AcctConf acctConf=acctMapper.queryAcctConf(acctId);
        log.info("账户配置:{}",acctConf);
        this.acctInfo =new AcctInfo(acctConf);
        this.acctInfo.getFastQueue().addHandler(this);
        subscribeEvent(FastEvent.EVENT_TICK);
        subscribeEvent(FastEvent.EVENT_TRADE);
        subscribeEvent(FastEvent.EVENT_ORDER);
        subscribeEvent(FastEvent.EVENT_POSITION);
        subscribeEvent(FastEvent.EVENT_ACCT);
        subscribeEvent(FastEvent.EVENT_CONTRACT);
        subscribeEvent(FastEvent.EVENT_ERROR);
        subscribeEvent(FastEvent.EVENT_GATEWAY);

        this.tdGateway= GatwayFactory.createTdGateway(acctInfo);
        this.mdGateway= GatwayFactory.createMdGateway(acctInfo);

        this.strategyEngine =new StrategyEngine();

    }

    public void close(){
        try {
            this.tdGateway.close();
            this.mdGateway.close();
        }catch (Exception ex){
            log.error("acctExecutor close...");
        }

    }


    public void subContract(String symobl){
        this.mdGateway.subscribe(symobl);
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


    public void connect(){
        if(this.tdGateway!=null && !this.tdGateway.isConnected())
            this.tdGateway.connect();
        if(this.mdGateway!=null && !this.mdGateway.isConnected())
            this.mdGateway.connect();;

    }
    public void discount(){
        if(this.tdGateway!=null && this.tdGateway.isConnected())
            this.tdGateway.close();
        if(this.mdGateway!=null && this.tdGateway.isConnected())
            this.mdGateway.close();;
    }

    public void onTick(Tick tick){
        BarGenerator barGenerator;
        if(!barGeneratorMap.containsKey(tick.getSymbol())){
            //创建bar
            barGenerator = new BarGenerator(bar->onBar(bar));
            barGeneratorMap.put(tick.getSymbol(), barGenerator);
        }else
            barGenerator = barGeneratorMap.get(tick.getSymbol());
        //更新bar
        barGenerator.updateTick(tick);
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
        this.strategyEngine.onOrder(order);
    }
    public void onTrade(Trade trade) throws Exception{
        //this.tradeMap.put(trade.getTradeID(),trade);
        this.strategyEngine.onTrade(trade);
    }
    public void onContract(Contract contract){
        this.contractMap.put(contract.getSymbol(),contract);

    }
    public void onAccoPosition(Position accoPosition){
        this.acctInfo.getPositions().put(accoPosition.getId(), accoPosition);
    }

    @Override
    public void onEvent(final FastEvent fastEvent, final long sequence, final boolean endOfBatch) throws Exception {
        if (!subscribedEventSet.contains(fastEvent.getType())) {
            return;
        }
        switch (fastEvent.getType()){
            case FastEvent.EVENT_TICK ->{
                Tick tick = fastEvent.getData(Tick.class);
                tick.setTimeStampOnEvent(System.nanoTime());
                if(!this.contractSubSet.contains(tick.getSymbol()))
                    return;
                this.lastTickMap.put(tick.getSymbol(),tick);
                this.onTick(tick);
            }
            case FastEvent.EVENT_TRADE ->{
                try {
                    Trade trade = fastEvent.getData(Trade.class);
                    this.onTrade(trade);
                } catch (Exception e) {
                    log.error("onTrade发生异常", e);
                }
            }
            case FastEvent.EVENT_ORDER ->{
                try {
                    Order order = fastEvent.getData(Order.class);
                    this.onOrder(order);
                } catch (Exception e) {
                    log.error("onOrder发生异常", e);
                }
            }
            case FastEvent.EVENT_CONTRACT ->{
                try {
                    Contract contract = fastEvent.getData(Contract.class);
                    this.onContract(contract);
                } catch (Exception e) {
                    log.error("onContract发生异常", e);
                }
            }
            case FastEvent.EVENT_POSITION ->{
                try {
                    Position position = fastEvent.getData(Position.class);
                    this.onAccoPosition(position);
                } catch (Exception e) {
                    log.error("onPosition发生异常", e);
                }
            }
            case FastEvent.EVENT_ACCT ->{
                try {
                    //Account account = fastEvent.ge();
                    AcctInfo acctInfo1 = fastEvent.getData(AcctInfo.class);
                    //onAccount(account);
                } catch (Exception e) {
                    log.error("onAccount发生异常", e);
                }
            }
            default -> {
                log.warn("未能识别的事件数据类型{}", JSON.toJSONString(fastEvent.getType()));
            }
        }
    }


}
