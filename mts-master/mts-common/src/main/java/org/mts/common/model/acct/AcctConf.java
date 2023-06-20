package org.mts.common.model.acct;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode
public class AcctConf {
    private String id;
    private String owner;
    private String group;
    private String tdType;
    private String tdAddress;
    private String  user;
    private String  pwd;
    private String mdType;
    private String mdAddress;
    private String subList;
    private Boolean enable;

}
