package org.mts.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Slf4j
@ComponentScan({"org.mts.common","org.mts.agent"})
@EnableScheduling
public class TradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeApplication.class, args);
        log.info("=====mts-trade started======");

        while (true){
            try {
                Thread.sleep(1000);
            }catch (Exception ex){

            }
        }
    }

}
