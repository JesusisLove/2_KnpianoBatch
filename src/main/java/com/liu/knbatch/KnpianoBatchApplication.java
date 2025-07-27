package com.liu.knbatch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
@EnableBatchProcessing
@MapperScan("com.liu.knbatch.dao")
public class KnpianoBatchApplication {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "batch");
        
        ConfigurableApplicationContext context = SpringApplication.run(KnpianoBatchApplication.class, args);
        
        try {
            String jobName = getJobName(args);
            String baseDate = getBaseDate(args, jobName);
            executeJob(context, jobName, baseDate);
        } catch (Exception e) {
            System.err.println("批处理执行失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            context.close();
        }
    }

    private static String getJobName(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--job.name=")) {
                return arg.substring("--job.name=".length());
            }
        }
        throw new IllegalArgumentException("缺少必要参数 --job.name，支持的值: KNDB1010_MANUAL, KNDB1010_AUTO");
    }

    private static String getBaseDate(String[] args, String jobName) {
        if ("KNDB1010_AUTO".equals(jobName)) {
            return LocalDate.now().format(DATE_FORMATTER);
        } else if ("KNDB1010_MANUAL".equals(jobName)) {
            for (String arg : args) {
                if (arg.startsWith("--base.date=")) {
                    return arg.substring("--base.date=".length());
                }
            }
            throw new IllegalArgumentException("手动执行模式缺少必要参数 --base.date=yyyyMMdd");
        } else {
            throw new IllegalArgumentException("不支持的作业名称: " + jobName);
        }
    }

    private static void executeJob(ConfigurableApplicationContext context, String jobName, String baseDate) 
            throws Exception {
        JobLauncher jobLauncher = context.getBean(JobLauncher.class);
        
        Job job;
        if ("KNDB1010_MANUAL".equals(jobName) || "KNDB1010_AUTO".equals(jobName)) {
            job = context.getBean("kndb1010Job", Job.class);
        } else {
            throw new IllegalArgumentException("未找到对应的作业配置: " + jobName);
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", baseDate)
                .addString("jobMode", jobName.contains("MANUAL") ? "MANUAL" : "AUTO")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        System.out.println("开始执行批处理作业: " + jobName + ", 基准日期: " + baseDate);
        jobLauncher.run(job, jobParameters);
        System.out.println("批处理作业执行完成");
    }
}