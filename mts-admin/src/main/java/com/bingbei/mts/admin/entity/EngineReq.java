package com.bingbei.mts.admin.entity;

import lombok.Data;

@Data
public class EngineReq {
    Enums.MSG cmd;
    Object data;
}
