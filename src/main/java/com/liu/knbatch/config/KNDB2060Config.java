package com.liu.knbatch.config;

import com.liu.knbatch.tasklet.KNDB2060Tasklet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * KNDB2060 空月课费自动生成 批处理配置类
 *
 * @author Liu
 * @version 1.0.0
 */
@Configuration
public class KNDB2060Config {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private KNDB2060Tasklet kndb2060Tasklet;

    /**
     * KNDB2060 批处理作业配置
     */
    @Bean("kndb2060Job")
    public Job kndb2060Job() {
        return jobBuilderFactory.get("KNDB2060")
                .incrementer(new RunIdIncrementer())
                .listener(new KNDB2060JobExecutionListener())
                .start(kndb2060Step())
                .build();
    }

    /**
     * KNDB2060 步骤配置
     */
    @Bean("kndb2060Step")
    public Step kndb2060Step() {
        return stepBuilderFactory.get("KNDB2060_STEP")
                .tasklet(kndb2060Tasklet)
                .build();
    }

    /**
     * KNDB2060 作业执行监听器
     */
    public static class KNDB2060JobExecutionListener extends JobExecutionListenerSupport {

        private static final Logger logger = LoggerFactory.getLogger(KNDB2060JobExecutionListener.class);

        @Override
        public void beforeJob(JobExecution jobExecution) {
            logger.info("*************************************************");
            logger.info("KNDB2060 空月课费自动生成作业开始执行");
            logger.info("作业名称: {}", jobExecution.getJobInstance().getJobName());
            logger.info("执行模式: {}", jobExecution.getJobParameters().getString("jobMode"));
            logger.info("基准日期: {}", jobExecution.getJobParameters().getString("baseDate"));
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("*************************************************");
        }

        @Override
        public void afterJob(JobExecution jobExecution) {
            String status = jobExecution.getStatus().toString();
            long duration = 0;

            if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
                duration = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
            }

            logger.info("*************************************************");
            logger.info("KNDB2060 空月课费自动生成作业执行完成");
            logger.info("执行状态: {}", status);
            logger.info("执行耗时: {} ms ({} 秒)", duration, duration / 1000.0);

            if ("COMPLETED".equals(status)) {
                logger.info("✅ 空月课费自动生成处理成功");
            } else if ("FAILED".equals(status)) {
                logger.error("❌ 空月课费自动生成处理失败");
            } else {
                logger.warn("⚠️ 空月课费自动生成处理状态异常: {}", status);
            }

            logger.info("*************************************************");
        }
    }
}
