package com.cgi.bank.account.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for asynchronous task execution.
 * Enables async processing and configures a thread pool for executing async tasks.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates a custom task executor for asynchronous operations.
     * This executor is used for background tasks like notification sending.
     *
     * @return A configured ThreadPoolTaskExecutor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("NotificationTask-");
        executor.initialize();
        return executor;
    }
} 