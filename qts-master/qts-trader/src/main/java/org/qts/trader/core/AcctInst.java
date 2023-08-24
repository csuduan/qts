package org.qts.trader.core;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.qts.common.disruptor.FastQueue;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.OrderInstr;
import org.qts.common.entity.trade.Position;
import org.qts.common.entity.trade.Trade;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 账户信息
 */
@Data
@NoArgsConstructor
public class AcctInst {
    private AcctDetail acct;
    //账户队列
    private FastQueue fastQueue;
    //报单指令
    private List<OrderInstr> orderInstrs =new ArrayList<>();
    private LocalDate updateTimestamp;//更新时间

    //public ExecutorService threadPool = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public AcctConf getConf(){
        return this.acct.getConf();
    }
    public AcctInst(AcctConf conf,FastQueue fastQueue){
        this.acct=new AcctDetail(conf);
        this.fastQueue = fastQueue;
    }
}
