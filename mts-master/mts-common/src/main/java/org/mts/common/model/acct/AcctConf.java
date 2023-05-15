package org.mts.common.model.acct;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Objects;

@Data
@EqualsAndHashCode
public class AcctConf {
    private String id;
    private String agent;
    private String group;
    private String tdAddress;
    private String mdAddress;
    private String user;
    private String pwd;
    private Boolean enable;
}
