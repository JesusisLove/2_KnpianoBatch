package com.liu.knbatch.config;

import com.liu.knbatch.tasklet.KNDB1010Tasklet;
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
 * Spring Batch 配置类
 * 
 * @author Liu
 * @version 1.0.0
 */
@Configuration
public class BatchConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);
    
    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    
    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    
    @Autowired
    private KNDB1010Tasklet kndb1010Tasklet;
    
    /**
     * KNDB1010 批处理作业配置
     */
    @Bean
    public Job kndb1010Job() {
        return jobBuilderFactory.get("KNDB1010")
                .incrementer(new RunIdIncrementer())
                .listener(new KNDB1010JobExecutionListener())
                .start(kndb1010Step())
                .build();
    }
    
    /**
     * KNDB1010 步骤配置
     */
    @Bean
    public Step kndb1010Step() {
        return stepBuilderFactory.get("KNDB1010_STEP")
                .tasklet(kndb1010Tasklet)
                .build();
    }
    
    /**
     * KNDB1010 作业执行监听器
     */
    public static class KNDB1010JobExecutionListener extends JobExecutionListenerSupport {
        
        private static final Logger logger = LoggerFactory.getLogger(KNDB1010JobExecutionListener.class);
        
        @Override
        public void beforeJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            String baseDate = jobExecution.getJobParameters().getString("baseDate");
            String jobMode = jobExecution.getJobParameters().getString("jobMode");
            
            logger.info("*************************************************");
            logger.info("作业开始执行");
            logger.info("作业名称: {}", jobName);
            logger.info("执行模式: {}", jobMode);
            logger.info("基准日期: {}", baseDate);
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("开始时间: {}", jobExecution.getStartTime());
            logger.info("*************************************************");
        }
        
        @Override
        public void afterJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            String status = jobExecution.getStatus().toString();
            long duration = 0;
            
            if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
                duration = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
            }
            
            logger.info("*************************************************");
            logger.info("作业执行完成");
            logger.info("作业名称: {}", jobName);
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("执行状态: {}", status);
            logger.info("开始时间: {}", jobExecution.getStartTime());
            logger.info("结束时间: {}", jobExecution.getEndTime());
            logger.info("执行耗时: {} ms ({} 秒)", duration, duration / 1000.0);
            
            // 如果有异常，记录异常信息
            if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                logger.error("作业执行异常:");
                for (Throwable throwable : jobExecution.getAllFailureExceptions()) {
                    logger.error("  - {}: {}", throwable.getClass().getSimpleName(), throwable.getMessage());
                }
            }
            
            logger.info("*************************************************");
        }
    }
}
