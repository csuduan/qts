package org.mts.admin.entity.sys;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class Role {

    private Long createTime;
    private Long updateTime;
    private String creator;
    private String updater;
    private Boolean deleted;
    private Integer tenantId;
    private Integer id;
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    private Integer type;
    private String remark;
    private Integer dataScope;
    private Object dataScopeDeptIds;
}
