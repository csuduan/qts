package org.qts.common.entity.acct;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.qts.common.disruptor.FastQueue;
import org.qts.common.entity.LoginInfo;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.Position;
import org.qts.common.entity.trade.Trade;
import org.qts.common.utils.SpringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账户信息
 */
@Data
@NoArgsConstructor
public class AcctInfo {

    /**
     * 账户基本信息
     */
    private AcctConf acctConf;
    private String id;
    private String name;//账户名称
    private String group;

    //资金信息
    private Double available ;
    private Double balance;
    private Double preBalance;
    private Double mv;
    private Double balanceProfit;
    private Double closeProfit;
    private Double margin;//保证金
    private Double marginRate;//保证金占比
    private Double commission;

    //持仓列表
    private Map<String, Position> positions =new HashMap<>();
    //挂单列表
    private Map<String, Order> pendingOrders = new HashMap<>();
    //成交列表
    private List<Trade> tradeList = new ArrayList<>();
    //报单列表
    private List<Order> orderList = new ArrayList<>();


    /**
     * 账户实例相关
     */
    //账户队列
    private FastQueue fastQueue;
    //状态信息
    private Boolean status;
    private String  statusMsg="未连接";//状态描述（已就绪，未连接）
    private Boolean tdStatus = false;
    private Boolean mdStatus = false;
    private Boolean pauseOpen = false;
    private Boolean pauseClose = false;
    private LocalDate updateTimestamp;//更新时间




    public AcctInfo(AcctConf conf){
        this.acctConf=conf;
        this.id=conf.getId();
        this.name=conf.getName();
        this.group=conf.getGroup();
        String waitStrategy= SpringUtils.getContext().getEnvironment().getProperty("fastQueue.WaitStrategy");
        this.fastQueue = new FastQueue(waitStrategy);
    }
}
