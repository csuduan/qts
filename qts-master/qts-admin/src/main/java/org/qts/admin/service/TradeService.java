package org.qts.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.qts.admin.dao.ConfMapper;
import org.qts.admin.exception.BizException;
import org.qts.admin.manager.AcctManager;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Message;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.event.MessageEvent;
import org.qts.common.entity.msg.ConfMsg;
import org.qts.common.rpc.uds.UdsClient;
import org.qts.common.utils.SequenceUtil;
import org.qts.common.utils.SpringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
