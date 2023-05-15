package org.mts.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.mts.admin.dao.AcctMapper;
import org.mts.admin.dao.AgentMapper;
import org.mts.admin.entity.po.AgentPo;
import org.mts.common.model.acct.Agent;
import org.mts.common.model.Enums;
import org.mts.common.model.acct.AcctConf;
import org.mts.common.model.event.MessageEvent;
import org.mts.common.model.msg.SyncMsg;
import org.mts.common.model.rpc.Message;
import org.mts.common.rpc.listener.ClientListener;
import org.mts.common.rpc.tcp.TcpClient;
import org.mts.common.utils.SpringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AgentService implements ClientListener {
    @Autowired
    private AgentMapper agentMapper;
    @Autowired
    private AcctMapper acctMapper;

    private Map<String, TcpClient> agentClients=new HashMap<>();
    private Map<String,Agent> agents=new HashMap<>();

    @Autowired
    private WebSocketService webSocketService;

    @PostConstruct
    public void init(){
        List<AgentPo> pos=agentMapper.getAgents();
        pos.forEach(po->{
            Agent agent=new Agent();
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
    public List<Agent> getAgents(){
        return new ArrayList<>(this.agents.values());
    }

    /**
     * 更新agent
     * @param agent
     * @return
     */
    public boolean updateAgent(Agent agent){
        AgentPo po=new AgentPo();
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
        List<AcctConf> acctConfList=acctMapper.getAcctConfs();
        if(StringUtils.hasLength(agent)){
            acctConfList=acctConfList.stream().filter(x->agent.equals(x.getAgent())).toList();
        }
        return acctConfList;
    }

    /**
     * 启动agentClient
     * @param agent
     * @return
     */
    private boolean startAgentClient(Agent agent){
        if(!agentClients.containsKey(agent.getId())){
            TcpClient tcpClient=new TcpClient(agent.getId(),agent.getAddress(),this);
            tcpClient.start();
            agentClients.put(agent.getId(),tcpClient);
        }
        return true;
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
        Agent agent=this.agents.get(id);
        agent.setStatus(status);
        //同步配置信息给agent
        if(status==true){
            SyncMsg syncMsg=new SyncMsg();
            syncMsg.setAcctConfList(this.getAcctConfs(agent.getId()));
            var client=this.agentClients.get(agent.getId());
            Message req=new Message(Enums.MSG_TYPE.SYNC,syncMsg);
            Message rsp=client.request(req);
            log.info("sync conf to agent:{} ret:{}",agent.getId(),rsp.getSuccess());
        }

        Message msg=new Message(Enums.MSG_TYPE.AGENT,agent);
        SpringUtils.pushEvent(new MessageEvent(msg));

    }

    @Override
    public void onMessage(Message msg) {
        log.info("onMessage:{}",msg);

        //推送消息
        SpringUtils.pushEvent(new MessageEvent(msg));
    }
}
