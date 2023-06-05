package org.mts.agent.model;

import lombok.Data;

@Data
public class CachePo {
    private String type;
    private String key;
    private String value;
}
