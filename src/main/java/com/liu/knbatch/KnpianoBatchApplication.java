package com.liu.knbatch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
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
            // 手动执行模式：执行完退出，不启动Web容器
            executeManualJob(args);
        } else {
            // Web服务模式：启动Web容器和批处理服务，持续运行
            runAsWebService(args);
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
     * 使用开发环境配置，适合本地开发和批处理测试，不启动Web容器
     */
    private static void executeManualJob(String[] args) {
        System.setProperty("spring.profiles.active", "dev");
        System.out.println("=== 手动执行模式 ===");
        System.out.println("使用环境: 开发环境 (dev)");
        System.out.println("适用于: 本地开发、功能测试、问题排查");
        
        // 创建非Web应用
        SpringApplication app = new SpringApplication(KnpianoBatchApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(args);
        
        try {
            String jobName = getJobName(args, context);
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
     * Web服务模式：启动Web容器和批处理服务
     * 使用生产环境配置，同时提供Web管理界面和定时任务执行
     */
    private static void runAsWebService(String[] args) {
        System.setProperty("spring.profiles.active", "prod");
        System.out.println("=== Web服务模式 ===");
        System.out.println("启动 KnPiano Batch Web 管理系统...");
        System.out.println("Web管理界面: http://localhost:8081");
        System.out.println("系统将持续运行，提供Web管理界面和定时任务执行");
        System.out.println("");
        

        // 强制指定为Web应用
        SpringApplication app = new SpringApplication(KnpianoBatchApplication.class);
        app.setWebApplicationType(WebApplicationType.SERVLET);
        ConfigurableApplicationContext context = app.run(args);
        
        // 启动完成后显示信息
        displayWebServiceInfo(context);
    }
    


    private static void displayWebServiceInfo(ConfigurableApplicationContext context) {
    try {
        BatchJobRegistry registry = context.getBean(BatchJobRegistry.class);
        displayRegisteredJobs(registry.getAllJobs());
        
        System.out.println("Web管理界面已启动:");
        System.out.println("  - 登录页面: http://localhost:8081");
        System.out.println("  - 作业配置管理: http://localhost:8081/batch/job");
        System.out.println("  - 邮件配置管理: http://localhost:8081/batch/mail");
        System.out.println("=====================================");
        
    } catch (Exception e) {
        System.err.println("批处理服务初始化警告: " + e.getMessage());
    }
}


    /**
     * 初始化批处理服务信息显示
     * 这个方法在Web容器启动后调用，用于显示批处理服务信息
     */
    public void initializeBatchServices(ConfigurableApplicationContext context) {
        try {  
            // 显示批处理服务信息
            BatchJobRegistry registry = context.getBean(BatchJobRegistry.class);
            displayRegisteredJobs(registry.getAllJobs());
            
            System.out.println("Web管理界面已启动:");
            System.out.println("  - 登录页面: http://localhost:8081");
            System.out.println("  - 作业配置管理: http://localhost:8081/batch/job");
            System.out.println("  - 邮件配置管理: http://localhost:8081/batch/mail");
            System.out.println("");
            System.out.println("日志查看:");
            System.out.println("  - 主日志: tail -f logs/knpiano-batch.log");
            System.out.println("  - 错误日志: tail -f logs/knpiano-batch-error.log");
            System.out.println("=====================================");
            
        } catch (Exception e) {
            System.err.println("获取批处理注册信息失败: " + e.getMessage());
            System.err.println("请检查 batch-jobs.xml 配置文件是否存在且格式正确");
            e.printStackTrace();
            // 注意：这里不退出应用，让Web服务继续运行
        }
    }
    
    /**
     * 动态显示所有已注册的批处理作业
     */
    private static void displayRegisteredJobs(List<BatchJobInfo> jobs) {
        System.out.println("已注册的定时任务:");
        for (BatchJobInfo jobInfo : jobs) {
            // 使用XML配置中的描述，消除硬编码
            String cronDesc = jobInfo.getCronDescription() != null ? 
                              jobInfo.getCronDescription() : 
                             (jobInfo.getCronExpression() != null ? jobInfo.getCronExpression() : "未设置定时");
            System.out.println("  - " + jobInfo.getJobId() + ": " + cronDesc + " 执行" + jobInfo.getDescription());
        }
        System.out.println("");
    }

    /**
     * 动态获取作业名称，并提供动态的错误提示
     */
    private static String getJobName(String[] args, ConfigurableApplicationContext context) {
        for (String arg : args) {
            if (arg.startsWith("--job.name=")) {
                return arg.substring("--job.name=".length());
            }
        }
        
        // 动态生成错误提示信息
        StringBuilder errorMessage = new StringBuilder("缺少必要参数 --job.name，支持的作业格式: ");
        
        try {
            BatchJobRegistry registry = context.getBean(BatchJobRegistry.class);
            List<BatchJobInfo> jobs = registry.getAllJobs();
            
            if (!jobs.isEmpty()) {
                errorMessage.append("\n可用的作业:");
                for (BatchJobInfo job : jobs) {
                    errorMessage.append("\n  - ").append(job.getJobId())
                              .append("_MANUAL --base.date=yyyyMMdd (手动执行)")
                              .append("\n  - ").append(job.getJobId())
                              .append("_AUTO (自动使用当前日期)");
                }
            } else {
                errorMessage.append("未找到任何已配置的作业，请检查 batch-jobs.xml 配置文件");
            }
        } catch (Exception e) {
            errorMessage.append("请使用格式: --job.name=作业ID_MANUAL 或 --job.name=作业ID_AUTO");
        }
        
        throw new IllegalArgumentException(errorMessage.toString());
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