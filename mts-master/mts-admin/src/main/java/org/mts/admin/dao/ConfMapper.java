package org.mts.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.mts.common.model.acct.AcctConf;

import java.util.List;

@Mapper
public interface ConfMapper {

    @Select("SELECT * FROM CONF_ACCT WHERE OWNER=#{owner}")
    List<AcctConf> getAcctConf(@Param("owner") String owner);


}
