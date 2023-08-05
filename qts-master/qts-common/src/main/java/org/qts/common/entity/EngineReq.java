package org.qts.common.entity;

import lombok.Data;
import org.fts.common.model.Enums;

@Data
public class EngineReq {
    Enums.MSG_TYPE cmd;
    Object data;
}
