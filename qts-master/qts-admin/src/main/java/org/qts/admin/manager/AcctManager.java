package org.qts.admin.manager;

import lombok.extern.slf4j.Slf4j;
import org.qts.admin.core.AcctInstance;
import org.qts.admin.dao.ConfMapper;
import org.qts.admin.service.WebSocketService;
import org.qts.common.entity.Message;
import org.qts.common.entity.acct.AcctInfo;
import org.qts.common.entity.config.AcctConf;
import org.qts.common.entity.event.MessageEvent;
import org.qts.common.rpc.tcp.server.ServerListener;
import org.qts.common.rpc.tcp.server.TcpServer;
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
 * 交易代理服务
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

    public boolean startAcctInstance(AcctConf acctConf){
        if(!"Y".equals(acctConf.getEnable()))
            return false;
        if(this.acctInstanceMap.containsKey(acctConf.getId()))
            return false;

        AcctInstance acctInstance=new AcctInstance(acctConf);
        this.acctInstanceMap.put(acctConf.getId(),acctInstance);
        //检查账户进程是否存在
        //启动账户实例
        try {
            ProcessBuilder pb = new ProcessBuilder();
            List<String> cmds = new ArrayList<String>();
            cmds.add("java");
            cmds.add("-Dacct="+acctConf.getId());
            cmds.add("-jar");
            //cmds.add("-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005");
            cmds.add("qts-core/target/qts-core.jar");
            pb.command(cmds);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            //pb.directory(new File(getWorkerDir()));
            Process process=pb.start();
            acctInstance.setProcess(process);
        }catch (Exception ex){

        }


        return true;
    }


    public List<AcctInfo> getAcctInfos(){
        return tradeService.getAcctInfos();
    }



    public Boolean acctOperate(String acctId,Enums.MSG_TYPE type,String data){
        org.qts.common.entity.Message.Message rsp=tradeService.request(acctId,type,data);
        return rsp.getSuccess();
    }

    private String getServerByAcct(String acctId){
        if(this.acctInfoMap.containsKey(acctId)){
            String serverId=this.acctInfoMap.get(acctId).getOwner();
            return serverId;
        }
        return null;
    }

    @EventListener(MessageEvent.class)
    public void eventHandler(MessageEvent messageEvent){
        this.webSocketService.push((Message.Message) messageEvent.getSource());
    }

    @Override
    public Message onRequest(Message req) {
        return null;
    }

    @Override
    public void onConnect(String acctId, Boolean connected) {

    }


    public void sendMsg(String acctId,Message msg){
        tcpServer.send(acctId,msg);
    }
}
