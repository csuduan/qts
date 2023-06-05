package org.mts.admin.dao;

import org.apache.ibatis.annotations.*;
import org.mts.admin.entity.po.TradePo;

import java.util.List;

@Mapper
public interface AgentMapper {
    @Select("SELECT * FROM SYS_TRADE")
    List<TradePo> getTrades();


    @Select("SELECT COUNT(0) FROM SYS_TRADE WHERE ID=#{id}")
    int count(@Param("id") String id);

    @Insert("")
    int inert(TradePo po);

    @Update("")
    int update(TradePo po);
}
