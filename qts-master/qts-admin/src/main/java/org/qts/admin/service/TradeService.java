package org.qts.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.qts.common.dao.ConfMapper;
import org.qts.admin.manager.AcctManager;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Message;
import org.qts.common.entity.acct.AcctInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.List;

@Service
@Slf4j
public class TradeService  {

    @Autowired
    private ConfMapper confMapper;
    @Autowired
    private AcctManager acctManager;


    @PostConstruct
    public void init(){
    }
    public List<AcctInfo> getAcctInfos(){
        return acctManager.getAcctInfos();
    }


    public Message request(String acctId, Enums.MSG_TYPE type, Object data){
        return acctManager.request(acctId,new Message(type,data));
    }

}
