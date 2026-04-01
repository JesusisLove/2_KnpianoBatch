package com.liu.knbatch.tasklet;

import com.liu.knbatch.config.BatchMailInfo;
import com.liu.knbatch.dao.BatchMailConfigDao;
import com.liu.knbatch.dao.KNDB2040Dao;
import com.liu.knbatch.entity.KNDB2040Entity;
import com.liu.knbatch.service.SimpleEmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * KNDB2040 学生指定月课费金额修正 业务处理任务
 *
 * 业务逻辑：
 * 1. 向 t_info_student_document 插入新子科目档案记录（adjusted_date = 起价月1日）
 * 2. 修正 t_info_lesson_fee 中指定学生+科目+起价年月以降的未付费（own_flg=0）记录
 *    - lessonType=1（月计划课）且 lsn_fee=0 的第5节跳过，不修正
 *    - lessonType=2（月加课）/ lessonType=0（课结算）每条直接修正
 * 两个操作在同一事务内执行。
 *
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDB2040Tasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(KNDB2040Tasklet.class);
    private static final int LESSON_TYPE_MONTHLY_SCHEDUAL = 1;
    private String jobId = "KNDB2040";

    @Autowired
    private KNDB2040Dao kndb2040Dao;

    @Autowired
    private BatchMailConfigDao mailDao;

    @Autowired(required = false)
    private SimpleEmailService emailService;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB2040";
        String description = "学生指定月课费金额修正";
        boolean success = false;
        int updatedCount = 0;
        StringBuilder logContent = new StringBuilder();

        addLog(logContent, "========== " + batchName + " 批处理开始执行 ==========");
        logger.info("========== {} 批处理开始执行 ==========", batchName);

        try {
            // 从 JobParameters 取参数
            Map<String, Object> params = chunkContext.getStepContext().getJobParameters();

            String stuId                = (String) params.get("stuId");
            String subjectId            = (String) params.get("subjectId");
            String newSubjectSubId      = (String) params.get("newSubjectSubId");
            double newLessonFee         = (Double) params.get("newLessonFee");
            double newLessonFeeAdjusted = (Double) params.get("newLessonFeeAdjusted");
            String startYearMonth       = (String) params.get("startYearMonth");

            addLog(logContent, "处理参数 - 学生ID: " + stuId + ", 科目ID: " + subjectId
                    + ", 新子科目: " + newSubjectSubId + ", 起价年月: " + startYearMonth);
            logger.info("处理参数 - 学生ID: {}, 科目ID: {}, 新子科目: {}, 起价年月: {}",
                    stuId, subjectId, newSubjectSubId, startYearMonth);

            // 决定实际使用的价格（调整价格优先，未指定则用标准价格）
            double effectivePrice = (newLessonFeeAdjusted > 0) ? newLessonFeeAdjusted : newLessonFee;
            addLog(logContent, "实际使用价格: " + effectivePrice
                    + "（标准: " + newLessonFee + ", 调整: " + newLessonFeeAdjusted + "）");
            logger.info("实际使用价格: {}（标准: {}, 调整: {}）", effectivePrice, newLessonFee, newLessonFeeAdjusted);

            // ------- 操作1：INSERT 学生档案新记录 -------
            addLog(logContent, "操作1: 开始 INSERT 学生档案新记录...");
            logger.info("操作1: 开始 INSERT 学生档案新记录...");

            // 从当前档案取 pay_style / minutes_per_lsn / year_lsn_cnt
            KNDB2040Entity currentDoc = kndb2040Dao.getCurrentSubjectInfo(stuId, subjectId);

            KNDB2040Entity newDoc = new KNDB2040Entity();
            newDoc.setStuId(stuId);
            newDoc.setSubjectId(subjectId);
            newDoc.setNewSubjectSubId(newSubjectSubId);
            // adjusted_date = 起价年月的1日（如 "2026-03" → 2026-03-01）
            newDoc.setAdjustedDate(Date.valueOf(startYearMonth + "-01"));
            newDoc.setPayStyle(currentDoc.getPayStyle());
            newDoc.setMinutesPerLsn(currentDoc.getMinutesPerLsn());
            newDoc.setYearLsnCnt(currentDoc.getYearLsnCnt());
            newDoc.setNewLessonFee(newLessonFee);
            newDoc.setNewLessonFeeAdjusted(newLessonFeeAdjusted > 0 ? newLessonFeeAdjusted : 0);

            kndb2040Dao.insertStudentDocument(newDoc);

            addLog(logContent, "操作1: 完了 - stu_id=" + stuId
                    + ", 新子科目=" + newSubjectSubId + ", adjusted_date=" + newDoc.getAdjustedDate());
            logger.info("操作1: 完了 - stu_id={}, 新子科目={}, adjusted_date={}",
                    stuId, newSubjectSubId, newDoc.getAdjustedDate());

            // ------- 操作2：UPDATE 课费表 -------
            addLog(logContent, "操作2: 开始修正课费记录...");
            logger.info("操作2: 开始修正课费记录...");

            List<KNDB2040Entity> feeList = kndb2040Dao.getLsnFeeListToFix(stuId, subjectId, startYearMonth);
            addLog(logContent, "操作2: 取得修正对象记录数=" + feeList.size());
            logger.info("操作2: 取得修正对象记录数={}", feeList.size());

            int lessonUpdatedCount = 0;
            for (KNDB2040Entity fee : feeList) {
                // 操作3：用 lesson_id（主键）精准更新课程表子科目ID（所有记录均需更新）
                kndb2040Dao.updateLessonSubjectSubId(fee.getLessonId(), newSubjectSubId);
                lessonUpdatedCount++;

                // 操作2：lessonType=1（月计划课）且 lsn_fee=0 的第5节跳过课费修正
                if (fee.getLessonType() == LESSON_TYPE_MONTHLY_SCHEDUAL && fee.getLsnFee() == 0) {
                    logger.debug("  跳过课费修正（第5节 lsn_fee=0）: lsn_fee_id={}, lesson_id={}",
                            fee.getLsnFeeId(), fee.getLessonId());
                    continue;
                }
                kndb2040Dao.updateLsnFee(fee.getLsnFeeId(), fee.getLessonId(), effectivePrice);
                updatedCount++;
            }

            addLog(logContent, "操作2: 完了 - 课费修正件数=" + updatedCount);
            logger.info("操作2: 完了 - 课费修正件数={}", updatedCount);
            addLog(logContent, "操作3: 完了 - 课程子科目ID更新件数=" + lessonUpdatedCount);
            logger.info("操作3: 完了 - 课程子科目ID更新件数={}", lessonUpdatedCount);

            contribution.incrementWriteCount(updatedCount + lessonUpdatedCount);

            success = true;
            logExecutionResult(batchName, "SUCCESS", feeList.size(), updatedCount, startTime, logContent);

            return RepeatStatus.FINISHED;

        } catch (Exception e) {
            addLog(logContent, "========== " + batchName + " 批处理执行异常 ==========");
            addLog(logContent, "错误信息: " + e.getMessage());
            logger.error("========== {} 批处理执行异常 ==========", batchName, e);

            success = false;
            logExecutionResult(batchName, "ERROR", 0, updatedCount, startTime, logContent);

            sendEmailNotification(batchName, description, success, logContent.toString());
            throw e;
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
     */
    private void logExecutionResult(String batchName, String status, int readCount, int writeCount,
                                    long startTime, StringBuilder logContent) {
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        addLog(logContent, "========== " + batchName + " 批处理执行完成 ==========");
        addLog(logContent, "执行状态: " + status);
        addLog(logContent, "修正对象件数: " + readCount);
        addLog(logContent, "实际修正件数: " + writeCount);
        addLog(logContent, "执行时间: " + executionTime + " ms (" + (executionTime / 1000.0) + " 秒)");
        addLog(logContent, "执行结束时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        addLog(logContent, "================================================");

        logger.info("========== {} 批处理执行完成 ==========", batchName);
        logger.info("执行状态: {}", status);
        logger.info("修正对象件数: {}", readCount);
        logger.info("实际修正件数: {}", writeCount);
        logger.info("执行时间: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
        logger.info("执行结束时间: {}",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("================================================");
    }

    /**
     * 发送邮件通知
     */
    private void sendEmailNotification(String jobName, String description, boolean success, String logContent) {
        BatchMailInfo mailInfo = mailDao.selectMailInfo(jobId);
        try {
            if (emailService != null) {
                emailService.setFromEmail(mailInfo.getEmailFrom());
                emailService.setToEmails(mailInfo.getMailToDevloper());
                emailService.sendBatchNotification(jobName, description, success, logContent);

                if (!mailInfo.getEmailToUser().isEmpty()) {
                    emailService.setToEmails(mailInfo.getEmailToUser());
                    emailService.sendBatchNotification(jobName, description, success, logContent);
                }

                logger.info("邮件通知发送完成 - jobName: {}, success: {}", jobName, success);
            } else {
                logger.info("邮件服务未启用，跳过邮件发送 - jobName: {}", jobName);
            }
        } catch (Exception e) {
            logger.error("发送邮件通知时出错 - jobName: {}, error: {}", jobName, e.getMessage(), e);
        }
    }
}
