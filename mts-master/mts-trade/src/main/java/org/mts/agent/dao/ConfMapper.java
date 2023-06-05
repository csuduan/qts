package org.mts.agent.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.mts.agent.model.CachePo;
import org.mts.common.model.conf.AcctConf;
import org.mts.common.model.conf.QuoteConf;

import java.util.List;

@Mapper
public interface ConfMapper {

    @Select("SELECT * FROM CONF_ACCT WHERE OWNER=#{owner}")
    List<AcctConf> getAcctConf(@Param("owner") String owner);

    @Select("SELECT * FROM CONF_QUOTE")
    List<QuoteConf> getQuoteConf();

}
