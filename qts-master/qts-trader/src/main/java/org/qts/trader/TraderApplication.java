package org.qts.trader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
@ComponentScan({"org.qts.common","org.qts.trader"})
@Slf4j
public class TraderApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(TraderApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("qts trader running...");
        // 注册钩子函数 当程序收到"kill"信号时 执行countDown
        CountDownLatch countDown = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(countDown::countDown));
        countDown.await();
        log.info("qts trader stop...");
    }
}
