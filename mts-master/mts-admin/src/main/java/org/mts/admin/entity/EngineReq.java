package org.mts.admin.entity;

import lombok.Data;
import org.mts.common.model.Enums;

@Data
public class EngineReq {
    Enums.MSG_TYPE cmd;
    Object data;
}
