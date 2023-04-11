package com.bingbei.mts.admin.entity;

import lombok.Data;

import java.util.Date;

@Data
public class JobInfo {
    private String name;
    private String group;
    private String desc;
    private String cron;
    private String status;
    private Date nextFireTime;
    private Date beforFireTime;
}