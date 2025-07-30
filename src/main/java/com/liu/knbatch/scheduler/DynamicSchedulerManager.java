package com.liu.knbatch.scheduler;

import com.liu.knbatch.config.BatchJobRegistry;
import com.liu.knbatch.config.BatchJobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 动态定时任务管理器
 * 基于XML配置自动注册和管理所有定时任务
 * 实现完全动态的定时任务系统
 * 
 * @author liu
 * @version 1.0.0
 */
@Component
public class DynamicSchedulerManager implements SchedulingConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicSchedulerManager.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    @Autowired
    private BatchJobRegistry jobRegistry;
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @PostConstruct
    public void init() {
        logger.info("========================================");
        logger.info("动态定时任务管理器初始化完成");
        logger.info("基于XML配置自动注册定时任务");
        logger.info("系统启动时间: {}", java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("系统时区: {}", java.time.ZoneId.systemDefault());
        logger.info("========================================");
    }
    
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 获取所有有定时表达式的作业
        List<BatchJobInfo> scheduledJobs = jobRegistry.getScheduledJobs();
        
        logger.info("开始注册动态定时任务...");
        logger.info("发现 {} 个需要定时执行的作业", scheduledJobs.size());
        
        for (BatchJobInfo jobInfo : scheduledJobs) {
            if (jobInfo.getCronExpression() != null && !jobInfo.getCronExpression().trim().isEmpty()) {
                registerScheduledTask(taskRegistrar, jobInfo);
            }
        }
        
        logger.info("动态定时任务注册完成");
        logger.info("========================================");
    }
    
    /**
     * 注册单个定时任务
     */
    private void registerScheduledTask(ScheduledTaskRegistrar taskRegistrar, BatchJobInfo jobInfo) {
        try {
            CronTrigger cronTrigger = new CronTrigger(jobInfo.getCronExpression());
            
            // 创建定时任务
            Runnable task = () -> executeBatchJob(jobInfo);
            
            // 注册定时任务
            taskRegistrar.addTriggerTask(task, cronTrigger);
            
            String cronDesc = parseCronDescription(jobInfo.getCronExpression());
            logger.info("已注册定时任务: {} - {} ({})", 
                    jobInfo.getJobId(), jobInfo.getDescription(), cronDesc);
            
        } catch (Exception e) {
            logger.error("注册定时任务失败: {} - {}", jobInfo.getJobId(), e.getMessage(), e);
        }
    }
    
    /**
     * 执行批处理作业
     */
    private void executeBatchJob(BatchJobInfo jobInfo) {
        String jobId = jobInfo.getJobId();
        String description = jobInfo.getDescription();
        
        try {
            LocalDate currentDate = LocalDate.now();
            String baseDate = currentDate.format(DATE_FORMATTER);
            String targetMonthStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            logger.info("=== {} 定时批处理开始执行 ===", jobId);
            logger.info("业务模块: {}", description);
            logger.info("执行时间: {}", java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            logger.info("处理目标: {}", getProcessingTarget(jobId, targetMonthStr));
            logger.info("基准日期: {}", baseDate);
            
            // 动态获取Job实例
            Job job = applicationContext.getBean(jobInfo.getBeanName(), Job.class);
            
            // 构建作业参数
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("baseDate", baseDate)
                    .addString("jobMode", "SCHEDULED")
                    .addString("businessModule", jobId)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            
            // 执行作业
            JobExecution execution = jobLauncher.run(job, jobParameters);
            
            // 检查执行结果
            String status = execution.getStatus().toString();
            if ("COMPLETED".equals(status)) {
                logger.info("=== {} 定时批处理执行成功 ===", jobId);
            } else {
                logger.error("=== {} 定时批处理执行失败，状态: {} ===", jobId, status);
            }
            
        } catch (Exception e) {
            logger.error("=== {} 定时批处理执行异常 ===", jobId, e);
        }
    }
    
    /**
     * 根据作业ID获取处理目标描述
     */
    private String getProcessingTarget(String jobId, String targetMonth) {
        switch (jobId) {
            case "KNDB1010":
                return targetMonth + " 月份的钢琴课程级别矫正";
            case "KNDB1020":
                return "学生信息同步";
            case "KNDB1030":
                return targetMonth + " 月份的教师排课管理";
            case "KNDB1040":
                return targetMonth + " 月份的学费结算处理";
            case "KNDB1050":
                return targetMonth + " 月份的报表生成";
            default:
                return jobId + " 业务处理";
        }
    }
    
    /**
     * 解析Cron表达式为人类可读的描述
     */
    private String parseCronDescription(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return "未设置定时";
        }
        
        // 常见的Cron表达式解析
        switch (cronExpression.trim()) {
            case "0 0 1 1 * ?":
                return "每月1号凌晨1:00";
            case "0 0 2 ? * SUN":
                return "每周日凌晨2:00";
            case "0 0 3 ? * MON":
                return "每周一凌晨3:00";
            case "0 0 4 L * ?":
                return "每月最后一天凌晨4:00";
            case "0 0 5 1 * ?":
                return "每月1号凌晨5:00";
            case "0 */5 * * * ?":
                return "每5分钟执行一次";
            default:
                return cronExpression;
        }
    }
}