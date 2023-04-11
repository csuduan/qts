package com.bingbei.mts.admin.entity;

import lombok.Data;

import java.util.List;

@Data
public class UserInfo {
    private List<String> roles;
    private String introduction;
    private String avatar;
    private String name;
}
