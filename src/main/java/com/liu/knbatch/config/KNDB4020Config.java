package com.liu.knbatch.config;

import com.liu.knbatch.tasklet.KNDB4020Tasklet;
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
 * KNDB4020 自动排课与手动排课碰撞检测 批处理配置类
 *      检测KNDB4010自动生成的课程与老师手动安排的课程之间是否存在时间冲突
 *      若发现碰撞则发送邮件报告通知老师
 * @author Liu
 * @version 1.0.0
 */
@Configuration
public class KNDB4020Config {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private KNDB4020Tasklet kndb4020Tasklet;

    /**
     * KNDB4020 批处理作业配置
     * 自动排课与手动排课碰撞检测作业
     */
    @Bean("kndb4020Job")
    public Job kndb4020Job() {
        return jobBuilderFactory.get("KNDB4020")
                .incrementer(new RunIdIncrementer())
                .listener(new KNDB4020JobExecutionListener())
                .start(kndb4020Step())
                .build();
    }

    /**
     * KNDB4020 步骤配置
     * 碰撞检测步骤
     */
    @Bean("kndb4020Step")
    public Step kndb4020Step() {
        return stepBuilderFactory.get("KNDB4020_STEP")
                .tasklet(kndb4020Tasklet)
                .build();
    }

    /**
     * KNDB4020 作业执行监听器
     * 监控碰撞检测作业的执行状态
     */
    public static class KNDB4020JobExecutionListener extends JobExecutionListenerSupport {

        private static final Logger logger = LoggerFactory.getLogger(KNDB4020JobExecutionListener.class);

        @Override
        public void beforeJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            String baseDate = jobExecution.getJobParameters().getString("baseDate");
            String jobMode = jobExecution.getJobParameters().getString("jobMode");
            String businessModule = jobExecution.getJobParameters().getString("businessModule");

            logger.info("*************************************************");
            logger.info("KNDB4020 自动排课与手动排课碰撞检测作业开始执行");
            logger.info("作业名称: {}", jobName);
            logger.info("业务模块: {}", businessModule);
            logger.info("执行模式: {}", jobMode);
            logger.info("基准日期: {}", baseDate);
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("开始时间: {}", jobExecution.getStartTime());
            logger.info("业务描述: 检测自动排课与手动排课之间的时间冲突，发现碰撞则发送邮件通知");
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
            logger.info("KNDB4020 碰撞检测作业执行完成");
            logger.info("作业名称: {}", jobName);
            logger.info("业务模块: {}", businessModule);
            logger.info("作业ID: {}", jobExecution.getId());
            logger.info("执行状态: {}", status);
            logger.info("开始时间: {}", jobExecution.getStartTime());
            logger.info("结束时间: {}", jobExecution.getEndTime());
            logger.info("执行耗时: {} ms ({} 秒)", duration, duration / 1000.0);

            // 根据执行状态输出不同级别的日志
            if ("COMPLETED".equals(status)) {
                logger.info("✅ 碰撞检测处理成功");
            } else if ("FAILED".equals(status)) {
                logger.error("❌ 碰撞检测处理失败");
            } else {
                logger.warn("⚠️ 碰撞检测处理状态异常: {}", status);
            }

            // 如果有异常，记录异常信息
            if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                logger.error("KNDB4020 碰撞检测作业执行异常:");
                for (Throwable throwable : jobExecution.getAllFailureExceptions()) {
                    logger.error("  - {}: {}", throwable.getClass().getSimpleName(), throwable.getMessage());
                }
            }

            logger.info("*************************************************");
        }
    }
}
