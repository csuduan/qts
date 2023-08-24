package org.qts.trader;

import lombok.extern.slf4j.Slf4j;
import org.qts.common.utils.SpringUtils;
import org.qts.trader.core.AcctExecutor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
@ComponentScan({"org.qts.common","org.qts.trader"})
@Slf4j
@EnableScheduling
public class TraderApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(TraderApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("qts trader running...");

        AcctExecutor acctExecutor = SpringUtils.getBean(AcctExecutor.class);
        acctExecutor.start();
//        // 注册钩子函数 当程序收到"kill"信号时 执行countDown
//        CountDownLatch countDown = new CountDownLatch(1);
//        Runtime.getRuntime().addShutdownHook(new Thread(countDown::countDown));
//        countDown.await();
        log.info("qts trader stop...");
    }
}
