package org.qts.admin.core;

import lombok.Data;
import org.qts.common.entity.Enums;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.config.AcctConf;

@Data
public class AcctInstance {
    private AcctConf acctConf;
    private AcctDetail acctDetail;
    private Enums.ACCT_STATUS  acctStatus;
    private Process process;
    public AcctInstance(AcctConf acctConf){

    }
}