package org.qts.common.entity.acct;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.qts.common.disruptor.FastQueue;
import org.qts.common.entity.Contract;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.Position;
import org.qts.common.entity.trade.Tick;
import org.qts.common.entity.trade.Trade;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Getter
@NoArgsConstructor
public class AcctDetail extends AcctInfo{
    //配置信息
    private AcctConf conf;
    //持仓列表
    protected Map<String, Position> positions =new HashMap<>();
    //报单列表(挂单)
    protected Map<String, Order> orders = new HashMap<>();
    //成交列表
    protected Map<String,Trade> tradeList = new HashMap<>();
    //行情列表
    private Map<String, Tick> ticks = new HashMap<>();
    //合约列表
    private Map<String,Contract> contracts = new HashMap<>();
    //订阅列表
    private Set<String> subList = new HashSet<>();
    private int versions;


    @ToString.Exclude
    @JSONField(serialize = false)
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    @ToString.Exclude
    @JSONField(serialize = false)
    private FastQueue fastQueue;

    public AcctDetail(AcctConf conf){
        this.id=conf.getId();
        this.name=conf.getName();
        this.group=conf.getGroup();
        this.conf = conf;

        if(StringUtils.hasLength(conf.getSubList())){
            this.subList.addAll(Arrays.asList(conf.getSubList().split(",")));
        }
    }

    public AcctDetail(AcctConf conf,FastQueue queue){
        this.id=conf.getId();
        this.name=conf.getName();
        this.group=conf.getGroup();
        this.conf = conf;
        this.fastQueue =queue;
    }

    public AcctInfo getAcctInfo(){
        AcctInfo acctInfo = new AcctInfo();
        BeanUtils.copyProperties(this,acctInfo);
        return acctInfo;
    }
}
