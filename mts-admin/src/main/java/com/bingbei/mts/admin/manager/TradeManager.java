package com.bingbei.mts.admin.manager;

import com.alibaba.fastjson.JSON;
import com.bingbei.mts.admin.entity.Message;
import com.bingbei.mts.common.exception.BizException;
import com.bingbei.mts.common.ipc.uds.NettyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;


@Component
@Slf4j
public class TradeManager {
    private Map<String, NettyClient> clientMap=new HashMap<>();

    @PostConstruct
    public void init(){
        this.createClient("sim");
        this.createClient("ost");
    }
    public void createClient(String acctId){
        NettyClient client=new NettyClient(acctId);
        clientMap.put(acctId,client);
    }

    public boolean tradeEngineOperate(String acctId, Message data ){
        if(!clientMap.containsKey(acctId)){
            log.error("找不到账户{}的客户端",acctId);
            throw new BizException("找不到账户");
        }
        if(clientMap.get(acctId).isConnected()==false){
            log.error("找不到账户{}的客户端",acctId);
            throw new BizException("账户未连接");
        }
        String message=JSON.toJSONString(data);
        boolean result=clientMap.get(acctId).send(message);
        return result;
    }




    public void startEngine(String engineName,String path){
        try{
            if(this.clientMap.containsKey(engineName)){
                log.error("账户已启动");
                return;
            }
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
