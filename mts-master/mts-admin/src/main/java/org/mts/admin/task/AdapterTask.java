package org.mts.admin.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdapterTask {

    @Scheduled(cron = "0 30 15 * * *")
    public void discount(){

    }
    @Scheduled(fixedRate = 300000)
    public void check(){
        log.info("check...");
    }
}
