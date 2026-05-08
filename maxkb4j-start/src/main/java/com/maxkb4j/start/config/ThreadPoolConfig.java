package com.maxkb4j.start.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
public class ThreadPoolConfig {

    @Bean("taskExecutor")
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("task-");
        executor.setVirtualThreads(true);
        return executor;
    }

    @Bean(name = "chatTaskExecutor")
    public TaskExecutor chatTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("chat-");
        executor.setVirtualThreads(true);
        return executor;
    }

    @Bean("workflowExecutor")
    public TaskExecutor workflowExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("workflow-");
        executor.setVirtualThreads(true);
        return executor;
    }

}