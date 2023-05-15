package org.mts.common.executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by duanqing on  2019/11/26
 */
@Configuration
public class JobExecutorConfig {
    @Value("${job.thread.maxSize:10}")
    private int threadMaxSize;

    @Bean (name="jobExecutor")
    public ThreadPoolTaskExecutor jobExecutor(){
        ThreadPoolTaskExecutor executor=new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.min(4,threadMaxSize));
        executor.setMaxPoolSize(threadMaxSize);
        executor.setQueueCapacity(100);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        executor.setThreadGroupName("job");
        return  executor;
    }

    @Bean (name="jobExecutor2")
    public ThreadPoolTaskExecutor jobExecutor2(){
        ThreadPoolTaskExecutor executor=new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(30);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(500);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        executor.setThreadGroupName("job2");
        return  executor;
    }

    @Bean (name="jobExecutor3")
    public ThreadPoolTaskExecutor jobExecutor3(){
        ThreadPoolTaskExecutor executor=new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(150);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        executor.setThreadGroupName("job3");
        return  executor;
    }
}
