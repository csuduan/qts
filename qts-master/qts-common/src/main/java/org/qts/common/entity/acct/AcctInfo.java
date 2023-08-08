package org.qts.common.entity.acct;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.qts.common.entity.LoginInfo;
import org.qts.common.entity.config.AcctConf;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账户基本信息
 */
@Data
@NoArgsConstructor
public class AcctInfo {
    private AcctConf acctConf;
    private String id;
    private String name;//账户名称
    private String group;

    private String user;
    //状态信息
    private Boolean status;
    private String  statusMsg="未连接";//状态描述（已就绪，未连接）

    private Boolean tdStatus = false;
    private Boolean mdStatus = false;
    private Boolean pauseOpen = false;
    private Boolean pauseClose = false;

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

    private LocalDate updateTimestamp;//更新时间

    public AcctInfo(AcctConf conf){
        this.acctConf=conf;
        this.id=conf.getId();
        this.name=conf.getName();
        this.group=conf.getGroup();
    }
}
