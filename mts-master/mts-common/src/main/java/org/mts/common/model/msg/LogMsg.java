package org.mts.common.model.msg;

import lombok.Data;

@Data
public class LogMsg {
    private String name;
    private String source;
    private String level;
    private String log;
}
