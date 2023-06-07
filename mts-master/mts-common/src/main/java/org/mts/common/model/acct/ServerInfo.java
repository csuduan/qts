package org.mts.common.model.acct;

import lombok.Data;

@Data
public class ServerInfo {
    private String  id;
    private String  address;
    private Boolean enable;//启用、禁用
    private Boolean status;//连接状态

}
