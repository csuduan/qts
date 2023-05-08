package org.mts.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.mts.admin.entity.po.RouterPo;

import java.util.List;

@Mapper
public interface RouterMapper {
    @Select("SELECT * FROM SYS_ROUTER")
    List<RouterPo> getRouters();
}
