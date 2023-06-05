package org.mts.agent.task;

import lombok.extern.slf4j.Slf4j;
import org.mts.common.model.Enums;
import org.mts.common.model.event.MessageEvent;
import org.mts.common.model.msg.LogMsg;
import org.mts.common.model.rpc.Message;
import org.mts.common.utils.SpringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogPushTask {

    @Scheduled(cron = "0 30 15 * * *")
    public void discount(){

    }
    @Scheduled(fixedRate = 30000)
    public void logPush(){
        log.info("check...");
        LogMsg logMsg=new LogMsg();
        Message msg=new Message(Enums.MSG_TYPE.ON_LOG,logMsg);
        SpringUtils.pushEvent(new MessageEvent(msg));
    }
}
