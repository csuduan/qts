package com.bingbei.mts.common.service.impl;

import com.bingbei.mts.common.dao.ContractMapper;
import com.bingbei.mts.common.entity.Contract;
import com.bingbei.mts.common.service.PersistSerivce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersistSerivceImpl implements PersistSerivce {
    @Autowired
    private ContractMapper contractMapper;
    @Override
    public List<Contract> getContracts() {
        return contractMapper.queryContracts();
    }
}
