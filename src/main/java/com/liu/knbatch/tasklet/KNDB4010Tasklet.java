package com.liu.knbatch.tasklet;

import com.liu.knbatch.config.BatchMailInfo;
import com.liu.knbatch.dao.BatchMailConfigDao;
import com.liu.knbatch.dao.KNDB4010Dao;
import com.liu.knbatch.entity.KNDB4010Entity;
import com.liu.knbatch.service.SimpleEmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * KNDB4010 次周自动排课正 业务处理任务
 * 概要：观妮通常在每周日的晚上对下一周的课程进行一周排课，此Batch处理就是替代她执行一周排课
 * 业务逻辑：
 * 1. 获取自动排课更新记录
 * 2. 如果对象周已经排课了，则停止处理
 * 3. 如果对象周尚未排课，则执行对象周自动排课操作
 * 
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDB4010Tasklet implements Tasklet {
    
    private static final Logger logger = LoggerFactory.getLogger(KNDB4010Tasklet.class);
    private String jobId = "KNDB4010";
    private String startWeekDate;
    private String endWeekDate;


    @Autowired
    private KNDB4010Dao kndb4010Dao;
    @Autowired
    private BatchMailConfigDao mailDao;

    @Autowired(required = false)
    private SimpleEmailService emailService;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB4010";
        String description="自动排下周课程";
        boolean success = false;
        int processedCount = 0;
        int updatedCount = 0;
        StringBuilder logContent = new StringBuilder();
        
        addLog(logContent, "========== " + batchName + " 批处理开始执行 ==========");
        logger.info("========== {} 批处理开始执行 ==========", batchName);
        
        try {
            // 获取作业参数
            String baseDate = (String) chunkContext.getStepContext()
                    .getJobParameters().get("baseDate");
            String jobMode = (String) chunkContext.getStepContext()
                    .getJobParameters().get("jobMode");

            // 日期格式化器（yyyyMMdd）
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            // 将 baseDate 字符串转为 LocalDate
            LocalDate baseLocalDate = LocalDate.parse(baseDate, formatter);

            // 获取次日
            LocalDate nextDate = baseLocalDate.plusDays(1);

            // 转回字符串格式（yyyyMMdd）
            baseDate = nextDate.format(formatter);
            
            addLog(logContent, "批处理参数 - 基准日期: " + baseDate + ", 执行模式: " + jobMode);
            logger.info("批处理参数 - 基准日期: {}, 执行模式: {}", baseDate, jobMode);
            
            // 步骤1: 获取排课钢琴错误级别的课程记录
            addLog(logContent, "步骤1: 查看该日期所在的星期的排课状态信息...");
            logger.info("步骤1: 查看该日期所在的星期的排课状态信息...");
            
            KNDB4010Entity kndb4010Entity = kndb4010Dao.getFixedStatusInfo(baseDate.replaceAll("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3"));
        
            int fixedStatus = kndb4010Entity.getFixedStatus();
            
            addLog(logContent, "步骤1: 查实该星期的排课状态是 - : " + fixedStatus);
            logger.info("步骤1: 查实该星期的排课状态是 - : {}", fixedStatus);
            
            if (fixedStatus == 1) {
                String message = "该星期的课程，即" + kndb4010Entity.getStartWeekDate() + "至" + kndb4010Entity.getEndWeekDate() + "的课程已经排课完了.";
                addLog(logContent, message);
                logger.info(message);
                
                success = true;
                logExecutionResult(batchName, "SUCCESS", processedCount, updatedCount, startTime, logContent);
                return RepeatStatus.FINISHED;
            }
            
            // 步骤2: 执行下一周的一周排课作业
            String startDate = kndb4010Entity.getStartWeekDate();
            String endDate = kndb4010Entity.getEndWeekDate();

            // 给成员变量赋值
            this.startWeekDate = startDate;
            this.endWeekDate = endDate;
            
            addLog(logContent, "步骤2: 开始执行下一周的一周排课作业...");
            addLog(logContent, "排课周期: " + startDate + " 至 " + endDate);
            logger.info("步骤2: 开始执行下一周的一周排课作业...");
            
            kndb4010Dao.doLsnWeeklySchedual(startDate, endDate,"kn-lsn-");
            processedCount = 1; // 执行了一次排课操作

            addLog(logContent, "步骤2: 下一周的一周排课作业完成");
            logger.info("步骤2: 下一周的一周排课作业完成");
            
            // 更新对象排课周的排课状态
            addLog(logContent, "更新: 排课状态表更新开始...");
            logger.info("更新: 排课状态表更新开始...");
            
            updatedCount = kndb4010Dao.updateWeeklyBatchStatus(startDate, endDate);
            
            addLog(logContent, "更新: 完成，有" + updatedCount + "条记录被更新");
            logger.info("更新: 完成，有{}条记录被更新", updatedCount);
            
            success = true;
            logExecutionResult(batchName, "SUCCESS", processedCount, updatedCount, startTime, logContent);
            
        } catch (Exception e) {
            addLog(logContent, "========== " + batchName + " 批处理执行异常 ==========");
            addLog(logContent, "错误信息: " + e.getMessage());
            logger.error("========== {} 批处理执行异常 ==========", batchName, e);
            
            success = false;
            logExecutionResult(batchName, "ERROR", processedCount, updatedCount, startTime, logContent);
            throw e;
        } finally {
            // 发送邮件通知
            sendEmailNotification(batchName, description, success, logContent.toString());
        }

        return RepeatStatus.FINISHED;
    }
    
    /**
     * 添加日志条目（带时间戳）
     */
    private void addLog(StringBuilder logContent, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logContent.append(String.format("[%s] %s\n", timestamp, message));
    }
    
    /**
     * 记录执行结果日志
     * 
     * @param batchName 批处理名称
     * @param status 执行状态
     * @param readCount 读取记录数
     * @param writeCount 写入记录数
     * @param startTime 开始时间
     * @param logContent 日志内容收集器
     */
    private void logExecutionResult(String batchName, String status, int readCount, int writeCount, long startTime, StringBuilder logContent) {
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        addLog(logContent, "========== " + batchName + " 批处理执行完成 ==========");
        addLog(logContent, "批处理名称: " + batchName);
        addLog(logContent, "执行状态: " + status);
        addLog(logContent, "处理数据条数: " + readCount);
        addLog(logContent, "更新数据条数: " + writeCount);
        addLog(logContent, "执行时间: " + executionTime + " ms (" + (executionTime / 1000.0) + " 秒)");
        addLog(logContent, "执行结束时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        addLog(logContent, "================================================");
        
        logger.info("========== {} 批处理执行完成 ==========", batchName);
        logger.info("批处理名称: {}", batchName);
        logger.info("执行状态: {}", status);
        logger.info("处理数据条数: {}", readCount);
        logger.info("更新数据条数: {}", writeCount);
        logger.info("执行时间: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
        logger.info("执行结束时间: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("================================================");
    }
    
    /**
     * 发送邮件通知
     */
    private void sendEmailNotification(String jobName, String description, boolean success, String logContent) {

        // 从数据库邮件管理表提取邮件管理信息
        BatchMailInfo mailInfo = mailDao.selectMailInfo(jobId);

        try {
            if (emailService != null) {
                // 给程序维护者发送邮件
                emailService.setFromEmail(mailInfo.getEmailFrom());
                emailService.setToEmails(mailInfo.getMailToDevloper());

                emailService.sendBatchNotification(jobName, description, success, logContent);

                // 如果用户邮件不为空，则给用户发送邮件
                if (!mailInfo.getEmailToUser().isEmpty()){
                    emailService.setFromEmail(mailInfo.getEmailFrom());
                    emailService.setToEmails(mailInfo.getEmailToUser());
                    String mailContent = mailInfo.getMailContentForUser();
                    mailContent = mailContent.replace("FROMDATE", this.startWeekDate)
                        .replace("TODATE", this.endWeekDate);
                    emailService.sendBatchNotification(jobName, description, success, mailContent);
                }

                logger.info("邮件通知发送完成 - jobName: {}, success: {}", jobName, success);
            } else {
                logger.info("邮件服务未启用，跳过邮件发送 - jobName: {}", jobName);
            }
        } catch (Exception e) {
            logger.error("发送邮件通知时出错 - jobName: {}, error: {}", jobName, e.getMessage(), e);
            // 不要因为邮件发送失败而影响批处理任务的状态
        }
    }
}