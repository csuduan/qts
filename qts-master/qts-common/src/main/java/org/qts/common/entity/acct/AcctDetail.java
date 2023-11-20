package org.qts.common.entity.acct;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.qts.common.entity.Contract;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.trade.Order;
import org.qts.common.entity.trade.Position;
import org.qts.common.entity.trade.Tick;
import org.qts.common.entity.trade.Trade;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.util.*;

@Getter
@NoArgsConstructor
public class AcctDetail extends AcctInfo{
    public AcctConf conf;

    /**
     * 账户基本信息
     */
    protected String id;
    protected String name;//账户名称
    protected String group;

    //资金信息
    protected Double available ;
    protected Double balance;
    protected Double preBalance;
    protected Double mv;
    protected Double balanceProfit;
    protected Double closeProfit;
    protected Double margin;//保证金
    protected Double marginRate;//保证金占比
    protected Double commission;

    //状态信息
    protected Boolean tdStatus = false;
    protected Boolean mdStatus = false;
    protected Boolean pauseOpen = false;
    protected Boolean pauseClose = false;

    //持仓列表
    protected Map<String, Position> positions =new HashMap<>();
    //行情列表
    private Map<String, Tick> ticks = new HashMap<>();
    //合约列表
    private Map<String,Contract> contracts = new HashMap<>();
    //订阅列表
    private Set<String> subList = new HashSet<>();


    public AcctDetail(AcctConf conf){
        this.conf = conf;
        this.id=conf.getId();
        this.name=conf.getName();
        this.group=conf.getGroup();
        if(StringUtils.hasLength(conf.getSubList())){
            this.subList.addAll(Arrays.asList(conf.getSubList().split(",")));
        }
    }

    public AcctInfo getAcctInfo(){
        AcctInfo acctInfo = new AcctInfo();
        BeanUtils.copyProperties(this,acctInfo);
        return acctInfo;
    }

    public void onTrade(Trade trade){
        var pos = this.positions.get(trade.getSymbol()+trade.getPosDirection());
        if(pos!=null)
            pos.update(trade);
    }

    public void onOrder(Order order){
        var posDir = order.getPosDirection();
        var pos =this.positions.get(order.getSymbol()+posDir);
        if(pos!=null){
            pos.update(order);
        }
    }
}
