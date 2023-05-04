package org.mts.admin.entity;

import lombok.Data;

@Data
public class EngineReq {
    Enums.MSG cmd;
    Object data;
}
