package org.qts.common.entity.acct;

import lombok.Data;

@Data
public class AcctInst {
    private String acctId;
    private String acctName;
    private Boolean status;
    private Boolean tdStatus;
    private Boolean mdStatus;
}
