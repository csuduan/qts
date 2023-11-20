package org.qts.trader.core;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.qts.common.disruptor.FastQueue;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.trade.*;
import org.qts.common.utils.ProcessUtil;
import org.qts.trader.gateway.GatwayFactory;
import org.qts.trader.gateway.MdGateway;
import org.qts.trader.gateway.TdGateway;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class AcctInst {
    public AcctDetail acct;


    private FastQueue fastQueue;

    private MdGateway mdGateway;
    private TdGateway tdGateway;


    public AcctInst(AcctConf conf,FastQueue queue){
        log.info("start acctInst ...");
        this.acct = new AcctDetail(conf);
        this.fastQueue = queue;
        this.tdGateway = GatwayFactory.createTdGateway(this);
        this.mdGateway = GatwayFactory.createMdGateway(this);
    }
    public void close(){
        this.tdGateway.close();
        this.mdGateway.close();
    }

    public AcctDetail getAcctDetail(){
        return this.acct;
    }

    public FastQueue getFastQueue(){
        return this.fastQueue;
    }

    public List<Position> getPositions(){
        return new ArrayList<>(this.acct.getPositions().values());
    }

    public List<Trade> getTrades(){
        return new ArrayList<>(this.acct.getTradeList().values());
    }

    public List<Order> getOrders(boolean isPending){
        if(!isPending)
            return new ArrayList<>(this.acct.getOrders().values());
        else
            return this.acct.getOrders().values().stream().filter(x->x.isFinished()).toList();
    }

    public void connect(boolean status){
        if(status == true){
            this.tdGateway.connect();
            this.mdGateway.connect();
        }else{
            this.tdGateway.close();
            this.mdGateway.close();
        }
    }

    public void pauseOpen(boolean enable){
        this.acct.setPauseOpen(enable);
    }

    public void pauseClose(boolean enable){
        this.acct.setPauseClose(enable);
    }

    public void subContract(List<String> symobls) {
        this.acct.getSubList().addAll(symobls);
        this.mdGateway.subscribe(symobls);
    }



    /**
     * 报单
     */
    public void insertOrder(Order order) {
        Tick lastTick = acct.getTicks().get(order.getSymbol());
        if (order.getPrice() == 0 && lastTick != null) {
            order.setPrice(lastTick.getLastPrice());
        }
        var posDir = order.getPosDirection();
        var pos =this.acct.getPositions().get(order.getSymbol()+posDir);
        if(pos==null){
            //create new pos
            pos = new Position(order.getSymbol(),posDir);
        }
        boolean ret = this.tdGateway.insertOrder(order);
        if (ret)
            pos.update(order);
    }
    /**
     * 撤单
     *
     * @param
     */
    public void cancelOrder(OrderCancelReq req) {
        this.tdGateway.cancelOrder(req);
    }

}
