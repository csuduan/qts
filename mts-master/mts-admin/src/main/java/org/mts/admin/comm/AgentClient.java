package org.mts.admin.comm;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.mts.admin.entity.po.AgentPo;
import org.mts.admin.entity.sys.Agent;
import org.mts.admin.event.AgentEvent;
import org.mts.common.ipc.handler.CustomHandler;
import org.mts.common.ipc.tcp.TcpClient;
import org.mts.common.utils.SpringUtils;
import org.springframework.beans.BeanUtils;

@Slf4j
public class AgentClient implements CustomHandler {
    private String name;
    private String address;
    private TcpClient tcpClient;
    private boolean enable=true;
    private boolean connected=false;
    private AgentPo po;


    public  AgentClient(AgentPo po){
        this.po=po;
        this.name=po.getName();
        this.address=po.getAddress();
    }
    public void start(){
        log.info("start agentClient[{}]",name);
        Thread thread=new Thread(()->{
            while (enable){
                try {
                    if(!this.connected){
                        this.connect();
                    }
                    Thread.sleep(10000);
                }catch (Exception ex){

                }
            }
        });
        thread.start();
    }

    @Synchronized
    private void connect(){
        if(connected==true){
            log.warn("agentClient[{}] already connected!!");
            return;
        }
        log.warn("agentClient[{}] connecting...",name);
        if(tcpClient!=null){
            this.tcpClient.close();
            this.tcpClient=null;
        }

        tcpClient=new TcpClient(name,address,this);
        tcpClient.connect();
    }

    public void close(){
        log.info("close  agentClient[{}]",name);
        this.enable=false;
        tcpClient.close();
        tcpClient=null;
    }

    public Boolean getStatus(){
        return this.connected;
    }


    @Override
    public void onStatus(boolean status) {
        log.info("agentClient[{}] onStatus:{}",name,status);
        this.connected=status;
        Agent agent=new Agent();
        BeanUtils.copyProperties(po,agent);
        agent.setStatus(connected);
        SpringUtils.pushEvent(new AgentEvent(this,agent));
    }

    @Override
    public void onData(Object data) {
        log.info("agentClient[{}] onData:{}",name,data);
    }
}
