package org.qts.trader.core;

import com.alibaba.fastjson.JSON;
import com.lmax.disruptor.EventHandler;
import org.qts.common.dao.AcctMapper;
import org.qts.common.disruptor.FastQueue;
import org.qts.common.disruptor.event.FastEvent;
import org.qts.common.entity.*;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.trade.*;
import org.qts.common.rpc.tcp.server.MsgHandler;
import org.qts.common.rpc.tcp.server.TcpServer;
import org.qts.common.utils.SpringUtils;
import org.qts.trader.gateway.MdGateway;
import org.qts.trader.gateway.TdGateway;
import org.qts.common.utils.BarGenerator;
import org.qts.trader.gateway.GatwayFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 账户执行器
 */
@Slf4j
@Component
public class AcctExecutor implements EventHandler<FastEvent>, MsgHandler {
    @Value("${acctId}")
    private String acctId;
    @Autowired
    private AcctMapper acctMapper;

    private TcpServer tcpServer;

    private FastQueue fastQueue;

    private AcctDetail acct;

    private MdGateway mdGateway;
    private TdGateway tdGateway;
    private StrategyEngine strategyEngine;
    private Map<String, BarGenerator> barGeneratorMap = new HashMap<>();


    @PostConstruct
    public void init() {

    }

    public void start() {
        AcctConf acctConf = acctMapper.queryAcctConf(acctId);
        log.info("账户配置:{}", acctConf);

        String waitStrategy = SpringUtils.getContext().getEnvironment().getProperty("fastQueue.WaitStrategy");
        this.fastQueue = new FastQueue(waitStrategy, this);

        this.acct = new AcctDetail(acctConf, fastQueue);


        this.tdGateway = GatwayFactory.createTdGateway(acct);
        this.mdGateway = GatwayFactory.createMdGateway(acct);

        this.strategyEngine = new StrategyEngine();

        //启动ipcServer
        Integer port = this.acct.getConf().getPort();
        this.tcpServer = new TcpServer(port, this);
        this.tcpServer.start();
    }

    public void close() {
        try {
            this.tdGateway.close();
            this.mdGateway.close();

        } catch (Exception ex) {
            log.error("acctExecutor close...");
        }

    }


    public void subContract(List<String> symobls) {
        this.acct.getSubList().addAll(symobls);
        this.mdGateway.subscribe(symobls);
    }

    /**
     * 报单
     */
    public void insertOrder(OrderInsertReq req) {
        //TODO 自成交校验
        Order order =new Order(req.getSymbol(),req.getOffset(),req.getDirection(), req.getVolume(), req.getPrice());
        order.setExchange(req.getExchange());
        Tick lastTick = acct.getTicks().get(order.getSymbol());
        if (order.getPrice() == 0 && lastTick != null) {
            order.setPrice(lastTick.getLastPrice());
        }
        this.tdGateway.insertOrder(order);
    }

    /**
     * 撤单(可以撤外部保单)
     *
     * @param
     */
    public void cancelOrder(OrderCancelReq req) {
        this.tdGateway.cancelOrder(req);
    }


    public void onTick(Tick tick) {
        BarGenerator barGenerator;
        if (!barGeneratorMap.containsKey(tick.getSymbol())) {
            //创建bar
            barGenerator = new BarGenerator(bar -> onBar(bar));
            barGeneratorMap.put(tick.getSymbol(), barGenerator);
        } else
            barGenerator = barGeneratorMap.get(tick.getSymbol());
        //更新bar
        barGenerator.updateTick(tick);
        strategyEngine.onTick(tick);
    }

    public void onBar(Bar bar) {

    }

    public void onOrder(Order order) throws Exception {
        this.strategyEngine.onOrder(order);
    }

    public void onTrade(Trade trade) throws Exception {
        //this.tradeMap.put(trade.getTradeID(),trade);
        this.strategyEngine.onTrade(trade);
    }

    public void onContract(Contract contract) {

    }

    public void onAccoPosition(Position accoPosition) {
        this.acct.getPositions().put(accoPosition.getId(), accoPosition);
    }

