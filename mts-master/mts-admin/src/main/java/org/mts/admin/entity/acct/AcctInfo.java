package org.mts.admin.entity.acct;

import lombok.Data;
import org.mts.admin.entity.Enums;

import java.math.BigDecimal;

@Data
public class AcctInfo {
    private String group;
    private String id;
    private String name;
    private String user;
    private String pwd;
    private String type;
    private Boolean autoStart;
    private Boolean enable;//是否激活
    private Enums.ACCT_STATUS  acctStatus;
    private Enums.API_STATUS apiStatus;

    private BigDecimal balance=BigDecimal.ZERO;
    private BigDecimal mv=BigDecimal.ZERO;
    private BigDecimal balanceProfit=BigDecimal.ZERO;
    private BigDecimal closeProfit=BigDecimal.ZERO;
    private BigDecimal margin=BigDecimal.ZERO;//保证金
    private BigDecimal risk=BigDecimal.ZERO;//风险度

}
