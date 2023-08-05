package org.qts.common.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.qts.common.entity.Contract;

import java.util.List;

@Mapper
public interface ContractMapper {
    int insertContracts(@Param("start")List<Contract> list);
    List<Contract> queryContracts();
}
