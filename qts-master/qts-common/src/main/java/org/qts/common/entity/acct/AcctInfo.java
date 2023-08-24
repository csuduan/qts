package org.qts.common.entity.acct;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.qts.common.disruptor.FastQueue;
import org.qts.common.entity.config.AcctConf;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class AcctInfo {
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
}
