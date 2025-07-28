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
 * KNDB1020 学生信息同步定时任务服务
 * 假设这是学生信息同步的业务模块
 */
@Service
public class KNDB1020Scheduled {
    
    private static final Logger logger = LoggerFactory.getLogger(KNDB1020Scheduled.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    @Qualifier("kndb1020Job")
    private Job kndb1020Job;

    public KNDB1020Scheduled() {
        logger.info("=== KNDB1020Scheduled 初始化成功 ===");
        logger.info("KNDB1020 定时任务已注册：每周日凌晨2:00执行学生信息同步");
        logger.info("系统时区: {}", java.time.ZoneId.systemDefault());
        logger.info("当前时间: {}", java.time.LocalDateTime.now());
    }

    /**
     * 每周日凌晨2:00执行KNDB1020批处理
     * Cron表达式：0 0 2 ? * SUN
     * 
     * 业务逻辑：
     * - 在每周日凌晨2:00执行
     * - 处理学生信息同步
     * - 例如：周日凌晨2:00执行，同步本周的学生信息变更
     */
    @Scheduled(cron = "0 0 2 ? * SUN")
    public void executeKNDB1020Job() {
        try {
            LocalDate currentDate = LocalDate.now();
            String baseDate = currentDate.format(DATE_FORMATTER);
            
            logger.info("=== KNDB1020 周日凌晨2:00 批处理开始执行 ===");
            logger.info("业务模块: 学生信息同步");
            logger.info("实际执行时间: {} 凌晨2:00", currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            logger.info("执行日期: {}", currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            logger.info("基准日期: {}", baseDate);
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("baseDate", baseDate)
                    .addString("jobMode", "SCHEDULED")
                    .addString("businessModule", "KNDB1020")
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            
            JobExecution execution = jobLauncher.run(kndb1020Job, jobParameters);
            
            String status = execution.getStatus().toString();
            if ("COMPLETED".equals(status)) {
                logger.info("=== KNDB1020 周日批处理执行成功 ===");
            } else {
                logger.error("=== KNDB1020 周日批处理执行失败，状态: {} ===", status);
            }
            
        } catch (Exception e) {
            logger.error("=== KNDB1020 定时批处理执行异常 ===", e);
        }
    }
}