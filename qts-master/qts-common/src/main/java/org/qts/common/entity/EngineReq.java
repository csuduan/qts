package org.qts.common.entity;

import lombok.Data;
import org.qts.common.entity.Enums;

@Data
public class EngineReq {
    Enums.MSG_TYPE cmd;
    Object data;
}
