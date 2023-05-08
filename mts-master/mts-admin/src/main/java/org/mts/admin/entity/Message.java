package org.mts.admin.entity;

import lombok.Data;
import org.mts.common.model.Enums;

@Data
public class Message {
    private int no;
    private Enums.MSG_TYPE type;
    private String actId;
    private String json;
}
