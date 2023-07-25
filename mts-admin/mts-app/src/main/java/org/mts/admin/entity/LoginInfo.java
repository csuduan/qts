package org.mts.admin.entity;

import lombok.Data;

@Data
public class LoginInfo {
    private String tdType;
    private String address;
    private String brokerId;
    private String accoutId;

    private String userId;
    private String useName;
    private String password;
    private String authCode;
    private String appId;

    public LoginInfo(String accoundId, String userInfo, String tdAddress) {
        this.accoutId = accoundId;
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
}
