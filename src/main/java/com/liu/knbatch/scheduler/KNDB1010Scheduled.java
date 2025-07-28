package com.liu.knbatch.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * KNDB1010 钢琴课程级别矫正定时任务服务
 */
@Service
public class KNDB1010Scheduled {
    
    private static final Logger logger = LoggerFactory.getLogger(KNDB1010Scheduled.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    @Qualifier("kndb1010Job")
    private Job kndb1010Job;

    public KNDB1010Scheduled() {
        logger.info("=== KNDB1010Scheduled 初始化成功 ===");
        logger.info("KNDB1010 定时任务已注册：每月1号凌晨1:00执行钢琴课程级别矫正");
        logger.info("系统时区: {}", java.time.ZoneId.systemDefault());
        logger.info("当前时间: {}", java.time.LocalDateTime.now());
    }

    /**
     * 每月1号凌晨1:00执行KNDB1010批处理
     * Cron表达式：0 0 1 1 * ?
     * 
     * 业务逻辑：
     * - 在每月1号凌晨1:00执行
     * - 处理当月的钢琴课程级别矫正
     * - 例如：8月1日凌晨1:00执行，处理8月份的课程数据
     */
    // @Scheduled(cron = "0 0 1 1 * ?")
      @Scheduled(cron = "0 15 1 29 7 ?")
    public void executeKNDB1010Job() {
        try {
            LocalDate currentDate = LocalDate.now();
            String baseDate = currentDate.format(DATE_FORMATTER);
            String targetMonthStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            logger.info("=== KNDB1010 {} 月1号凌晨1:00 批处理开始执行 ===", targetMonthStr);
            logger.info("业务模块: 钢琴课程级别矫正");
            logger.info("实际执行时间: {} 凌晨1:00", currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            logger.info("处理目标月份: {}", targetMonthStr);
            logger.info("基准日期: {}", baseDate);
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("baseDate", baseDate)
                    .addString("jobMode", "SCHEDULED")
                    .addString("businessModule", "KNDB1010")
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            
            JobExecution execution = jobLauncher.run(kndb1010Job, jobParameters);
            
            String status = execution.getStatus().toString();
            if ("COMPLETED".equals(status)) {
                logger.info("=== KNDB1010 {} 月1号批处理执行成功，已处理 {} 月份数据 ===", 
                    targetMonthStr, targetMonthStr);
            } else {
                logger.error("=== KNDB1010 {} 月1号批处理执行失败，状态: {} ===", 
                    targetMonthStr, status);
            }
            
        } catch (Exception e) {
            logger.error("=== KNDB1010 定时批处理执行异常 ===", e);
        }
    }
}