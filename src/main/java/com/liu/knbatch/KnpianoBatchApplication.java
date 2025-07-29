package com.liu.knbatch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling  // 启用定时任务
@MapperScan("com.liu.knbatch.dao")
public class KnpianoBatchApplication {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static void main(String[] args) {
        // 检查是否为手动执行模式
        boolean isManualMode = hasManualJobParameter(args);
        
        if (isManualMode) {
            // 手动执行模式：执行完退出
            executeManualJob(args);
        } else {
            // 服务模式：持续运行，等待定时任务
            runAsService(args);
        }
    }
    
    /**
     * 检查是否为手动执行模式
     */
    private static boolean hasManualJobParameter(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--job.name=")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 手动执行模式
     * 使用开发环境配置，适合本地开发和批处理测试
     */
    private static void executeManualJob(String[] args) {
        System.setProperty("spring.profiles.active", "dev");
        System.out.println("=== 手动执行模式 ===");
        System.out.println("使用环境: 开发环境 (dev)");
        System.out.println("适用于: 本地开发、功能测试、问题排查");
        
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
    
    /**
     * 自动执行模式：持续运行，等待定时任务
     * 使用生产环境配置，适合生产部署和定时任务执行
     */
    private static void runAsService(String[] args) {
        System.setProperty("spring.profiles.active", "prod");
        System.out.println("=== 自动执行模式 ===");
        // System.out.println("使用环境: 生产环境 (prod)");
        // System.out.println("适用于: 生产部署、定时任务执行");
        // System.out.println("");
        System.out.println("启动 KnPiano Batch 管理系统...");
        System.out.println("系统将持续运行，等待定时任务执行");
        System.out.println("");
        System.out.println("已注册的定时任务:");
        System.out.println("  - KNDB1010: 每月1号凌晨1:00执行钢琴课程级别矫正");
        System.out.println("  - KNDB1020: 每周日凌晨2:00执行学生信息同步");
        System.out.println("");
        System.out.println("日志查看:");
        System.out.println("  - 主日志: tail -f /var/log/knpiano-batch/knpiano-batch.log");
        System.out.println("  - 错误日志: tail -f /var/log/knpiano-batch/knpiano-batch-error.log");
        System.out.println("=====================================");
        
        SpringApplication.run(KnpianoBatchApplication.class, args);
        // 应用会持续运行，不会退出
    }

    private static String getJobName(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--job.name=")) {
                return arg.substring("--job.name=".length());
            }
        }
        throw new IllegalArgumentException("缺少必要参数 --job.name，支持的值: " +
                "KNDB1010_MANUAL, KNDB1010_AUTO, KNDB1020_MANUAL, KNDB1020_AUTO");
    }

    private static String getBaseDate(String[] args, String jobName) {
        if (jobName.endsWith("_AUTO")) {
            return LocalDate.now().format(DATE_FORMATTER);
        } else if (jobName.endsWith("_MANUAL")) {
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
        String businessModule;
        
        // 根据作业名称选择对应的Job和业务模块
        if ("KNDB1010_MANUAL".equals(jobName) || "KNDB1010_AUTO".equals(jobName)) {
            job = context.getBean("kndb1010Job", Job.class);
            businessModule = "KNDB1010";
        } else if ("KNDB1020_MANUAL".equals(jobName) || "KNDB1020_AUTO".equals(jobName)) {
            job = context.getBean("kndb1020Job", Job.class);
            businessModule = "KNDB1020";
        } else {
            throw new IllegalArgumentException("未找到对应的作业配置: " + jobName + 
                "，支持的值: KNDB1010_MANUAL, KNDB1010_AUTO, KNDB1020_MANUAL, KNDB1020_AUTO");
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", baseDate)
                .addString("jobMode", jobName.contains("MANUAL") ? "MANUAL" : "AUTO")
                .addString("businessModule", businessModule)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        System.out.println("开始执行批处理作业: " + jobName);
        System.out.println("业务模块: " + businessModule);
        System.out.println("基准日期: " + baseDate);
        System.out.println("执行环境: 开发环境 (dev)");
        jobLauncher.run(job, jobParameters);
        System.out.println("批处理作业执行完成");
    }
}