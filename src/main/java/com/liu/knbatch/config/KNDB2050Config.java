package com.liu.knbatch.config;

import com.liu.knbatch.tasklet.KNDB2050Tasklet;
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
 * KNDB2050 计划课转换成加课 批处理配置类
 *
 * @author Liu
 * @version 1.0.0
 */
@Configuration
public class KNDB2050Config {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private KNDB2050Tasklet kndb2050Tasklet;

    /**
     * KNDB2050 批处理作业配置
     */
    @Bean("kndb2050Job")
    public Job kndb2050Job() {
        return jobBuilderFactory.get("KNDB2050")
                .incrementer(new RunIdIncrementer())
                .listener(new KNDB2050JobExecutionListener())
                .start(kndb2050Step())
                .build();
    }

    /**
     * KNDB2050 步骤配置
     */
    @Bean("kndb2050Step")
    public Step kndb2050Step() {
        return stepBuilderFactory.get("KNDB2050_STEP")
                .tasklet(kndb2050Tasklet)
                .build();
    }

    /**
     * KNDB2050 作业执行监听器
     */
    public static class KNDB2050JobExecutionListener extends JobExecutionListenerSupport {

        private static final Logger logger = LoggerFactory.getLogger(KNDB2050JobExecutionListener.class);

        @Override
        public void beforeJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            String stuId = jobExecution.getJobParameters().getString("stuId");
            String subjectId = jobExecution.getJobParameters().getString("subjectId");
            String baseDate = jobExecution.getJobParameters().getString("baseDate");
            String businessModule = jobExecution.getJobParameters().getString("businessModule");

            logger.info("*************************************************");
            logger.info("KNDB2050 计划课转换成加课作业开始执行");
            logger.info("作业名称: {}", jobName);
            logger.info("业务模块: {}", businessModule);
            logger.info("学生ID: {}", stuId);
            logger.info("科目ID: {}", subjectId);
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
            logger.info("KNDB2050 计划课转换成加课作业执行完成");
            logger.info("作业名称: {}", jobName);
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("执行状态: {}", status);
            logger.info("开始时间: {}", jobExecution.getStartTime());
            logger.info("结束时间: {}", jobExecution.getEndTime());
            logger.info("执行耗时: {} ms ({} 秒)", duration, duration / 1000.0);

            if ("COMPLETED".equals(status)) {
                logger.info("✅ 计划课转换成加课处理成功");
            } else if ("FAILED".equals(status)) {
                logger.error("❌ 计划课转换成加课处理失败");
            } else {
                logger.warn("⚠️ 计划课转换成加课处理状态异常: {}", status);
            }

            if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                logger.error("KNDB2050 作业执行异常:");
                for (Throwable throwable : jobExecution.getAllFailureExceptions()) {
                    logger.error("  - {}: {}", throwable.getClass().getSimpleName(), throwable.getMessage());
                }
            }

            logger.info("*************************************************");
        }
    }
}
