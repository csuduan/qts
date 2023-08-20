package org.qts.common.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.qts.common.entity.config.AcctConf;

import java.util.List;

@Mapper
public interface AcctMapper {
    int count();
    AcctConf queryAcctConf(@Param("acctId") String acctId);

    @Select("SELECT * FROM CONF_ACCT")
    List<AcctConf> getAcctConf();
}
