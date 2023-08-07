package org.qts.trader;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"org.qts.common","org.qts.trader"})
public class TraderApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(TraderApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
