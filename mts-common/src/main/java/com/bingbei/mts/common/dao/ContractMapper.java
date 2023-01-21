package com.bingbei.mts.common.dao;

import com.bingbei.mts.common.entity.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContractMapper {
    int insertContracts(@Param("start")List<Contract> list);
    List<Contract> queryContracts();
}
