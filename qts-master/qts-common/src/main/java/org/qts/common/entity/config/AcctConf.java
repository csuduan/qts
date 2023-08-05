package org.qts.common.entity.config;

import lombok.Data;

@Data
public class AcctConf {
    private String id;
    private String group;
    private String name;
    private String user;
    private String pwd;
    private String tdAddress;
    private String tdType;
    private String mdType;
    private String mdAddress;
    private String enable;

}
