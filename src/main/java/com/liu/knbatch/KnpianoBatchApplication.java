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
import com.liu.knbatch.config.BatchJobRegistry;
import com.liu.knbatch.config.BatchJobInfo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        System.out.println("启动 KnPiano Batch 管理系统...");
        System.out.println("系统将持续运行，等待定时任务执行");
        System.out.println("");
        
        // 启动应用上下文
        ConfigurableApplicationContext context = SpringApplication.run(KnpianoBatchApplication.class, args);
        
        try {
            // 动态显示所有已注册的定时任务
            BatchJobRegistry registry = context.getBean(BatchJobRegistry.class);
            displayRegisteredJobs(registry.getAllJobs());
            
            System.out.println("日志查看:");
            System.out.println("  - 主日志: tail -f /var/log/knpiano-batch/knpiano-batch.log");
            System.out.println("  - 错误日志: tail -f /var/log/knpiano-batch/knpiano-batch-error.log");
            System.out.println("=====================================");
            
        } catch (Exception e) {
            System.err.println("获取批处理注册信息失败: " + e.getMessage());
            System.err.println("请检查 batch-jobs.xml 配置文件是否存在且格式正确");
            e.printStackTrace();
            System.exit(1);  // 纯动态版本：配置文件必须存在，否则退出
        }
        
        // 应用会持续运行，不会退出
    }
    
    /**
     * 动态显示所有已注册的批处理作业
     */
    private static void displayRegisteredJobs(List<BatchJobInfo> jobs) {
        System.out.println("已注册的定时任务:");
        for (BatchJobInfo jobInfo : jobs) {
            String cronDesc = parseCronDescription(jobInfo.getCronExpression());
            System.out.println("  - " + jobInfo.getJobId() + ": " + cronDesc + " 执行" + jobInfo.getDescription());
        }
        System.out.println("");
    }
    
    /**
     * 解析Cron表达式为人类可读的描述
     */
    private static String parseCronDescription(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return "未设置定时";
        }
        
        if ("0 0 1 1 * ?".equals(cronExpression)) {
            return "每月1号凌晨1:00";
        } else if ("0 0 2 ? * SUN".equals(cronExpression)) {
            return "每周日凌晨2:00";
        } else if ("0 0 3 ? * MON".equals(cronExpression)) {
            return "每周一凌晨3:00";
        } else if ("0 0 4 L * ?".equals(cronExpression)) {
            return "每月最后一天凌晨4:00";
        } else if ("0 0 5 1 * ?".equals(cronExpression)) {
            return "每月1号凌晨5:00";
        } else {
            return cronExpression;
        }
    }

    private static String getJobName(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--job.name=")) {
                return arg.substring("--job.name=".length());
            }
        }
        throw new IllegalArgumentException("缺少必要参数 --job.name，请使用格式: --job.name=KNDB1010_MANUAL 或 --job.name=KNDB1010_AUTO");
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
            throw new IllegalArgumentException("不支持的作业名称: " + jobName + "，请使用 _MANUAL 或 _AUTO 后缀");
        }
    }

    /**
     * 纯动态作业执行方法
     * 完全依赖XML配置，无任何硬编码
     */
    private static void executeJob(ConfigurableApplicationContext context, String jobName, String baseDate) 
            throws Exception {
        JobLauncher jobLauncher = context.getBean(JobLauncher.class);
        
        // 从作业名称中提取业务模块ID (例如: KNDB1010_MANUAL -> KNDB1010)
        String businessModule = extractBusinessModule(jobName);
        
        // 通过注册表动态获取作业信息
        BatchJobRegistry registry = context.getBean(BatchJobRegistry.class);
        BatchJobInfo jobInfo = registry.getJobInfo(businessModule);
        
        if (jobInfo == null) {
            // 显示可用的作业列表
            System.err.println("未找到业务模块 " + businessModule + " 对应的批处理作业配置");
            System.err.println("可用的作业列表:");
            for (BatchJobInfo availableJob : registry.getAllJobs()) {
                System.err.println("  - " + availableJob.getJobId() + ": " + availableJob.getDescription());
            }
            throw new IllegalArgumentException("未找到业务模块 " + businessModule + " 对应的批处理作业配置");
        }
        
        // 动态获取Spring Bean中的Job实例
        Job job = context.getBean(jobInfo.getBeanName(), Job.class);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", baseDate)
                .addString("jobMode", jobName.contains("MANUAL") ? "MANUAL" : "AUTO")
                .addString("businessModule", businessModule)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        System.out.println("开始执行批处理作业: " + jobName);
        System.out.println("业务模块: " + businessModule);
        System.out.println("作业描述: " + jobInfo.getDescription());
        System.out.println("基准日期: " + baseDate);
        System.out.println("执行环境: 开发环境 (dev)");
        
        jobLauncher.run(job, jobParameters);
        System.out.println("批处理作业执行完成");
    }
    
    /**
     * 从作业名称中提取业务模块ID
     * 例如: KNDB1010_MANUAL -> KNDB1010, KNDB1020_AUTO -> KNDB1020
     */
    private static String extractBusinessModule(String jobName) {
        if (jobName.contains("_")) {
            return jobName.substring(0, jobName.lastIndexOf("_"));
        }
        return jobName;
    }
}