package com.example.batchmonitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class AppConfig {

    @Value("${batch.scripts.baseDir}")
    private String baseDir;

    @Value("${batch.execution.maxConcurrent}")
    private int maxConcurrent;

    @Bean
    public Path scriptBaseDirectory() {
        return Paths.get(baseDir);
    }

    @Bean
    public TaskExecutor scriptExecutionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(maxConcurrent);
        executor.setMaxPoolSize(maxConcurrent);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("script-executor-");
        executor.initialize();
        return executor;
    }
}
