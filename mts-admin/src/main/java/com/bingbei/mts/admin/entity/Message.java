package com.bingbei.mts.admin.entity;

import lombok.Data;

@Data
public class Message {
    private int no;
    private Operate.Cmd type;
    private String data;
}
