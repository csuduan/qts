package org.mts.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.mts.admin.dao.ConfMapper;
import org.mts.admin.exception.BizException;
import org.mts.common.model.Enums;
import org.mts.common.model.acct.AcctInst;
import org.mts.common.model.conf.AcctConf;
import org.mts.common.model.acct.AcctInfo;
import org.mts.common.model.event.MessageEvent;
import org.mts.common.model.msg.ConfMsg;
import org.mts.common.model.rpc.Message;
import org.mts.common.rpc.listener.ClientListener;
import org.mts.common.rpc.uds.UdsClient;
import org.mts.common.utils.SpringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TradeService implements ClientListener {

    @Autowired
    private CacheService cacheService;
    @Autowired
    private ConfMapper confMapper;

    @Value("${server.name}")
    private String owner;

    private Map<String, AcctInst> acctInstMap=new HashMap<>();

    @PostConstruct
    public void init(){
    }
    public List<AcctInfo> getAcctInfos(){
        List<AcctInfo> result=new ArrayList<>();
        acctInstMap.forEach((x,y)->result.add(y.getAcctInfo()));
        return result;
    }

    public boolean updateConf(ConfMsg confMsg){
        //为了不引起已就绪的交易核心因账户配置刷新引起的问题，不主动推新的配置给交易核心
        if(!CollectionUtils.isEmpty(confMsg.getAcctConfList())){
            for(var acctConf :confMsg.getAcctConfList()){
                //this.cacheService.save(ACCT,acctConf.getId(),acctConf);
                //this.acctConfMap.put(acctConf.getId(),acctConf);
                //todo 更新CONF表
                //this.startAcctClient(acctConf);
            }
        }
        return true;
    }


    /**
     * 启动acctClient
     * @param acctConf
     * @return
     */
    public boolean startAcctClient(AcctConf acctConf){
        if(acctConf.getEnable()==false)
            return false;
        if(this.acctInstMap.containsKey(acctConf.getId()))
            return false;

        AcctInst acctInst=new AcctInst(acctConf);
        this.acctInstMap.put(acctInst.getAcctId(),acctInst);

        UdsClient udsClient=new UdsClient(acctConf.getId(),this);
        acctInst.setUdsClient(udsClient);
        udsClient.start();

        return true;
    }

    /**
     * 关闭acctClient
     * @param acctId
     * @return
     */
    private boolean stopAcctClient(String acctId){
        if(this.acctInstMap.containsKey(acctId)){
            this.acctInstMap.get(acctId).getUdsClient().close();
            this.acctInstMap.remove(acctId);
        }
        return true;
    }

    public Message request(String acctId, Enums.MSG_TYPE type,Object data){
        Message req=new Message(type,data);
        log.info("request==>req:{}",req);
        Message rsp=this.request(acctId,req);
        log.info("request==>rsp:{}",rsp);
        return rsp;
    }

    public Message request(String acctId,Message req){
        if(!this.acctInstMap.containsKey(acctId)
                || !this.acctInstMap.get(acctId).getAcctInfo().getStatus())
            throw  new BizException("账户未连接");
        var client=this.acctInstMap.get(acctId).getUdsClient();
        return  client.request(req);
    }


    @Override
    public void onStatus(String id,boolean status) {
        log.info("acct client[{}] status changed:{}",id,status);
        AcctInst acctInst=this.acctInstMap.get(id);
        if(status==true){
            acctInst.getAcctInfo().setStatus(true);
            acctInst.getAcctInfo().setStatusMsg("已就绪");
            //与ACCT进行一次同步
            AcctConf acctConf=acctInst.getAcctConf();
            if(acctConf!=null){
                Message rsp=this.request(id,Enums.MSG_TYPE.SYNC,acctConf);
                AcctInfo acctInfo=rsp.getData(AcctInfo.class);
                //BeanUtils.copyProperties(acctInfo,acctInst.getAcctInfo());
                acctInst.getAcctInfo().setTdStatus(acctInfo.getTdStatus());
                log.info("sync to acct:{} ret:{}",id,rsp.getSuccess());
            }
        }else{
            acctInst.getAcctInfo().setStatus(false);
            acctInst.getAcctInfo().setStatusMsg("未连接");
        }
        //推送给admin
        SpringUtils.pushEvent(new MessageEvent(new Message(Enums.MSG_TYPE.ON_ACCT,acctInst.getAcctInfo())));
    }

    @Override
    public void onMessage(Message msg) {
        log.info("onMessage:{}",msg);
        AcctInst acctInst=this.acctInstMap.get(msg.getAcctId());
        switch (msg.getType()){
            case ON_ACCT -> {
                //Message rsp=this.request(acctInst.getAcctId(),new Message(Enums.MSG_TYPE.QRY_ACCT,null));
                AcctInfo acctInfo=msg.getData(AcctInfo.class);
                //BeanUtils.copyProperties(acctInfo,acctInst.getAcctInfo());
                acctInst.getAcctInfo().setTdStatus(acctInfo.getTdStatus());
                SpringUtils.pushEvent(new MessageEvent(new Message(Enums.MSG_TYPE.ON_ACCT,acctInst.getAcctInfo())));
            }
        }
        //再次推送
        //SpringUtils.pushEvent(new MessageEvent(msg));
    }
}
