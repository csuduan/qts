package org.mts.admin.service.impl;

import org.mts.admin.dao.ContractMapper;
import org.mts.admin.entity.Contract;
import org.mts.admin.service.PersistSerivce;
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