    @Override
    public void onEvent(final FastEvent fastEvent, final long sequence, final boolean endOfBatch) throws Exception {
        switch (fastEvent.getType()) {
            case FastEvent.EV_TICK -> {
                Tick tick = fastEvent.getData(Tick.class);
                tick.setTimeStampOnEvent(System.nanoTime());
                this.onTick(tick);
            }
            case FastEvent.EV_TRADE -> {
                try {
                    Trade trade = fastEvent.getData(Trade.class);
                    this.onTrade(trade);
                } catch (Exception e) {
                    log.error("onTrade发生异常", e);
                }
            }
            case FastEvent.EV_ORDER -> {
                try {
                    Order order = fastEvent.getData(Order.class);
                    this.onOrder(order);
                } catch (Exception e) {
                    log.error("onOrder发生异常", e);
                }
            }
            case FastEvent.EV_CONTRACT -> {
                try {
                    Contract contract = fastEvent.getData(Contract.class);
                    this.onContract(contract);
                } catch (Exception e) {
                    log.error("onContract发生异常", e);
                }
            }
            case FastEvent.EV_POSITION -> {
                try {
                    Position position = fastEvent.getData(Position.class);
                    this.onAccoPosition(position);
                } catch (Exception e) {
                    log.error("onPosition发生异常", e);
                }
            }
            case FastEvent.EV_ACCT -> {
                try {
                    //Account account = fastEvent.ge();
                    AcctDetail acctInfo1 = fastEvent.getData(AcctDetail.class);
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


    @Override
    public Message onRequest(Message req) {
        Message rsp = req.buildResp(0, null);
        switch (req.getType()) {
            case EXIT -> {
                new Thread(() -> {
                    try {
                        log.error("will exit 5s later...");
                        Thread.sleep(5000);
                        this.close();
                        System.exit(0);
                    } catch (Exception ex) {

                    }
                }).start();
            }
            case CONNECT -> {
                this.tdGateway.connect();
                this.mdGateway.connect();

            }
            case DISCONNECT -> {
                this.tdGateway.close();
                this.mdGateway.close();
            }
            case MD_SUBS -> {
                List<String> symbols = req.getList(String.class);
                this.subContract(symbols);
            }
            case PAUSE_OPEN -> {
                CommReq data = req.getData(CommReq.class);
                Boolean enable = data.isEnable();
                this.acct.setPauseOpen(enable);
            }
            case PAUSE_CLOSE -> {
                CommReq data = req.getData(CommReq.class);
                Boolean enable = data.isEnable();
                this.acct.setPauseClose(enable);
            }
            case QRY_ACCT -> {
                rsp =req.buildResp(0, this.acct);
            }
            case QRY_POSITION -> {
                rsp =req.buildResp(0, this.acct.getPositions().values());
            }
            case QRY_TRADE -> {
                rsp =req.buildResp(0, this.acct.getTradeList().values());

            }
            case QRY_ORDER -> {
                rsp =req.buildResp(0, this.acct.getOrders().values());
            }
            case  ORDER_INSERT -> {
                OrderInsertReq orderInsertReq = req.getData(OrderInsertReq.class);
                this.insertOrder(orderInsertReq);
            }
            case ORDER_CANCEL -> {
                OrderCancelReq orderCancelReq = req.getData(OrderCancelReq.class);
                this.cancelOrder(orderCancelReq);
            }

            default -> {
                log.warn("暂不支持此类型:{}", req.getType());
                rsp.setCode(-1);
                rsp.setRetMsg("暂不支持此类型");
            }
        }
        return rsp;
    }

    @Override
    public void onConnected() {
        //新客户端连接，全量推送一次账户详情
        Message msg=new Message(Enums.MSG_TYPE.ON_DETAIL,this.acct);
        this.tcpServer.push(msg);
    }


    @Scheduled(fixedRate = 30000)
    public void printAcct() {
        if(this.acct == null)
            return;
        StringBuilder builder = new StringBuilder();
        builder.append(this.acct.getAcctInfo().toString()+"\n");
        this.acct.getPositions().values().forEach(x -> builder.append(x.toString() + "\n"));
        this.acct.getOrders().values().forEach(x -> builder.append(x.toString() + "\n"));
        this.acct.getTradeList().values().forEach(x -> builder.append(x.toString() + "\n"));
        log.info("PRINT==> \n{}", builder.toString());
    }

}
