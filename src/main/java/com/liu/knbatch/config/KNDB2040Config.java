package com.liu.knbatch.config;

import com.liu.knbatch.tasklet.KNDB2040Tasklet;
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
 * KNDB2040 学生指定月课费金额修正 批处理配置类
 *
 * @author Liu
 * @version 1.0.0
 */
@Configuration
public class KNDB2040Config {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private KNDB2040Tasklet kndb2040Tasklet;

    /**
     * KNDB2040 批处理作业配置
     * 学生指定月课费金额修正作业
     */
    @Bean("kndb2040Job")
    public Job kndb2040Job() {
        return jobBuilderFactory.get("KNDB2040")
                .incrementer(new RunIdIncrementer())
                .listener(new KNDB2040JobExecutionListener())
                .start(kndb2040Step())
                .build();
    }

    /**
     * KNDB2040 步骤配置
     */
    @Bean("kndb2040Step")
    public Step kndb2040Step() {
        return stepBuilderFactory.get("KNDB2040_STEP")
                .tasklet(kndb2040Tasklet)
                .build();
    }

    /**
     * KNDB2040 作业执行监听器
     */
    public static class KNDB2040JobExecutionListener extends JobExecutionListenerSupport {

        private static final Logger logger = LoggerFactory.getLogger(KNDB2040JobExecutionListener.class);

        @Override
        public void beforeJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            String stuId = jobExecution.getJobParameters().getString("stuId");
            String subjectId = jobExecution.getJobParameters().getString("subjectId");
            String newSubjectSubId = jobExecution.getJobParameters().getString("newSubjectSubId");
            String startYearMonth = jobExecution.getJobParameters().getString("startYearMonth");
            String businessModule = jobExecution.getJobParameters().getString("businessModule");

            logger.info("*************************************************");
            logger.info("KNDB2040 学生指定月课费金额修正作业开始执行");
            logger.info("作业名称: {}", jobName);
            logger.info("业务模块: {}", businessModule);
            logger.info("学生ID: {}", stuId);
            logger.info("科目ID: {}", subjectId);
            logger.info("新子科目: {}", newSubjectSubId);
            logger.info("起价年月: {}", startYearMonth);
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
            logger.info("KNDB2040 学生指定月课费金额修正作业执行完成");
            logger.info("作业名称: {}", jobName);
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("执行状态: {}", status);
            logger.info("开始时间: {}", jobExecution.getStartTime());
            logger.info("结束时间: {}", jobExecution.getEndTime());
            logger.info("执行耗时: {} ms ({} 秒)", duration, duration / 1000.0);

            if ("COMPLETED".equals(status)) {
                logger.info("✅ 课费金额修正处理成功");
            } else if ("FAILED".equals(status)) {
                logger.error("❌ 课费金额修正处理失败");
            } else {
                logger.warn("⚠️ 课费金额修正处理状态异常: {}", status);
            }

            if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                logger.error("KNDB2040 作业执行异常:");
                for (Throwable throwable : jobExecution.getAllFailureExceptions()) {
                    logger.error("  - {}: {}", throwable.getClass().getSimpleName(), throwable.getMessage());
                }
            }

            logger.info("*************************************************");
        }
    }
}
