package com.maxkb4j.start.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class ThreadPoolConfig {

    /**
     * 通用任务线程池
     */
    @Bean("taskScheduler")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("trigger-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setVirtualThreads(true);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * 通用任务线程池
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        return createExecutor("task-", 8, 32, 256);
    }

    /**
     * 聊天任务线程池（通常 IO 密集型，可适当调大核心/最大线程数）
     */
    @Bean("chatTaskExecutor")
    public Executor chatTaskExecutor() {
        return createExecutor("chat-", 16, 64, 512);
    }

    /**
     * 工作流任务线程池
     */
    @Bean("workflowTaskExecutor")
    public Executor workflowTaskExecutor() {
        return createExecutor("workflow-", 8, 32, 256);
    }

    /**
     * 统一创建有界线程池的方法
     * @param threadNamePrefix 线程名前缀
     * @param corePoolSize     核心线程数
     * @param maxPoolSize      最大线程数
     * @param queueCapacity    队列容量（有界）
     */
    private ThreadPoolTaskExecutor createExecutor(String threadNamePrefix,
                                                  int corePoolSize,
                                                  int maxPoolSize,
                                                  int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setVirtualThreads(true);

        // 拒绝策略：当线程池和队列都满时，由调用者线程执行，起到背压/限流作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 允许核心线程超时回收（可选，视业务是否需要长时间保持空闲线程而定）
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}