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

    private AcctInst acctInst;
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

        //账户实例
        this.acctInst = new AcctInst(acctConf, fastQueue);

        //策略引擎
        this.strategyEngine = new StrategyEngine(this.acctInst);

        //启动ipcServer
        Integer port = acctConf.getPort();
        this.tcpServer = new TcpServer(port, this);
        this.tcpServer.start();
    }



    public void onBar(Bar bar) {
    }


    @Override
    public void onEvent(final FastEvent fastEvent, final long sequence, final boolean endOfBatch) throws Exception {
        switch (fastEvent.getType()) {
            case FastEvent.EV_TICK -> {
                Tick tick = fastEvent.getData(Tick.class);
                tick.setTimeStampOnEvent(System.nanoTime());
//                BarGenerator barGenerator;
//                if (!barGeneratorMap.containsKey(tick.getSymbol())) {
//                    //创建bar
//                    barGenerator = new BarGenerator(bar -> onBar(bar));
//                    barGeneratorMap.put(tick.getSymbol(), barGenerator);
//                } else
//                    barGenerator = barGeneratorMap.get(tick.getSymbol());
//                //更新bar
//                barGenerator.updateTick(tick);
                strategyEngine.onTick(tick);

            }
            case FastEvent.EV_TRADE -> {
                try {
                    Trade trade = fastEvent.getData(Trade.class);
                    //this.onTrade(trade);
                    this.strategyEngine.onTrade(trade);
                } catch (Exception e) {
                    log.error("onTrade发生异常", e);
                }
            }
            case FastEvent.EV_ORDER -> {
                try {
                    Order order = fastEvent.getData(Order.class);
                    //this.onOrder(order);
                    this.strategyEngine.onOrder(order);
                } catch (Exception e) {
                    log.error("onOrder发生异常", e);
                }
            }
            case FastEvent.EV_CONTRACT -> {
                try {
                    Contract contract = fastEvent.getData(Contract.class);
                    //his.onContract(contract);
                } catch (Exception e) {
                    log.error("onContract发生异常", e);
                }
            }
            case FastEvent.EV_POSITION -> {
                try {
                    Position position = fastEvent.getData(Position.class);
                } catch (Exception e) {
                    log.error("onPosition发生异常", e);
                }
            }
            case FastEvent.EV_ACCT -> {
                try {
                    //AcctDetail acctInfo1 = fastEvent.getData(AcctDetail.class);
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
                        this.acctInst.close();
                        System.exit(0);
                    } catch (Exception ex) {

                    }
                }).start();
            }
            case CONNECT -> {
                this.acctInst.connect(true);

            }
            case DISCONNECT -> {
                this.acctInst.connect(false);
            }
            case MD_SUBS -> {
                List<String> symbols = req.getList(String.class);
                this.acctInst.subContract(symbols);
            }
            case PAUSE_OPEN -> {
                CommReq data = req.getData(CommReq.class);
                Boolean enable = data.isEnable();
                this.acctInst.pauseOpen(enable);
            }
            case PAUSE_CLOSE -> {
                CommReq data = req.getData(CommReq.class);
                Boolean enable = data.isEnable();
                this.acctInst.pauseClose(enable);
            }
            case QRY_ACCT -> {
                rsp =req.buildResp(0, this.acctInst.getAcctDetail());
            }
            case QRY_POSITION -> {
                rsp =req.buildResp(0, this.acctInst.getPositions());
            }
            case QRY_TRADE -> {
                rsp =req.buildResp(0, this.acctInst.getTrades());

            }
            case QRY_ORDER -> {
                rsp =req.buildResp(0, this.acctInst.getOrders(false));
            }
            case  ORDER_INSERT -> {
                OrderInsertReq orderInsertReq = req.getData(OrderInsertReq.class);
                Order order =new Order(orderInsertReq.getSymbol(),orderInsertReq.getOffset(),orderInsertReq.getDirection(), orderInsertReq.getVolume(), orderInsertReq.getPrice());
                order.setExchange(orderInsertReq.getExchange());
                this.acctInst.insertOrder(order);
            }
            case ORDER_CANCEL -> {
                OrderCancelReq orderCancelReq = req.getData(OrderCancelReq.class);
                this.acctInst.cancelOrder(orderCancelReq);
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
        Message msg=new Message(Enums.MSG_TYPE.ON_DETAIL,this.acctInst.getAcctDetail());
        this.tcpServer.push(msg);
    }


    @Scheduled(fixedRate = 30000)
    public void printAcct() {
        if(this.acctInst == null)
            return;
        StringBuilder builder = new StringBuilder();
        builder.append(this.acctInst.getAcctDetail().getAcctInfo().toString()+"\n");
        this.acctInst.getPositions().forEach(x -> builder.append(x.toString() + "\n"));
        this.acctInst.getOrders(true).forEach(x -> builder.append(x.toString() + "\n"));
        this.acctInst.getTrades().forEach(x -> builder.append(x.toString() + "\n"));
        log.info("PRINT==> \n{}", builder.toString());
    }

}
