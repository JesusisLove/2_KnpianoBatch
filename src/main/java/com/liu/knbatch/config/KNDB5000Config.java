package com.liu.knbatch.config;

import com.liu.knbatch.tasklet.KNDB5000Tasklet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * KNDB5000 数据库备份 批处理配置
 * 
 * @author Liu
 * @version 1.0.0
 */
@Configuration
public class KNDB5000Config {

    // private static final Logger logger = LoggerFactory.getLogger(KNDB5000Config.class);

    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    
    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private KNDB5000Tasklet kndb5000Tasklet;

    /**
     * KNDB5000数据库备份作业
     */
    @Bean("kndb5000Job")
    public Job kndb5000Job() {
        return jobBuilderFactory.get("KNDB5000")
                .incrementer(new RunIdIncrementer())
                .listener(new KNDB5000JobExecutionListener())
                .start(kndb5000Step())
                .build();
    }

    /**
     * KNDB5000数据库备份步骤
     */
    @Bean("kndb5000Step")
    public Step kndb5000Step() {
        return stepBuilderFactory.get("KNDB5000_STEP")
                .tasklet(kndb5000Tasklet)
                .build();
    }

    /**
     * KNDB5000作业执行监听器
     */
    public static class KNDB5000JobExecutionListener implements JobExecutionListener {

        private static final Logger logger = LoggerFactory.getLogger(KNDB5000JobExecutionListener.class);

        @Override
        public void beforeJob(JobExecution jobExecution) {
            JobParameters jobParameters = jobExecution.getJobParameters();
            
            logger.info("*************************************************");
            logger.info("KNDB5000 数据库备份作业开始执行");
            logger.info("作业名称: {}", jobExecution.getJobInstance().getJobName());
            logger.info("业务模块: {}", jobParameters.getString("businessModule"));
            logger.info("执行模式: {}", jobParameters.getString("jobMode"));
            logger.info("基准日期: {}", jobParameters.getString("baseDate"));
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("开始时间: {}", jobExecution.getStartTime());
            logger.info("业务描述: 执行KNStudent数据库定期备份，确保数据安全");
            logger.info("*************************************************");
        }

        @Override
        public void afterJob(JobExecution jobExecution) {
            JobParameters jobParameters = jobExecution.getJobParameters();
            BatchStatus status = jobExecution.getStatus();
            
            logger.info("*************************************************");
            logger.info("KNDB5000 数据库备份作业执行完成");
            logger.info("作业名称: {}", jobExecution.getJobInstance().getJobName());
            logger.info("业务模块: {}", jobParameters.getString("businessModule"));
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("执行状态: {}", status);
            logger.info("开始时间: {}", jobExecution.getStartTime());
            logger.info("结束时间: {}", jobExecution.getEndTime());
            
            if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
                long executionTime = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
                logger.info("执行耗时: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
            }
            
            if (status == BatchStatus.COMPLETED) {
                logger.info("✅ 数据库备份处理成功");
            } else {
                logger.info("❌ 数据库备份处理失败");
                // 记录失败原因
                jobExecution.getAllFailureExceptions().forEach(throwable -> 
                    logger.error("失败原因: {}", throwable.getMessage(), throwable)
                );
            }
            logger.info("*************************************************");
        }
    }
}