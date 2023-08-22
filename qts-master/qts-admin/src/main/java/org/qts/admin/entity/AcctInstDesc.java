package org.qts.admin.entity;

import lombok.Data;
import org.qts.common.entity.Enums;

@Data
public class AcctInstDesc {
    private String id;
    private String group;
    private String name;
    private Enums.ACCT_STATUS status;
    private Boolean tdStatus;
    private Boolean mdStatus;

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

    private String updateTimes;
}
