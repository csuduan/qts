package org.qts.admin.task;

import lombok.extern.slf4j.Slf4j;
import org.qts.common.entity.Enums;
import org.qts.common.entity.Message;
import org.qts.common.entity.event.MessageEvent;
import org.qts.common.entity.msg.LogMsg;
import org.qts.common.utils.SpringUtils;
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
