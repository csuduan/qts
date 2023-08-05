package org.qts.common.entity.po;

import lombok.Data;

@Data
public class CachePo {
    private String type;
    private String key;
    private String value;
}
