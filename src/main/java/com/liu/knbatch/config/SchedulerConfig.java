package com.liu.knbatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务配置类
 * 配置动态定时任务的线程池和相关参数
 * 
 * @author liu
 * @version 1.0.0
 */
@Configuration
public class SchedulerConfig {
    
    /**
     * 配置定时任务线程池
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        
        // 线程池大小 - 根据作业数量调整
        scheduler.setPoolSize(5);
        
        // 线程名称前缀
        scheduler.setThreadNamePrefix("knpiano-dynamic-scheduler-");
        
        // 等待任务完成后再关闭线程池
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间（秒）
        scheduler.setAwaitTerminationSeconds(30);
        
        // 初始化
        scheduler.initialize();
        
        return scheduler;
    }
}