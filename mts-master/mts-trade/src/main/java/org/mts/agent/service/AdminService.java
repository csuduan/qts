package org.mts.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.mts.common.model.conf.AcctConf;
import org.mts.common.model.event.MessageEvent;
import org.mts.common.model.msg.ConfMsg;
import org.mts.common.model.rpc.Message;
import org.mts.common.rpc.listener.ServerListener;
import org.mts.common.rpc.tcp.TcpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@Slf4j
public class AdminService implements ServerListener {
    @Autowired
    private TradeService acctService;
    @Autowired
    private TcpServer tcpServer;
    @Value("${server.port}")
    private  int port;

    @PostConstruct
    public void start(){
        tcpServer.start(port,this);
    }

    @Override
    public Message onRequest(Message req) {
        Message response=req.buildResp(false,null);
        switch (req.getType()){
            case QRY_CONF -> {
                List<AcctConf> confList=acctService.getAcctConfs();
                ConfMsg confMsg=new ConfMsg();
                confMsg.setAcctConfList(confList);
                response=req.buildResp(true,confMsg);
            }
            default -> {
                //转发给acct
                if(StringUtils.hasLength(req.getRid()))
                response=acctService.request(req.getRid(),req);
            }
        }
        log.info("request => req:{}  rsp:{}",req,response);
        return response;
    }

    @EventListener(MessageEvent.class)
    public void eventHandler(MessageEvent messageEvent){
        this.tcpServer.send((Message) messageEvent.getSource());
    }
}
