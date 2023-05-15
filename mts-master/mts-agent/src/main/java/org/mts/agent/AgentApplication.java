package org.mts.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Slf4j
@ComponentScan({"org.mts.common","org.mts.agent"})
@EnableScheduling
public class AgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
        log.info("=====mts-agent started======");

        while (true){
            try {
                Thread.sleep(1000);
            }catch (Exception ex){

            }
        }
    }

}
