package org.mts.common.model.conf;

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
    private String tdAddress;
    private String  user;
    private String  pwd;
    private Boolean enable;
    private String quotes;

    private List<QuoteConf> quoteConfs=new ArrayList<>();
}
