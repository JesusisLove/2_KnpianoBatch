package com.liu.knbatch.config;

import com.liu.knbatch.tasklet.KNDB4030Tasklet;
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
 * KNDB4030 零碎課補整節課郵件提醒 批処理配置類
 *      毎日碎片加課（lesson_type=2）を検出し、標準課時に達する組み合わせがあれば
 *      ピアノ教師にメール通知を送信する
 * @author Liu
 * @version 1.0.0
 */
@Configuration
public class KNDB4030Config {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private KNDB4030Tasklet kndb4030Tasklet;

    /**
     * KNDB4030 批処理作業配置
     * 零碎課補整節課郵件提醒作業
     */
    @Bean("kndb4030Job")
    public Job kndb4030Job() {
        return jobBuilderFactory.get("KNDB4030")
                .incrementer(new RunIdIncrementer())
                .listener(new KNDB4030JobExecutionListener())
                .start(kndb4030Step())
                .build();
    }

    /**
     * KNDB4030 步驟配置
     * 零碎課検出步驟
     */
    @Bean("kndb4030Step")
    public Step kndb4030Step() {
        return stepBuilderFactory.get("KNDB4030_STEP")
                .tasklet(kndb4030Tasklet)
                .build();
    }

    /**
     * KNDB4030 作業執行監聽器
     * 監控零碎課検出作業的執行狀態
     */
    public static class KNDB4030JobExecutionListener extends JobExecutionListenerSupport {

        private static final Logger logger = LoggerFactory.getLogger(KNDB4030JobExecutionListener.class);

        @Override
        public void beforeJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            String baseDate = jobExecution.getJobParameters().getString("baseDate");
            String jobMode = jobExecution.getJobParameters().getString("jobMode");
            String businessModule = jobExecution.getJobParameters().getString("businessModule");

            logger.info("*************************************************");
            logger.info("KNDB4030 零碎課補整節課郵件提醒作業開始執行");
            logger.info("作業名稱: {}", jobName);
            logger.info("業務模塊: {}", businessModule);
            logger.info("執行模式: {}", jobMode);
            logger.info("基準日期: {}", baseDate);
            logger.info("作業ID: {}", jobExecution.getId());
            logger.info("開始時間: {}", jobExecution.getStartTime());
            logger.info("業務描述: 検出碎片加課是否可湊成整課並郵件提醒");
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
            logger.info("KNDB4030 零碎課提醒作業執行完成");
            logger.info("作業名稱: {}", jobName);
            logger.info("業務模塊: {}", businessModule);
            logger.info("作業ID: {}", jobExecution.getId());
            logger.info("執行狀態: {}", status);
            logger.info("開始時間: {}", jobExecution.getStartTime());
            logger.info("結束時間: {}", jobExecution.getEndTime());
            logger.info("執行耗時: {} ms ({} 秒)", duration, duration / 1000.0);

            if ("COMPLETED".equals(status)) {
                logger.info("零碎課検出処理成功");
            } else if ("FAILED".equals(status)) {
                logger.error("零碎課検出処理失敗");
            } else {
                logger.warn("零碎課検出処理狀態異常: {}", status);
            }

            if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                logger.error("KNDB4030 零碎課提醒作業執行異常:");
                for (Throwable throwable : jobExecution.getAllFailureExceptions()) {
                    logger.error("  - {}: {}", throwable.getClass().getSimpleName(), throwable.getMessage());
                }
            }

            logger.info("*************************************************");
        }
    }
}
