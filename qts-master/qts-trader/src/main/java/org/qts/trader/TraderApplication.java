package org.qts.trader;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.qts.common.entity.config.AcctConf;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

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
        //加载账户配置
        AcctConf acctConf=new AcctConf();

        //启动账户执行器
    }
}
