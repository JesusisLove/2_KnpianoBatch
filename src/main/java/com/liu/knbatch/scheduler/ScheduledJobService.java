// package com.liu.knbatch.scheduler;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.batch.core.Job;
// import org.springframework.batch.core.JobExecution;
// import org.springframework.batch.core.JobParameters;
// import org.springframework.batch.core.JobParametersBuilder;
// import org.springframework.batch.core.launch.JobLauncher;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;

// import java.time.LocalDate;
// import java.time.format.DateTimeFormatter;

// /**
//  * 定时任务服务
//  * 负责定时执行批处理作业
//  */
// @Service
// public class ScheduledJobService {
    
//     private static final Logger logger = LoggerFactory.getLogger(ScheduledJobService.class);
//     private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
//     @Autowired
//     private JobLauncher jobLauncher;
    
//     @Autowired
//     private Job kndb1010Job;

//     public ScheduledJobService() {
//         logger.info("=== ScheduledJobService 初始化成功 ===");
//         logger.info("定时任务已注册：每月1号凌晨1:00执行KNDB1010批处理");
//         logger.info("系统时区: {}", java.time.ZoneId.systemDefault());
//         logger.info("当前时间: {}", java.time.LocalDateTime.now());
//     }

//     // Cron表达式详细解析
//     // 0 0 1 1 * ?
//     // │ │ │ │ │ │
//     // │ │ │ │ │ └── 星期 (? = 不指定)
//     // │ │ │ │ └──── 月份 (* = 每月)
//     // │ │ │ └────── 日期 (1 = 1号)
//     // │ │ └──────── 小时 (1 = 凌晨1点)
//     // │ └────────── 分钟 (0 = 0分)
//     // └──────────── 秒 (0 = 0秒)
//     /**
//      * 每月1号的凌晨1点执行KNDB1010批处理
//      * Cron表达式：0 0 1 1 * ?
//      * 
//      * 业务逻辑：
//      * - 在每月1号凌晨1:00执行
//      * - 处理当月的钢琴课程级别矫正
//      * - 例如：8月1日凌晨1:00执行，处理8月份的课程数据
//      */
//     @Scheduled(cron = "0 0 1 1 * ?")
//     public void executeKNDB1010Job() {
//         try {
//             // 获取当前月份（要处理的目标月份）
//             LocalDate currentDate = LocalDate.now(); // 当前是X月1日
            
//             String baseDate = currentDate.format(DATE_FORMATTER); // X月1日作为基准
//             String targetMonthStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM")); // 当前月份
            
//             logger.info("=== {} 月1号凌晨1:00 KNDB1010 批处理开始执行 ===", targetMonthStr);
//             logger.info("实际执行时间: {} 凌晨1:00", currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
//             logger.info("处理目标月份: {}", targetMonthStr);
//             logger.info("基准日期: {}", baseDate);
            
//             JobParameters jobParameters = new JobParametersBuilder()
//                     .addString("baseDate", baseDate)
//                     .addString("jobMode", "SCHEDULED") // 标记为定时执行
//                     .addLong("timestamp", System.currentTimeMillis())
//                     .toJobParameters();
            
//             JobExecution execution = jobLauncher.run(kndb1010Job, jobParameters);
            
//             // 检查执行结果
//             String status = execution.getStatus().toString();
//             if ("COMPLETED".equals(status)) {
//                 logger.info("=== {} 月1号 KNDB1010 批处理执行成功，已处理 {} 月份数据 ===", 
//                     targetMonthStr, targetMonthStr);
//             } else {
//                 logger.error("=== {} 月1号 KNDB1010 批处理执行失败，状态: {} ===", 
//                     targetMonthStr, status);
//             }
            
//         } catch (Exception e) {
//             logger.error("=== KNDB1010 定时批处理执行异常 ===", e);
//         }
//     }
// }