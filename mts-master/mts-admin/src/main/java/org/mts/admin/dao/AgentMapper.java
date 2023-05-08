package org.mts.admin.dao;

import org.apache.ibatis.annotations.*;
import org.mts.admin.entity.po.AgentPo;

import java.util.List;

@Mapper
public interface AgentMapper {
    @Select("SELECT * FROM SYS_AGENT")
    List<AgentPo> getAgents();

    @Select("SELECT * FROM SYS_AGENT WHERE ID=#{id}")
    AgentPo getAgent(@Param("id") String id);

    @Select("SELECT COUNT(0) FROM SYS_AGENT WHERE ID=#{id}")
    int count(@Param("id") String id);

    @Insert("")
    int inert(AgentPo po);

    @Update("")
    int update(AgentPo po);
}
