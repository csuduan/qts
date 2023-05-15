package org.mts.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.mts.common.model.acct.AcctConf;

import java.util.List;

@Mapper
public interface AcctMapper {
    int count();

    @Select("SELECT * FROM ACCT_CONF")
    List<AcctConf> getAcctConfs();
}
