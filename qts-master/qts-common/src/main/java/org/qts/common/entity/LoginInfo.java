package org.qts.common.entity;

import lombok.Data;
import org.qts.common.entity.config.AcctConf;

@Data
public class LoginInfo {
    private String tdType;
    private String address;
    private String brokerId;
    private String acctId;

    private String userId;
    private String useName;
    private String password;
    private String authCode;
    private String appId;

    public LoginInfo(String accoundId, String userInfo, String tdAddress) {
        this.acctId = accoundId;
        var tmp = userInfo.split("\\|");
        this.userId = tmp[0];
        this.password = tmp[1];
        var tmp1 = tdAddress.split("\\|");
        this.tdType = tmp1[0];
        this.address = tmp1[1];
        var tmp2=tmp1[2].split(":");
        this.brokerId = tmp2[0];
        this.appId = tmp2[1];
        this.authCode = tmp2[2];
    }

    public LoginInfo(AcctConf acctConf){
        this.acctId = acctConf.getId();
        this.userId = acctConf.getUser();
        this.password = acctConf.getPwd();
        this.tdType = acctConf.getTdType();
        var tmp1 = acctConf.getTdAddress().split("\\|");
        this.address = tmp1[0];
        this.brokerId = tmp1[1];
        this.appId = tmp1[2];
        this.authCode = tmp1[3];
    }
}
