package org.qts.common.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.qts.common.entity.po.RouterPo;

import java.util.List;

@Mapper
public interface SysMapper {
    @Select("SELECT * FROM SYS_ROUTER")
    List<RouterPo> getRouters();
}
