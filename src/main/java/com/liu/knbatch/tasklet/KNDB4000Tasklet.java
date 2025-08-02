package com.liu.knbatch.tasklet;

import com.liu.knbatch.dao.KNDB4000Dao;
import com.liu.knbatch.entity.KNDB4000Entity;
import com.liu.knbatch.service.SimpleEmailService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * KNDB4000 次周自动排课正 业务处理任务
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
public class KNDB4000Tasklet implements Tasklet {
    
    private static final Logger logger = LoggerFactory.getLogger(KNDB4000Tasklet.class);
    
    @Autowired
    private KNDB4000Dao kndb4000Dao;

    @Autowired(required = false)
    private SimpleEmailService emailService;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB4000";
        boolean success = false;
        int deletedCount = 0;
        int insertedCount = 0;
        StringBuilder logContent = new StringBuilder();
        
        addLog(logContent, "========== " + batchName + " 批处理开始执行 ==========");
        logger.info("========== {} 批处理开始执行 ==========", batchName);
        
        try {
            // 获取作业参数
            String baseDate = (String) chunkContext.getStepContext()
                    .getJobParameters().get("baseDate");
            String jobMode = (String) chunkContext.getStepContext()
                    .getJobParameters().get("jobMode");
            
            addLog(logContent, "批处理参数 - 基准日期: " + baseDate + ", 执行模式: " + jobMode);
            logger.info("批处理参数 - 基准日期: {}, 执行模式: {}", baseDate, jobMode);
            
            // 步骤1: 删除去年的年度周次表生成记录
            addLog(logContent, "步骤1: 查看该日期所在的星期的排课状态信息...");
            logger.info("步骤1: 查看该日期所在的星期的排课状态信息...");
            
            deletedCount = kndb4000Dao.deleteAll();
        
            addLog(logContent, "步骤1: 删除年度周次表记录 - : " + deletedCount + " 条");
            logger.info("步骤1: 删除年度周次表记录 - : {} 条", deletedCount);
            
            // 步骤2: 执行新年度的年度周次表生成
            int year = LocalDate.now().getYear(); // 获取年份部分
            addLog(logContent, "步骤2: 新年度周次表生成开始 - 当前年度 : " + year + " 年");
            logger.info("步骤2: 新年度周次表生成开始 - 当前年度 : {} 年", year);
            
            insertedCount = insertWeeksForYear(year, logContent);

            addLog(logContent, "步骤2: 新年度周次表生成结束  - 生成记录 : " + insertedCount + " 条");
            logger.info("步骤2: 新年度周次表生成结束  - 生成记录 : {} 条", insertedCount);
            
            success = true;
            logExecutionResult(batchName, "SUCCESS", deletedCount, insertedCount, startTime, logContent);
            
            return RepeatStatus.FINISHED;
            
        } catch (Exception e) {
            addLog(logContent, "========== " + batchName + " 批处理执行异常 ==========");
            addLog(logContent, "错误信息: " + e.getMessage());
            logger.error("========== {} 批处理执行异常 ==========", batchName, e);
            
            success = false;
            logExecutionResult(batchName, "ERROR", deletedCount, insertedCount, startTime, logContent);
            throw e;
        } finally {
            // 发送邮件通知
            sendEmailNotification(batchName, success, logContent.toString());
        }
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
        addLog(logContent, "删除记录数: " + readCount);
        addLog(logContent, "插入记录数: " + writeCount);
        addLog(logContent, "执行时间: " + executionTime + " ms (" + (executionTime / 1000.0) + " 秒)");
        addLog(logContent, "执行结束时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        addLog(logContent, "================================================");
        
        logger.info("========== {} 批处理执行完成 ==========", batchName);
        logger.info("批处理名称: {}", batchName);
        logger.info("执行状态: {}", status);
        logger.info("删除记录数: {}", readCount);
        logger.info("插入记录数: {}", writeCount);
        logger.info("执行时间: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
        logger.info("执行结束时间: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("================================================");
    }

    /**
     * 为指定年份插入周次记录
     * 
     * @param year 年份
     * @param logContent 日志内容收集器
     * @return 插入的记录数
     */
    private int insertWeeksForYear(int year, StringBuilder logContent) {
        LocalDate date = LocalDate.of(year, 1, 1);
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int cnt = 0;
        
        addLog(logContent, "开始为 " + year + " 年生成周次表...");
        
        while (date.getYear() == year) {
            int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
            LocalDate weekStart = date.with(DayOfWeek.MONDAY);
            LocalDate weekEnd = date.with(DayOfWeek.SUNDAY);

            // 将 LocalDate 转换成"yyyy-MM-dd"的字符串日期格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String weekStartStr = weekStart.format(formatter);
            String weekEndStr = weekEnd.format(formatter);

            if (weekStart.getYear() < year) {
                date = date.plusWeeks(1);
                continue;
            }

            KNDB4000Entity status = new KNDB4000Entity();
            status.setWeekNumber(weekNumber);
            status.setStartWeekDate(weekStartStr);
            status.setEndWeekDate(weekEndStr);

            kndb4000Dao.insertFixedLessonStatus(status);
            
            // 每10条记录记录一次进度
            if (cnt % 10 == 0) {
                addLog(logContent, "已生成第 " + (cnt + 1) + " 周记录: 第" + weekNumber + "周 (" + weekStartStr + " 至 " + weekEndStr + ")");
            }

            date = date.plusWeeks(1);
            cnt++;
        }
        
        addLog(logContent, "年度周次表生成完成，共生成 " + cnt + " 条记录");
        return cnt;
    }
    
    /**
     * 发送邮件通知
     */
    private void sendEmailNotification(String jobName, boolean success, String logContent) {
        try {
            if (emailService != null) {
                emailService.sendBatchNotification(jobName, success, logContent);
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