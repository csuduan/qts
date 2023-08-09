package org.qts.admin.manager;

import lombok.extern.slf4j.Slf4j;
import org.qts.admin.core.AcctInstance;
import org.qts.common.dao.ConfMapper;
import org.qts.admin.exception.BizException;
import org.qts.admin.service.WebSocketService;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Message;
import org.qts.common.entity.acct.AcctDetail;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.event.MessageEvent;
import org.qts.common.rpc.tcp.server.ServerListener;
import org.qts.common.rpc.tcp.server.TcpServer;
import org.qts.common.utils.ProcessUtil;
import org.qts.common.utils.SequenceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * 账户管理
 */
@Service
@Slf4j
public class AcctManager implements ServerListener {
    @Autowired
    private ConfMapper confMapper;
    @Autowired
    private WebSocketService webSocketService;


    private Map<String, AcctConf> acctConfMap=new HashMap<>();
    private Map<String, AcctInstance> acctInstanceMap=new HashMap<>();
    private TcpServer tcpServer;

    @Value("${tcpServer.port:8083}")
    private int port;

    public AcctManager(){

    }

    @PostConstruct
    public void init(){
        //启动tcpServer
        tcpServer=new TcpServer();
        tcpServer.start(port,this);

        //初始化
        var list =confMapper.getAcctConf();
        for(AcctConf acctConf :list){
            acctConfMap.put(acctConf.getId(),acctConf);
            this.startAcctInstance(acctConf);
        }
    }

    public List<AcctInfo> getAcctInfos(){
        return acctInstanceMap.values().stream().map(x->(AcctInfo)x.getAcctDetail()).toList();
    }
    public AcctDetail getAcctDetail(String acctId){
        return acctInstanceMap.get(acctId).getAcctDetail();
    }

    public boolean startAcctInstance(AcctConf acctConf){
        if(!"Y".equals(acctConf.getEnable()))
            return false;
        if(this.acctInstanceMap.containsKey(acctConf.getId()))
            return false;

        AcctInstance acctInstance=new AcctInstance(acctConf);
        this.acctInstanceMap.put(acctConf.getId(),acctInstance);
        //检查账户进程是否存在
        int pid=ProcessUtil.getProcess("qts-trader",acctConf.getId());
        if(pid>0){
            log.info("trader[{}]已启动",acctConf.getId());
        }else{
            List<String> cmds = new ArrayList<String>();
            cmds.add("java");
            cmds.add("-Dacct="+acctConf.getId());
            cmds.add("-jar");
            //cmds.add("-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005");
            cmds.add("qts-trader/target/qts-trader.jar");
            pid=ProcessUtil.startProcess(cmds)  ;
        }
        acctInstance.setPid(pid);
        return true;
    }


    public Message request(String acctId, Message req){
        if(!this.acctInstanceMap.containsKey(acctId)
                || this.acctInstanceMap.get(acctId).getAcctStatus()!= Enums.ACCT_STATUS.READY )
            throw  new BizException("账户未就绪");

        String requestId = SequenceUtil.getLocalSerialNo(16);
        req.setRequestId(requestId);
        log.info("request==>req:{}",req);
        //org.qts.common.entity.Message.Message rsp=client.request(req);
        //log.info("request==>rsp:{}",rsp);
        return  null;
    }


    @EventListener(MessageEvent.class)
    public void eventHandler(MessageEvent messageEvent){
        this.webSocketService.push((Message) messageEvent.getSource());
    }

    @Override
    public Message onRequest(Message msg) {
        log.info("onMessage:{}",msg);
//        AcctInst acctInst=this.acctInstMap.get(msg.getAcctId());
//        switch (msg.getType()){
//            case ON_ACCT -> {
//                //Message rsp=this.request(acctInst.getAcctId(),new Message(Enums.MSG_TYPE.QRY_ACCT,null));
//                AcctInfo acctInfo=msg.getData(AcctInfo.class);
//                //BeanUtils.copyProperties(acctInfo,acctInst.getAcctInfo());
//                acctInst.getAcctInfo().setTdStatus(acctInfo.getTdStatus());
//                acctInst.getAcctInfo().setMdStatus(acctInfo.getMdStatus());
//                SpringUtils.pushEvent(new MessageEvent(new Message.Message(Enums.MSG_TYPE.ON_ACCT,acctInst.getAcctInfo())));
//            }
//        }
        return null;
    }

    @Override
    public void onStatus(String acctId, Boolean connected) {
//        log.info("acct client[{}] status changed:{}",id,status);
//        AcctInst acctInst=this.acctInstMap.get(id);
//        if(status==true){
//            acctInst.getAcctInfo().setStatus(true);
//            //查询账户信息
//            try {
//                org.qts.common.entity.Message.Message rsp=this.request(id,Enums.MSG_TYPE.QRY_ACCT,null);
//                AcctInfo acctInfo=rsp.getData(AcctInfo.class);
//                BeanUtils.copyProperties(acctInfo,acctInst.getAcctInfo());
//                acctInst.getAcctInfo().setStatus(true);
//                acctInst.getAcctInfo().setStatusMsg("已就绪");
//
//            }catch (Exception ex){
//                log.error("qry acct error!",ex);
//            }
//
//        }else{
//            acctInst.getAcctInfo().setStatus(false);
//            acctInst.getAcctInfo().setStatusMsg("未连接");
//            acctInst.getAcctInfo().setTdStatus(false);
//            acctInst.getAcctInfo().setMdStatus(false);
//        }
//        //推送给admin
//        SpringUtils.pushEvent(new MessageEvent(new org.qts.common.entity.Message.Message(Enums.MSG_TYPE.ON_ACCT,acctInst.getAcctInfo())));
//
    }

    public void sendMsg(String acctId,Message msg){
        tcpServer.send(acctId,msg);
    }
}
