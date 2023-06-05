package org.mts.common.model.conf;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class QuoteConf {
    private String id;
    private String type;
    private String address;
    private String subList;
    private String user;
    private String pwd;
    private Boolean enable;
}
