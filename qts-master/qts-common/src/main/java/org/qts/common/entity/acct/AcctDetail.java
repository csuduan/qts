package org.qts.common.entity.acct;

import lombok.Data;
import org.qts.common.disruptor.FastQueue;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.Position;
import org.qts.common.entity.trade.Trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class AcctDetail extends AcctInfo{
    //配置信息
    protected AcctConf conf;

    //持仓列表
    protected Map<String, Position> positions =new HashMap<>();
    //报单列表(挂单)
    protected Map<String, Order> orders = new HashMap<>();
    //成交列表
    protected List<Trade> tradeList = new ArrayList<>();

    public AcctDetail(AcctConf conf){
        this.id=conf.getId();
        this.name=conf.getName();
        this.group=conf.getGroup();
        this.conf = conf;
    }
}
