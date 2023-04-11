package com.bingbei.mts.admin.entity;

import lombok.Data;

@Data
public class EngineReq {
    Operate.Cmd cmd;
    Object data;
}
