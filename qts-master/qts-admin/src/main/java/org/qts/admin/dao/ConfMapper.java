package org.qts.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.fts.common.entity.config.AcctConf;

import java.util.List;

@Mapper
public interface ConfMapper {

    @Select("SELECT * FROM TRADE_ACCT")
    List<AcctConf> getAcctConf();


}
