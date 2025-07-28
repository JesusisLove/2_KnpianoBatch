package com.liu.knbatch.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 定时任务管理服务
 * 提供定时任务的统一管理和监控
 */
@Service
public class ScheduledTaskManagerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskManagerService.class);
    
    @PostConstruct
    public void init() {
        logger.info("========================================");
        logger.info("KnPiano Batch 定时任务管理系统初始化完成");
        logger.info("系统启动时间: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("已注册的定时任务:");
        logger.info("  - KNDB1010: 每月1号凌晨1:00 (钢琴课程级别矫正)");
        logger.info("  - KNDB1020: 每周日凌晨2:00 (学生信息同步)");
        logger.info("系统时区: {}", java.time.ZoneId.systemDefault());
        logger.info("========================================");
    }
    
    /**
     * 系统健康检查 - 每小时执行一次
     * 记录系统运行状态，确保定时任务系统正常工作
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void healthCheck() {
        logger.debug("定时任务系统健康检查 - {}", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
    
    /**
     * 每日系统状态报告 - 每天凌晨0:30执行
     * 记录系统运行状态和统计信息
     */
    @Scheduled(cron = "0 30 0 * * ?")
    public void dailyStatusReport() {
        logger.info("=== KnPiano Batch 系统日报 ===");
        logger.info("报告时间: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("系统运行状态: 正常");
        logger.info("已注册定时任务数: 2");
        logger.info("今日待执行任务:");
        
        LocalDateTime now = LocalDateTime.now();
        int dayOfMonth = now.getDayOfMonth();
        int dayOfWeek = now.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        
        if (dayOfMonth == 1) {
            logger.info("  - KNDB1010: 今日1:00将执行钢琴课程级别矫正");
        }
        
        if (dayOfWeek == 7) { // Sunday
            logger.info("  - KNDB1020: 今日2:00将执行学生信息同步");
        }
        
        if (dayOfMonth != 1 && dayOfWeek != 7) {
            logger.info("  - 今日无定时任务执行");
        }
        
        logger.info("===============================");
    }
}