package com.bingbei.mts.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.bingbei.mts")
public class MtsAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtsAdminApplication.class, args);
    }

}
