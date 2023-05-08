package org.mts.admin.entity.sys;

import lombok.Data;

@Data
public class Agent {
    private String  id;
    private String  name;
    private String  address;
    private Boolean enable;//启用、禁用
    private Boolean status;//连接状态
}
