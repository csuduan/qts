package org.qts.admin.manager;

import lombok.extern.slf4j.Slf4j;
import org.qts.admin.core.AcctInst;
import org.qts.admin.entity.AcctDesc;
import org.qts.common.dao.AcctMapper;
import org.qts.admin.exception.BizException;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Message;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.config.AcctConf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * 账户管理
 */
@Service
@Slf4j
public class AcctManager {
    @Autowired
    private AcctMapper acctMapper;
    private Map<String, AcctConf> acctConfMap=new HashMap<>();
    private Map<String, AcctInst> acctInstanceMap=new HashMap<>();

    public AcctManager(){
    }

    @PostConstruct
    public void init(){
        //初始化
        var list =acctMapper.getAcctConf();
        for(AcctConf acctConf :list){
            acctConfMap.put(acctConf.getId(),acctConf);
            if(acctConf.getEnable()){
                AcctInst acctInst=new AcctInst(acctConf);
                acctInstanceMap.put(acctInst.getId(), acctInst);
            }
        }
    }

    public List<AcctConf> getAcctConfs(){
        return this.acctConfMap.values().stream().toList();
    }
    public List<AcctDesc> getAcctInstDescs(){
        return acctInstanceMap.values().stream().map(x->x.getAcctInstDesc()).toList();
    }
    public AcctDetail getAcctDetail(String acctId){
        if(!this.acctInstanceMap.containsKey(acctId))
            throw  new BizException("账户不存在");
        var acctInst=acctInstanceMap.get(acctId);
        return acctInst.getAcct();
    }

    public Message request(String acctId, Message req){
        if(!this.acctInstanceMap.containsKey(acctId)
                || this.acctInstanceMap.get(acctId).getStatus()!= Enums.ACCT_STATUS.READY )
            throw  new BizException("账户未就绪");
        AcctInst inst = this.acctInstanceMap.get(acctId);
        return  inst.request(req);
    }

    @Scheduled(fixedRate = 3000)
    public void checkInstStatus(){
        this.acctInstanceMap.values().forEach(inst->{
            inst.chekcInstStatus();
        });
    }
}
