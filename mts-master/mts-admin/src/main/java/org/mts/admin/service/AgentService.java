package org.mts.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.mts.admin.dao.AcctMapper;
import org.mts.admin.dao.AgentMapper;
import org.mts.admin.entity.po.TradePo;
import org.mts.common.model.acct.AcctInfo;
import org.mts.common.model.acct.TradeAgent;
import org.mts.common.model.Enums;
import org.mts.common.model.conf.AcctConf;
import org.mts.common.model.event.MessageEvent;
import org.mts.common.model.msg.ConfMsg;
import org.mts.common.model.rpc.Message;
import org.mts.common.rpc.listener.ClientListener;
import org.mts.common.rpc.tcp.TcpClient;
import org.mts.common.utils.SpringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * 交易代理服务
 */
@Service
@Slf4j
public class AgentService implements ClientListener {
    @Autowired
    private AgentMapper agentMapper;
    @Autowired
    private AcctMapper acctMapper;

    private Map<String,List<AcctConf>> acctConfMap=new HashMap<>();

    private Map<String, TcpClient> agentClients=new HashMap<>();
    private Map<String, TradeAgent> agents=new HashMap<>();

    @Autowired
    private WebSocketService webSocketService;

    @PostConstruct
    public void init(){
        List<TradePo> pos=agentMapper.getTrades();
        pos.forEach(po->{
            TradeAgent agent=new TradeAgent();
            BeanUtils.copyProperties(po,agent);
            agents.put(agent.getId(),agent);

            if(po.getEnable())
                startAgentClient(agent);
        });

    }

    /**
     * 获取agent列表
     * @return
     */
    public List<TradeAgent> getAgents(){
        return new ArrayList<>(this.agents.values());
    }

    /**
     * 更新agent
     * @param agent
     * @return
     */
    public boolean updateAgent(TradeAgent agent){
        TradePo po=new TradePo();
        BeanUtils.copyProperties(agent,po);
        int count=agentMapper.count(agent.getId());
        if(count==0){
            agentMapper.inert(po);
        }else {
            agentMapper.update(po);
        }
        //重新刷新AgentClient
        if(agent.getEnable())
            this.startAgentClient(agent);
        else
            this.stopAgentClient(agent.getId());
        this.agents.put(agent.getId(),agent);
        return true;
    }

    public List<AcctConf> getAcctConfs(String agent){
        return acctConfMap.get(agent);
    }

    /**
     * 启动agentClient
     * @param agent
     * @return
     */
    private boolean startAgentClient(TradeAgent agent){
        if(!agentClients.containsKey(agent.getId())){
            TcpClient tcpClient=new TcpClient(agent.getId(),agent.getAddress(),this);
            tcpClient.start();
            agentClients.put(agent.getId(),tcpClient);
        }
        return true;
    }

    public List<AcctInfo> getAcctInfos(){
        return null;
    }

    /**
     * 关闭agentClient
     * @param agentId
     * @return
     */
    private boolean stopAgentClient(String agentId){
        if(agentClients.containsKey(agentId)){
            agentClients.get(agentId).close();
            agentClients.remove(agentId);
        }
        return true;
    }


    public Message request(String agentId, Message req){
        var client=this.agentClients.get(agentId);
        Message rsp=client.request(req);
        return rsp;
    }

    public void pushData(Message message){
//        //广播
//        agents.values().forEach(x->{
//            if(x.getStatus()==true){
//                this.request(x.getId(),message)
//            }
//
//        });
    }

    @Override
    public void onStatus(String id,boolean status) {
        log.info("tradeAgent[{}] status[{}] changed!",id,status);
        TradeAgent agent=this.agents.get(id);
        agent.setStatus(status);
        //同步配置信息给agent
        if(status==true){
            //查询账户配置
            var client=this.agentClients.get(agent.getId());
            Message req=new Message(Enums.MSG_TYPE.QRY_CONF,null);
            Message rsp=client.request(req);
            ConfMsg confMsg=rsp.getData(ConfMsg.class);
            this.acctConfMap.put(agent.getId(),confMsg.getAcctConfList());
            log.info("sync conf from trade:{} ret:{}",agent.getId(),rsp.getSuccess());
        }

        Message msg=new Message(Enums.MSG_TYPE.ON_AGENT,agent);
        SpringUtils.pushEvent(new MessageEvent(msg));

    }

    @Override
    public void onMessage(Message msg) {
        log.info("onMessage:{}",msg);
        //推送消息
        SpringUtils.pushEvent(new MessageEvent(msg));
    }

    public Boolean acctOperate(String acctId,Enums.MSG_TYPE type,String data){
        String agentId=this.getAgentByAcct(acctId);
        if(agentId==null){
            log.error("找不到账户[{}]对应的tradeAgent");
            return false;
        }
        Message req=new Message(type,acctId,data);
        Message rsp=this.agentClients.get(agentId).request(req);
        log.info("request==>req:{},rsp:{}",req,rsp);
        return rsp.getSuccess();
    }

    private String getAgentByAcct(String acctId){
        String agentId=null;
        for(var item : acctConfMap.entrySet()){
            if(item.getValue().stream().anyMatch(x->x.getId().equals(acctId))){
                agentId=item.getKey();
                break;
            }
        }
        return agentId;
    }
}
