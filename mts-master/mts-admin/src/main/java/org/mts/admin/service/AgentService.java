package org.mts.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.mts.admin.comm.AgentClient;
import org.mts.admin.dao.AgentMapper;
import org.mts.admin.entity.WsMessage;
import org.mts.admin.entity.po.AgentPo;
import org.mts.admin.entity.sys.Agent;
import org.mts.admin.event.AgentEvent;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AgentService {
    @Autowired
    private AgentMapper agentMapper;
    private Map<String,AgentClient> agentClients=new HashMap<>();

    @Autowired
    private WebSocketService webSocketService;

    @PostConstruct
    public void init(){
        List<AgentPo> pos=agentMapper.getAgents();
        pos.forEach(po->{
            if(po.getEnable())
                startAgentClient(po);
        });

    }

    /**
     * 获取agent列表
     * @return
     */
    public List<Agent> getAgents(){
        List<AgentPo> pos=agentMapper.getAgents();
        List<Agent> res=new ArrayList<>();
        pos.forEach(po->{
            Agent agent=new Agent();
            BeanUtils.copyProperties(po,agent);
            if(this.agentClients.containsKey(po.getId()))
                agent.setStatus(this.agentClients.get(po.getId()).getStatus());
            res.add(agent);
        });
        return res;
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
        po=agentMapper.getAgent(agent.getId());
        if(agent.getEnable())
            this.startAgentClient(po);
        else
            this.stopAgentClient(po.getId());
        return true;
    }

    /**
     * 启动agentClient
     * @param agentPo
     * @return
     */
    private boolean startAgentClient(AgentPo agentPo){
        if(!agentClients.containsKey(agentPo.getId())){
            AgentClient agentClient=new AgentClient(agentPo);
            agentClient.start();
            agentClients.put(agentPo.getId(),agentClient);
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

    @EventListener(AgentEvent.class)
    public void consumer(AgentEvent msgEvent) {
        webSocketService.push(new WsMessage(0,msgEvent.getAgent()));
    }


}
