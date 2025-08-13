package com.liu.knbatch.tasklet;

import com.liu.knbatch.dao.KNDB1010Dao;
import com.liu.knbatch.entity.KNDB1010Entity;
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
import java.util.List;

/**
 * KNDB1010 钢琴课程级别矫正 业务处理任务
 * 
 * 业务逻辑：
 * 1. 获取排课钢琴错误级别的课程记录
 * 2. 如果记录数为0，则停止处理
 * 3. 如果记录数大于0，则执行数据矫正操作
 * 4. 验证矫正结果
 * 
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDB1010Tasklet implements Tasklet {
    
    private static final Logger logger = LoggerFactory.getLogger(KNDB1010Tasklet.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    @Autowired
    private KNDB1010Dao kndb1010Dao;

    @Autowired(required = false)
    private SimpleEmailService emailService;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB1010";
        String description = "钢琴课程级别矫正";
        boolean success = false;
        int incorrectCount = 0;
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
            
            addLog(logContent, "批处理参数 - 基准日期: " + baseDate + ", 执行模式: " + jobMode);
            logger.info("批处理参数 - 基准日期: {}, 执行模式: {}", baseDate, jobMode);
            
            // 转换日期格式 yyyyMMdd -> yyyy-MM
            LocalDate date = LocalDate.parse(baseDate, DATE_FORMATTER);
            String targetMonth = date.format(MONTH_FORMATTER);
            
            addLog(logContent, "目标处理月份: " + targetMonth);
            logger.info("目标处理月份: {}", targetMonth);
            
            // 步骤1: 获取排课钢琴错误级别的课程记录
            addLog(logContent, "步骤1: 开始获取排课钢琴错误级别的课程记录...");
            logger.info("步骤1: 开始获取排课钢琴错误级别的课程记录...");
            
            List<KNDB1010Entity> incorrectLessons = kndb1010Dao.selectIncorrectPianoLevelLessons(targetMonth);
            incorrectCount = incorrectLessons.size();
            
            addLog(logContent, "步骤1: 完成 - 发现错误级别课程记录数: " + incorrectCount);
            logger.info("步骤1: 完成 - 发现错误级别课程记录数: {}", incorrectCount);
            
            if (incorrectCount == 0) {
                addLog(logContent, "未发现错误的钢琴级别课程记录，批处理正常结束");
                logger.info("未发现错误的钢琴级别课程记录，批处理正常结束");
                
                success = true;
                logExecutionResult(batchName, "SUCCESS", 0, 0, startTime, logContent);
                return RepeatStatus.FINISHED;
            }
            
            // 记录错误课程的详细信息
            addLog(logContent, "错误课程记录详情:");
            for (int i = 0; i < Math.min(incorrectLessons.size(), 5); i++) { // 邮件中最多显示5条
                KNDB1010Entity lesson = incorrectLessons.get(i);
                String detailInfo = String.format("  - 学生ID: %s, 科目ID: %s, 当前级别: %s, 排课日期: %s", 
                        lesson.getStuId(), lesson.getSubjectId(), 
                        lesson.getSubjectSubId(), lesson.getSchedualDate());
                addLog(logContent, detailInfo);
                
                if (logger.isDebugEnabled()) {
                    logger.debug(detailInfo);
                }
            }
            if (incorrectLessons.size() > 5) {
                addLog(logContent, "  ... 还有 " + (incorrectLessons.size() - 5) + " 条记录");
                if (logger.isDebugEnabled()) {
                    logger.debug("  ... 还有 {} 条记录", incorrectLessons.size() - 5);
                }
            }
            
            // 步骤2: 执行数据矫正操作
            addLog(logContent, "步骤2: 开始执行钢琴级别课程数据矫正...");
            logger.info("步骤2: 开始执行钢琴级别课程数据矫正...");
            
            updatedCount = kndb1010Dao.updateIncorrectPianoLevelLessons(targetMonth);
            
            addLog(logContent, "步骤2: 完成 - 成功矫正课程记录数: " + updatedCount);
            logger.info("步骤2: 完成 - 成功矫正课程记录数: {}", updatedCount);
            
            // 验证矫正结果
            addLog(logContent, "验证: 开始验证矫正结果...");
            logger.info("验证: 开始验证矫正结果...");
            
            int remainingIncorrectCount = kndb1010Dao.countIncorrectPianoLevelLessons(targetMonth);
            
            if (remainingIncorrectCount == 0) {
                addLog(logContent, "验证: 成功 - 所有错误级别课程已完成矫正");
                logger.info("验证: 成功 - 所有错误级别课程已完成矫正");
                
                success = true;
                logExecutionResult(batchName, "SUCCESS", incorrectCount, updatedCount, startTime, logContent);
            } else {
                addLog(logContent, "验证: 警告 - 仍有 " + remainingIncorrectCount + " 条错误级别课程未能矫正");
                logger.warn("验证: 警告 - 仍有 {} 条错误级别课程未能矫正", remainingIncorrectCount);
                
                success = true; // 虽然有警告，但批处理本身是成功的
                logExecutionResult(batchName, "WARNING", incorrectCount, updatedCount, startTime, logContent);
            }
            
            // 更新贡献统计
            contribution.incrementReadCount();
            contribution.incrementWriteCount(updatedCount);
            
            return RepeatStatus.FINISHED;
            
        } catch (Exception e) {
            addLog(logContent, "========== " + batchName + " 批处理执行异常 ==========");
            addLog(logContent, "错误信息: " + e.getMessage());
            logger.error("========== {} 批处理执行异常 ==========", batchName, e);
            
            success = false;
            logExecutionResult(batchName, "ERROR", incorrectCount, updatedCount, startTime, logContent);
            throw e;
        } finally {
            // 发送邮件通知
            sendEmailNotification(batchName, description, success, logContent.toString());
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
        addLog(logContent, "发现错误记录数: " + readCount);
        addLog(logContent, "矫正记录数: " + writeCount);
        addLog(logContent, "执行时间: " + executionTime + " ms (" + (executionTime / 1000.0) + " 秒)");
        addLog(logContent, "执行结束时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        addLog(logContent, "================================================");
        
        logger.info("========== {} 批处理执行完成 ==========", batchName);
        logger.info("批处理名称: {}", batchName);
        logger.info("执行状态: {}", status);
        logger.info("发现错误记录数: {}", readCount);
        logger.info("矫正记录数: {}", writeCount);
        logger.info("执行时间: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
        logger.info("执行结束时间: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("================================================");
    }
    
    /**
     * 发送邮件通知
     */
    private void sendEmailNotification(String jobName, String description, boolean success, String logContent) {
        try {
            if (emailService != null) {
                emailService.sendBatchNotification(jobName, description, success, logContent);
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