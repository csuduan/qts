package org.mts.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.mts.common.model.Enums;
import org.mts.common.model.acct.AcctConf;
import org.mts.common.model.acct.AcctInfo;
import org.mts.common.model.event.MessageEvent;
import org.mts.common.model.msg.ConfMsg;
import org.mts.common.model.rpc.Message;
import org.mts.common.rpc.listener.ClientListener;
import org.mts.common.rpc.uds.UdsClient;
import org.mts.common.utils.SpringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AcctService implements ClientListener {

    @Autowired
    private CacheService cacheService;
    private Map<String, UdsClient> acctClients=new HashMap<>();
    private Map<String, AcctInfo> acctInfos=new HashMap<>();
    private Map<String,AcctConf> acctConfs=new HashMap<>();

    private  final String ACCT  = "ACCT";

    @PostConstruct
    public void init(){
        //启动时从缓存加载账户配置
        List<AcctConf> acctConfList=cacheService.getCaches(ACCT,AcctConf.class);
        acctConfList.forEach(x->{
            acctConfs.put(x.getId(),x);
        });
    }

    public boolean addConf(ConfMsg confMsg){
        //todo
        return true;
    }
    public void updateAcctConf(AcctConf acctConf){
        var old=acctConfs.get(acctConf.getId());
        if(acctConf.equals(old)){
            return;
        }
        acctConfs.put(acctConf.getId(),acctConf);
        AcctInfo acctInfo=new AcctInfo();
        BeanUtils.copyProperties(acctInfo,acctInfo);
        acctInfos.put(acctConf.getId(),acctInfo);
        if(!this.acctClients.containsKey(acctConf.getId())){
            this.startAcctClient(acctConf);
        }
        //刷新缓存
        cacheService.save(ACCT,acctConf.getId(),acctConf);
    }

    /**
     * 启动acctClient
     * @param acctConf
     * @return
     */
    private boolean startAcctClient(AcctConf acctConf){
        if(!acctClients.containsKey(acctConf.getId())){
            UdsClient udsClient=new UdsClient(acctConf.getId(),this);
            udsClient.start();
            acctClients.put(acctConf.getId(),udsClient);
        }
        return true;
    }

    /**
     * 关闭acctClient
     * @param acctId
     * @return
     */
    private boolean stopAcctClient(String acctId){
        if(acctClients.containsKey(acctId)){
            acctClients.get(acctId).close();
            acctClients.remove(acctId);
        }
        return true;
    }


    public Message request(String acctId, Enums.MSG_TYPE type,Object data){
        var client=this.acctClients.get(acctId);
        Message req=new Message(type,data);
        Message rsp=client.request(req);
        return rsp;
    }

    public Message request(String acctId,Message req){
        var client=this.acctClients.get(acctId);
        return  client.request(req);
    }


    @Override
    public void onStatus(String id,boolean status) {
        AcctInfo acctInfo=this.acctInfos.get(id);
        acctInfo.setStatus(status);
        if(status==true){
            //同步配置信息给acct实例

            /*SyncMsg syncMsg=new SyncMsg();
            syncMsg.setAcctConfList(this.getAcctConfs(agent.getId()));
            var client=this.agentClients.get(agent.getId());
            Request req=new Request(Enums.MSG_TYPE.SYNC,syncMsg);
            Response rsp=client.request(req);
            log.info("sync conf to agent:{} ret:{}",agent.getId(),rsp.getData(Boolean.class));*/
        }
    }

    @Override
    public void onMessage(Message msg) {
        log.info("onMessage:{}",msg);
        //todo 推送消息处理
        //再次推送
        SpringUtils.pushEvent(new MessageEvent(msg));
    }
}
