package com.bingbei.mts.common.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
public interface AccountMapper {
    int count();
}
