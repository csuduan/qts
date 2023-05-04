package org.mts.admin.manager;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.mts.admin.entity.Message;
import org.mts.admin.exception.BizException;
import org.mts.admin.ipc.uds.NettyClient;
import org.mts.admin.ipc.uds.UdsServer;
import org.mts.admin.ipc.uds.UdsSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;


@Component
@Slf4j
public class TradeManager {
    private final String uname="mts";

    @Autowired
    private UdsServer udsServer;

    @PostConstruct
    public void init(){
        //启动管理服务
        udsServer.start(uname);
    }
    public boolean tradeEngineOperate(String acctId, Message data ){
//        if(!udsServer.getSession()){
//            log.error("找不到账户{}的客户端",acctId);
//            throw new BizException("找不到账户");
//        }
//        if(clientMap.get(acctId).isConnected()==false){
//            log.error("找不到账户{}的客户端",acctId);
//            throw new BizException("账户未连接");
//        }
//        String message=JSON.toJSONString(data);
//        boolean result=clientMap.get(acctId).send(message);
        return true;
    }




    public void startEngine(String engineName,String path){
        try{
//            if(this.clientMap.containsKey(engineName)){
//                log.error("账户已启动");
//                return;
//            }
//            ProcessBuilder pb = new ProcessBuilder();
//            List<String> cmds = new ArrayList<String>();
//            cmds.add("java");
//            cmds.add("-Daccount="+engineName);
//            cmds.add("-jar");
//            //cmds.add("-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005");
//            cmds.add("qts-core/target/qts-core.jar");
//            pb.command(cmds);
//            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
//            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
//            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
//            //pb.directory(new File(getWorkerDir()));
//            Process process=pb.start();
//            processMap.put(accountName,process);
        }catch (Exception ex){

        }

    }
}
