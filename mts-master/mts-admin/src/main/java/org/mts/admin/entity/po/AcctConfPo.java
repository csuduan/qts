package org.mts.admin.entity.po;

import lombok.Data;

@Data
public class AcctConfPo {
    private String id;
    private String agent;
    private String group;
    private String tdAddress;
    private String mdAddress;
    private Boolean enable;
    private String user;
    private String pwd;
}
