package org.mts.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.mts.agent.dao.ConfMapper;
import org.mts.common.model.Enums;
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
import org.springframework.util.StringUtils;

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

    private Map<String, UdsClient> acctClients=new HashMap<>();
    private Map<String, AcctInfo> acctInfos=new HashMap<>();
    //private Map<String,AcctConf> acctConfs=new HashMap<>();

    private  final String ACCT  = "ACCT";
    private Map<String,AcctConf> acctConfMap=new HashMap<>();


    @PostConstruct
    public void init(){
        //启动时从缓存加载账户配置
        var list =confMapper.getAcctConf(owner);
        var quoteConfs=confMapper.getQuoteConf();
        for(var acctConf :list){
            if(StringUtils.hasLength(acctConf.getQuotes())){
                for(String quote:acctConf.getQuotes().split(",")){
                    var opt=quoteConfs.stream().filter(q->q.getId().equals(quote)).findFirst();
                    if(opt.isPresent())
                        acctConf.getQuoteConfs().add(opt.get());
                }
            }
            acctConfMap.put(acctConf.getId(),acctConf);
            this.startAcctClient(acctConf);
        }
    }

    public List<AcctConf> getAcctConfs(){
        return new ArrayList<>(acctConfMap.values());
    }

    public boolean updateConf(ConfMsg confMsg){
        //为了不引起已就绪的交易核心因账户配置刷新引起的问题，不主动推新的配置给交易核心
        if(!CollectionUtils.isEmpty(confMsg.getAcctConfList())){
            for(var acctConf :confMsg.getAcctConfList()){
                //this.cacheService.save(ACCT,acctConf.getId(),acctConf);
                this.acctConfMap.put(acctConf.getId(),acctConf);
                //todo 更新CONF表
                this.startAcctClient(acctConf);
            }
        }
        return true;
    }


    /**
     * 启动acctClient
     * @param acctConf
     * @return
     */
    private boolean startAcctClient(AcctConf acctConf){
        if(acctConf.getEnable()==false)
            return false;
        if(!acctInfos.containsKey(acctConf.getId())){
            AcctInfo acctInfo=new AcctInfo();
            BeanUtils.copyProperties(acctConf,acctInfo);
            acctInfos.put(acctConf.getId(),acctInfo);
        }
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
            AcctConf acctConf=this.acctConfMap.get(id);
            if(acctConf!=null){
                var client=this.acctClients.get(id);
                Message rsp=client.request(new Message(Enums.MSG_TYPE.CONF,acctConf));
                log.info("sync conf to acct:{} ret:{}",id,rsp.getSuccess());
            }
        }
        //推送给admin
        SpringUtils.pushEvent(new MessageEvent(new Message(Enums.MSG_TYPE.ON_ACCT,acctInfo)));
    }

    @Override
    public void onMessage(Message msg) {
        log.info("onMessage:{}",msg);
        //todo 推送消息处理
        //再次推送
        SpringUtils.pushEvent(new MessageEvent(msg));
    }
}
