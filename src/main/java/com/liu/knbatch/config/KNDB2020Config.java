package com.liu.knbatch.config;

import com.liu.knbatch.tasklet.KNDB2020Tasklet;
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
 * KNDB2020 年度月收入报告数据监视 批处理配置类
 *
 * @author Liu
 * @version 1.0.0
 */
@Configuration
public class KNDB2020Config {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private KNDB2020Tasklet kndb2020Tasklet;

    /**
     * KNDB2020 批处理作业配置
     * 年度月收入报告数据监视作业
     */
    @Bean("kndb2020Job")
    public Job kndb2020Job() {
        return jobBuilderFactory.get("KNDB2020")
                .incrementer(new RunIdIncrementer())
                .listener(new KNDB2020JobExecutionListener())
                .start(kndb2020Step())
                .build();
    }

    /**
     * KNDB2020 步骤配置
     * 年度月收入报告数据监视步骤
     */
    @Bean("kndb2020Step")
    public Step kndb2020Step() {
        return stepBuilderFactory.get("KNDB2020_STEP")
                .tasklet(kndb2020Tasklet)
                .build();
    }

    /**
     * KNDB2020 作业执行监听器
     * 监控年度月收入报告数据监视作业的执行状态
     */
    public static class KNDB2020JobExecutionListener extends JobExecutionListenerSupport {

        private static final Logger logger = LoggerFactory.getLogger(KNDB2020JobExecutionListener.class);

        @Override
        public void beforeJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            String baseDate = jobExecution.getJobParameters().getString("baseDate");
            String jobMode = jobExecution.getJobParameters().getString("jobMode");
            String businessModule = jobExecution.getJobParameters().getString("businessModule");

            logger.info("*************************************************");
            logger.info("KNDB2020 年度月收入报告数据监视作业开始执行");
            logger.info("作业名称: {}", jobName);
            logger.info("业务模块: {}", businessModule);
            logger.info("执行模式: {}", jobMode);
            logger.info("基准日期: {}", baseDate);
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("开始时间: {}", jobExecution.getStartTime());
            logger.info("业务描述: 验证年度月收入报告数据的准确性，确保应收 = 已支付 + 未支付");
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
            logger.info("KNDB2020 年度月收入报告数据监视作业执行完成");
            logger.info("作业名称: {}", jobName);
            logger.info("业务模块: {}", businessModule);
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("执行状态: {}", status);
            logger.info("开始时间: {}", jobExecution.getStartTime());
            logger.info("结束时间: {}", jobExecution.getEndTime());
            logger.info("执行耗时: {} ms ({} 秒)", duration, duration / 1000.0);

            // 根据执行状态输出不同级别的日志
            if ("COMPLETED".equals(status)) {
                logger.info("✅ 年度月收入报告数据监视处理成功");
            } else if ("FAILED".equals(status)) {
                logger.error("❌ 年度月收入报告数据监视处理失败");
            } else {
                logger.warn("⚠️ 年度月收入报告数据监视处理状态异常: {}", status);
            }

            // 如果有异常，记录异常信息
            if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                logger.error("KNDB2020 年度月收入报告数据监视作业执行异常:");
                for (Throwable throwable : jobExecution.getAllFailureExceptions()) {
                    logger.error("  - {}: {}", throwable.getClass().getSimpleName(), throwable.getMessage());
                }
            }

            logger.info("*************************************************");
        }
    }
}
