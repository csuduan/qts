package org.qts.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.fts.common.entity.po.RouterPo;

import java.util.List;

@Mapper
public interface SysMapper {
    @Select("SELECT * FROM SYS_ROUTER")
    List<RouterPo> getRouters();
}
