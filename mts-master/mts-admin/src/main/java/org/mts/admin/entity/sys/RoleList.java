package org.mts.admin.entity.sys;

import lombok.Data;

import java.util.List;

@Data
public class RoleList {
    private List<Role> list;
    private int total;
}
