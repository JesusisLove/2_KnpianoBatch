package com.liu.knbatch.tasklet;

import com.liu.knbatch.dao.KNDB2030Dao;
import com.liu.knbatch.entity.KNDB2030Entity;
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
 * KNDB2030 课费预支付再调整 业务处理任务
 * 
 * 业务逻辑：
 * 1. 获取有没有本应该已经签到的预支付课，却没有签到
 * 2. 如果记录数为0，则停止处理
 * 3. 如果记录数大于0，则执行预支付矫正操作
 * 4. 基于第3步，用已签到的lesson_id取替换本该签到而没有签到的lesson_id（更新《预支付表》）
 * 
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDB2030Tasklet implements Tasklet {
    
    private static final Logger logger = LoggerFactory.getLogger(KNDB2030Tasklet.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    @Autowired
    private KNDB2030Dao kndb2030Dao;

    @Autowired(required = false)
    private SimpleEmailService emailService;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB2030";
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
            String yearMonth = date.format(MONTH_FORMATTER);
            
            addLog(logContent, "目标处理月份: " + yearMonth);
            logger.info("目标处理月份: {}", yearMonth);
            
            // 步骤1: 获取排课钢琴错误级别的课程记录
            addLog(logContent, "步骤1: 开始获取排课钢琴错误级别的课程记录...");
            logger.info("步骤1: 开始获取排课钢琴错误级别的课程记录...");
            
            List<KNDB2030Entity> advcAdjustedList = kndb2030Dao.getAdvcLsnPayList(yearMonth);
            incorrectCount = advcAdjustedList.size();
            
            addLog(logContent, "步骤1: 完成 - 发现预支付再调整记录数: " + incorrectCount);
            logger.info("步骤1: 完成 - 发现预支付再调整记录数: {}", incorrectCount);
            
            if (incorrectCount == 0) {
                addLog(logContent, "未发现预支付再调整的课程记录，批处理正常结束");
                logger.info("未发现预支付再调整的课程记录，批处理正常结束");
                
                success = true;
                logExecutionResult(batchName, "SUCCESS", 0, 0, startTime, logContent);
                return RepeatStatus.FINISHED;
            }
            
            // 记录错误课程的详细信息
            addLog(logContent, "预支付再调整课程记录详情:");
            for (int i = 0; i < Math.min(advcAdjustedList.size(), 5); i++) { // 邮件中最多显示5条
                KNDB2030Entity lesson = advcAdjustedList.get(i);
                String detailInfo = String.format("  - 学生ID: %s, 科目ID: %s, 当前级别: %s, 排课日期: %s", 
                        lesson.getStuId(), lesson.getSubjectId(), lesson.getLessonId());
                addLog(logContent, detailInfo);
                
                if (logger.isDebugEnabled()) {
                    logger.debug(detailInfo);
                }
            }
            if (advcAdjustedList.size() > 5) {
                addLog(logContent, "  ... 还有 " + (advcAdjustedList.size() - 5) + " 条记录");
                if (logger.isDebugEnabled()) {
                    logger.debug("  ... 还有 {} 条记录", advcAdjustedList.size() - 5);
                }
            }
            
            // 步骤2: 执行预支付再调整操作
            addLog(logContent, "步骤2: 开始执行预支付再调整...");
            logger.info("步骤2: 开始执行预支付再调整...");

            for (KNDB2030Entity entity : advcAdjustedList) {
                String lessonId = entity.getLessonId();
                String lsnFeeId = entity.getLsnFeeId();
                String lsnPayId = entity.getLsnPayId();
                String subjectId = entity.getSubjectId();
                String stuId = entity.getStuId();

                // ①通过失效的lessonId取得有效的lessonId(该月多个有效的lesson_id，对其排序，只返回1个)
                String validLessonId = kndb2030Dao.getValidAdvancePaymentLessonId(stuId, subjectId, lessonId);
                
                // ②更新《课费预支付》表，把失效的lessonId，用①抽出的lessonId来替换失效的lessonId
                int cnt = kndb2030Dao.updateAdvancePayment(validLessonId, lessonId, lsnFeeId, lsnPayId);
                
                // ③删除《课费表》中失效的lessonId记录，因为这个课程没有签到，所以把它从《课费表》中删除掉
                kndb2030Dao.deleteInvalidAdvancePaymentLessonId(lessonId);

                updatedCount += cnt;

            }

            
            addLog(logContent, "步骤2: 完成 - 成功预支付再调整记录数: " + updatedCount);
            logger.info("步骤2: 完成 - 成功预支付再调整记录数: {}", updatedCount);
            
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
        addLog(logContent, "发现预支付再调整记录数: " + readCount);
        addLog(logContent, "执行预支付再调整记录数: " + writeCount);
        addLog(logContent, "执行时间: " + executionTime + " ms (" + (executionTime / 1000.0) + " 秒)");
        addLog(logContent, "执行结束时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        addLog(logContent, "================================================");
        
        logger.info("========== {} 批处理执行完成 ==========", batchName);
        logger.info("批处理名称: {}", batchName);
        logger.info("执行状态: {}", status);
        logger.info("发现预支付再调整记录数: {}", readCount);
        logger.info("执行预支付再调整记录数: {}", writeCount);
        logger.info("执行时间: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
        logger.info("执行结束时间: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("================================================");
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