package org.qts.common.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.qts.common.entity.po.CachePo;

import java.util.List;

@Mapper
public interface CacheMapper {
    @Select("SELECT * FROM CACHE WHERE TYPE=#{type}")
    List<CachePo> getCaches(@Param("type") String type);

    @Select("SELECT * FROM CACHE WHERE TYPE=#{type} AND KEY=#{key}")
    CachePo getCacheByKey(@Param("type") String type,@Param("key") String key);

    @Insert({"INSERT INTO CACHE(TYPE, KEY,VALUE) values(#{type}, #{key},#{value})"})
    int insert(CachePo po);

    @Insert({"UPDATE  CACHE SET VALUE=#{value} WHERE TYPE=#{type} AND KEY=#{key}"})
    int update(CachePo po);

    @Select("SELECT COUNT(0) FROM CACHE WHERE TYPE=#{type} AND KEY=#{key}")
    int count(@Param("type") String type,@Param("key") String key);
}
