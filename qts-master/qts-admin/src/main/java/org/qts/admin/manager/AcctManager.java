package org.qts.admin.manager;

import lombok.extern.slf4j.Slf4j;
import org.qts.admin.core.AcctInst;
import org.qts.admin.entity.AcctInstDesc;
import org.qts.common.dao.AcctMapper;
import org.qts.admin.exception.BizException;
import org.qts.admin.service.WebSocketService;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Message;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.event.MessageEvent;
import org.qts.common.rpc.tcp.server.MsgHandler;
import org.qts.common.rpc.tcp.server.TcpServer;
import org.qts.common.utils.ProcessUtil;
import org.qts.common.utils.SequenceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
                AcctInst acctInst=new AcctInst();
                acctInst.setId(acctConf.getId());
                acctInst.setGroup(acctInst.getGroup());
                acctInst.setName(acctInst.getName());
                acctInst.setStatus(Enums.ACCT_STATUS.UNSTARTED);
                acctInst.setAcctInfo(new AcctInfo(acctConf));
                acctInst.setUpdateTimes(LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
                acctInst.init();
                acctInstanceMap.put(acctInst.getId(), acctInst);
            }
        }
    }

    public List<AcctConf> getAcctConfs(){
        return this.acctConfMap.values().stream().toList();
    }
    public List<AcctInstDesc> getAcctInstDescs(){
        return acctInstanceMap.values().stream().map(x->x.getAcctInstDesc()).toList();
    }
    public AcctInfo getAcctDetail(String acctId){
        return acctInstanceMap.get(acctId).getAcctInfo();
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
