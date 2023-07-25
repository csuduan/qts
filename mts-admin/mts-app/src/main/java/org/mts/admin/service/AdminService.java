package org.mts.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.mts.common.model.rpc.Message;
import org.mts.common.rpc.listener.ServerListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AdminService implements ServerListener {
    /*@Autowired
    private TradeService acctService;
    @Autowired
    private TcpServer tcpServer;
    @Value("${server.port}")
    private  int port;

    //@PostConstruct
    public void start(){
        tcpServer.start(port,this);
    }*/

    @Override
    public Message onRequest(Message req) {
        Message response=req.buildResp(false,null);
        /*switch (req.getType()){
            case QRY_CONF -> {
                List<AcctConf> confList=acctService.getAcctConfs();
                ConfMsg confMsg=new ConfMsg();
                confMsg.setAcctConfList(confList);
                response=req.buildResp(true,confMsg);
            }
            case QRY_ACCT -> {
                List<AcctInfo> acctInfos=acctService.getAcctInfos();
                response=req.buildResp(true,acctInfos);
            }
            default -> {
                //转发给acct
                if(StringUtils.hasLength(req.getAcctId()))
                response=acctService.request(req.getAcctId(),req);
            }
        }
        log.info("request => req:{}  rsp:{}",req,response);*/
        return response;
    }

    /*@EventListener(MessageEvent.class)
    public void eventHandler(MessageEvent messageEvent){
        this.tcpServer.send((Message) messageEvent.getSource());
    }*/
}
