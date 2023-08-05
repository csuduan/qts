package org.qts.common.entity.acct;

import lombok.Data;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.rpc.uds.UdsClient;
import org.springframework.beans.BeanUtils;

@Data
public class AcctInst {
    private String acctId;
    private AcctConf acctConf;
    private AcctInfo acctInfo;
    private UdsClient udsClient;

    public AcctInst(){

    }
    public AcctInst(AcctConf acctConf){
        this.acctId=acctConf.getId();
        this.acctConf=acctConf;
        AcctInfo acctInfo=new AcctInfo();
        BeanUtils.copyProperties(acctConf,acctInfo);
        this.acctInfo=acctInfo;
    }
}
