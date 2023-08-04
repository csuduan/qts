package org.mts.common.model.acct;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AcctInfo {
    private String owner;
    private String group;
    private String id;
    private String user;
    private String type;
    private Boolean enable;//是否激活
    //状态信息
    private Boolean status;
    private String  statusMsg="未连接";//状态描述（已就绪，未连接）
    private Boolean tdStatus = false;
    private Boolean mdStatus = false;
    private Boolean pauseOpen = false;
    private Boolean pauseClose = false;

    //资金信息
    private BigDecimal balance=BigDecimal.ZERO;
    private BigDecimal mv=BigDecimal.ZERO;
    private BigDecimal balanceProfit=BigDecimal.ZERO;
    private BigDecimal closeProfit=BigDecimal.ZERO;
    private BigDecimal margin=BigDecimal.ZERO;//保证金
    private BigDecimal marginRate=BigDecimal.ZERO;//保证金占比
    private BigDecimal fee=BigDecimal.ZERO;

    private LocalDate updateTimestamp;//更新时间
}
