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
    private int  startType;//启动类型(1-自动，2-手动)
    private Boolean enable;//是否激活
    //状态信息
    private Boolean status=false;//账户状态
    private String  statusMsg="加载中";//状态描述（账户就绪，Agent未连接,TradeCore未连接）
    private Boolean tdStatus;
    private Boolean mdStatus;


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
