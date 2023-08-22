package org.qts.admin.core;

import lombok.Data;
import org.qts.admin.entity.AcctInstDesc;
import org.qts.common.entity.Enums;
import org.qts.common.entity.acct.AcctInfo;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class AcctInst {
    private String id;
    private String group;
    private String name;
    private Enums.ACCT_STATUS status;
    private Boolean tdStatus;
    private Boolean mdStatus;

    private AcctInfo acctInfo;
    private Integer pid;
    private String updateTimes;

    public AcctInstDesc getAcctInstDesc(){
        AcctInstDesc acctInstDesc =new AcctInstDesc();
        BeanUtils.copyProperties(this,acctInstDesc);
        BeanUtils.copyProperties(this.acctInfo,acctInstDesc);
        return acctInstDesc;
    }
}
