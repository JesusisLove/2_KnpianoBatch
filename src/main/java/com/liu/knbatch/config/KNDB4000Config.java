package com.liu.knbatch.config;

import com.liu.knbatch.tasklet.KNDB4000Tasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KNDB4000 年度周次表生成 批处理配置类
 *      观妮通常在每周日的晚上对下一周的课程进行一周排课，此Batch处理就是替代她执行一周排课
 * @author Liu
 * @version 1.0.0
 */
@Configuration
public class KNDB4000Config {
    
    private static final Logger logger = LoggerFactory.getLogger(KNDB4000Config.class);
    
    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    
    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    
    @Autowired
    private KNDB4000Tasklet kndb4000Tasklet;
    
    /**
     * KNDB4000 批处理作业配置
     * 年度周次表生成作业
     */
    @Bean("kndb4000Job")
    public Job kndb4000Job() {
        return jobBuilderFactory.get("KNDB4000")
                .incrementer(new RunIdIncrementer())
                .listener(new KNDB4000JobExecutionListener())
                .start(kndb4000Step())
                .build();
    }
    
    /**
     * KNDB4000 步骤配置
     * 年度周次表生成步骤
     */
    @Bean("kndb4000Step")
    public Step kndb4000Step() {
        return stepBuilderFactory.get("KNDB4000_STEP")
                .tasklet(kndb4000Tasklet)
                .build();
    }
    
    /**
     * KNDB4000 作业执行监听器
     * 监控年度周次表生成作业的执行状态
     */
    public static class KNDB4000JobExecutionListener extends JobExecutionListenerSupport {
        
        private static final Logger logger = LoggerFactory.getLogger(KNDB4000JobExecutionListener.class);
        
        @Override
        public void beforeJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            String baseDate = jobExecution.getJobParameters().getString("baseDate");
            String jobMode = jobExecution.getJobParameters().getString("jobMode");
            String businessModule = jobExecution.getJobParameters().getString("businessModule");
            
            logger.info("*************************************************");
            logger.info("KNDB4000 年度周次表生成作业开始执行");
            logger.info("作业名称: {}", jobName);
            logger.info("业务模块: {}", businessModule);
            logger.info("执行模式: {}", jobMode);
            logger.info("基准日期: {}", baseDate);
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("开始时间: {}", jobExecution.getStartTime());
            logger.info("业务描述: 处理年度周次表生成，确保学生课程级别与实际进度匹配");
            logger.info("*************************************************");
        }
        
        @Override
        public void afterJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            String status = jobExecution.getStatus().toString();
            String businessModule = jobExecution.getJobParameters().getString("businessModule");
            long duration = 0;
            
            if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
                duration = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
            }
            
            logger.info("*************************************************");
            logger.info("KNDB4000 年度周次表生成作业执行完成");
            logger.info("作业名称: {}", jobName);
            logger.info("业务模块: {}", businessModule);
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("执行状态: {}", status);
            logger.info("开始时间: {}", jobExecution.getStartTime());
            logger.info("结束时间: {}", jobExecution.getEndTime());
            logger.info("执行耗时: {} ms ({} 秒)", duration, duration / 1000.0);
            
            // 根据执行状态输出不同级别的日志
            if ("COMPLETED".equals(status)) {
                logger.info("✅ 年度周次表生成处理成功");
            } else if ("FAILED".equals(status)) {
                logger.error("❌ 年度周次表生成处理失败");
            } else {
                logger.warn("⚠️ 年度周次表生成处理状态异常: {}", status);
            }
            
            // 如果有异常，记录异常信息
            if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                logger.error("KNDB4000 年度周次表生成作业执行异常:");
                for (Throwable throwable : jobExecution.getAllFailureExceptions()) {
                    logger.error("  - {}: {}", throwable.getClass().getSimpleName(), throwable.getMessage());
                }
            }
            
            logger.info("*************************************************");
        }
    }
}