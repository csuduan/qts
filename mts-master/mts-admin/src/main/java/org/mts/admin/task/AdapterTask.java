package org.mts.admin.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdapterTask {

    @Scheduled(cron = "0 30 15 * * *")
    public void discount(){

    }
    @Scheduled(fixedRate = 60000)
    public void check(){
        System.out.println("check...");
    }
}
