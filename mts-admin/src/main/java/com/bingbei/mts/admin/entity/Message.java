package com.bingbei.mts.admin.entity;

import lombok.Data;

@Data
public class Message {
    private int no;
    private Enums.MSG type;
    private String actId;
    private String json;
}
