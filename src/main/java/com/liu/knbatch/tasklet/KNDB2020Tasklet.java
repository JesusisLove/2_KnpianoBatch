package com.liu.knbatch.tasklet;

import com.liu.knbatch.config.BatchMailInfo;
import com.liu.knbatch.dao.BatchMailConfigDao;
import com.liu.knbatch.dao.KNDB2020Dao;
import com.liu.knbatch.entity.KNDB2020MonthSummaryEntity;
import com.liu.knbatch.entity.KNDB2020ValidationSummaryEntity;
import com.liu.knbatch.entity.KNDB2020FeeErrorEntity;
import com.liu.knbatch.entity.KNDB2020PayErrorEntity;
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
 * KNDB2020 年度月收入报告数据监视 业务处理任务
 *
 * 业务逻辑：
 * 1. 验证指定年度的所有月份课费数据（应收 = 已支付 + 未支付）
 * 2. 如果发现数据不一致，检查费用表和支付表的错误记录
 * 3. 发送邮件通知包含验证结果和错误详情
 *
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDB2020Tasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(KNDB2020Tasklet.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private String jobId = "KNDB2020";

    @Autowired
    private KNDB2020Dao kndb2020Dao;
    @Autowired
    private BatchMailConfigDao mailDao;

    @Autowired(required = false)
    private SimpleEmailService emailService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB2020";
        String description = "年度月收入报告数据监视";
        boolean success = false;
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

            // 提取年份 yyyyMMdd -> yyyy
            LocalDate date = LocalDate.parse(baseDate, DATE_FORMATTER);
            String year = String.valueOf(date.getYear());

            addLog(logContent, "目标验证年度: " + year);
            logger.info("目标验证年度: {}", year);

            // 步骤1: 获取指定年度的整体验证结果汇总
            addLog(logContent, "步骤1: 开始获取年度汇总验证结果...");
            logger.info("步骤1: 开始获取年度汇总验证结果...");

            KNDB2020ValidationSummaryEntity validationSummary = kndb2020Dao.getValidationSummary(year);

            addLog(logContent, "步骤1: 完成 - 验证结果汇总:");
            addLog(logContent, "  总月份数: " + validationSummary.getTotalMonthCount());
            addLog(logContent, "  正确月份数: " + validationSummary.getCorrectMonthCount());
            addLog(logContent, "  错误月份数: " + validationSummary.getErrorMonthCount());
            addLog(logContent, "  最终结果: " + validationSummary.getFinalResult());

            logger.info("步骤1: 完成 - 总月份数: {}, 正确月份数: {}, 错误月份数: {}, 最终结果: {}",
                    validationSummary.getTotalMonthCount(),
                    validationSummary.getCorrectMonthCount(),
                    validationSummary.getErrorMonthCount(),
                    validationSummary.getFinalResult());

            // 步骤2: 获取月度汇总明细
            addLog(logContent, "步骤2: 开始获取月度汇总明细...");
            logger.info("步骤2: 开始获取月度汇总明细...");

            List<KNDB2020MonthSummaryEntity> monthSummaryList = kndb2020Dao.getMonthSummaryList(year);

            addLog(logContent, "步骤2: 完成 - 获取到 " + monthSummaryList.size() + " 个月份的数据");
            logger.info("步骤2: 完成 - 获取到 {} 个月份的数据", monthSummaryList.size());

            // 步骤3: 如果有错误月份，检查费用表和支付表的错误记录
            List<KNDB2020FeeErrorEntity> feeErrorList = null;
            List<KNDB2020PayErrorEntity> payErrorList = null;

            if (validationSummary.getErrorMonthCount() > 0) {
                addLog(logContent, "步骤3: 发现错误月份，开始检查费用表和支付表...");
                logger.info("步骤3: 发现错误月份，开始检查费用表和支付表...");

                // 检查费用表错误
                feeErrorList = kndb2020Dao.getFeeErrorList();
                addLog(logContent, "步骤3-1: 费用表错误记录数: " + feeErrorList.size());
                logger.info("步骤3-1: 费用表错误记录数: {}", feeErrorList.size());

                // 检查支付表错误
                payErrorList = kndb2020Dao.getPayErrorList();
                addLog(logContent, "步骤3-2: 支付表错误记录数: " + payErrorList.size());
                logger.info("步骤3-2: 支付表错误记录数: {}", payErrorList.size());
            } else {
                addLog(logContent, "步骤3: 所有月份数据正确，无需检查错误表");
                logger.info("步骤3: 所有月份数据正确，无需检查错误表");
            }

            // 更新贡献统计
            contribution.incrementReadCount();

            success = true;
            logExecutionResult(batchName, "SUCCESS", validationSummary, startTime, logContent);

            // 发送邮件通知（无论是否有错误都发送）
            sendEmailNotification(batchName, description, success, logContent.toString(),
                    validationSummary, monthSummaryList, feeErrorList, payErrorList);

            return RepeatStatus.FINISHED;

        } catch (Exception e) {
            addLog(logContent, "========== " + batchName + " 批处理执行异常 ==========");
            addLog(logContent, "错误信息: " + e.getMessage());
            logger.error("========== {} 批处理执行异常 ==========", batchName, e);

            success = false;
            addLog(logContent, "执行状态: ERROR");
            addLog(logContent, "执行时间: " + (System.currentTimeMillis() - startTime) + " ms");
            addLog(logContent, "================================================");

            // 异常情况下也发送邮件通知
            sendEmailNotification(batchName, description, success, logContent.toString(),
                    null, null, null, null);
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
    private void logExecutionResult(String batchName, String status,
            KNDB2020ValidationSummaryEntity validationSummary, long startTime, StringBuilder logContent) {
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        addLog(logContent, "========== " + batchName + " 批处理执行完成 ==========");
        addLog(logContent, "批处理名称: " + batchName);
        addLog(logContent, "执行状态: " + status);
        addLog(logContent, "验证结果: " + validationSummary.getFinalResult());
        addLog(logContent, "执行时间: " + executionTime + " ms (" + (executionTime / 1000.0) + " 秒)");
        addLog(logContent, "执行结束时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        addLog(logContent, "================================================");

        logger.info("========== {} 批处理执行完成 ==========", batchName);
        logger.info("批处理名称: {}", batchName);
        logger.info("执行状态: {}", status);
        logger.info("验证结果: {}", validationSummary.getFinalResult());
        logger.info("执行时间: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
        logger.info("执行结束时间: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("================================================");
    }

    /**
     * 发送邮件通知
     */
    private void sendEmailNotification(String jobName, String description, boolean success,
            String logContent, KNDB2020ValidationSummaryEntity validationSummary,
            List<KNDB2020MonthSummaryEntity> monthSummaryList,
            List<KNDB2020FeeErrorEntity> feeErrorList,
            List<KNDB2020PayErrorEntity> payErrorList) {

        // 从数据库邮件管理表提取邮件管理信息
        BatchMailInfo mailInfo = mailDao.selectMailInfo(jobId);

        try {
            if (emailService != null) {
                // 构建友好的邮件内容
                String emailContent = buildEmailContent(success, logContent, validationSummary,
                        monthSummaryList, feeErrorList, payErrorList);

                emailService.setFromEmail(mailInfo.getEmailFrom());

                // 给程序维护者发送邮件
                emailService.setToEmails(mailInfo.getMailToDevloper());
                emailService.sendBatchNotification(jobName, description, success, emailContent);

                // 如果用户邮件不为空，则给用户发送邮件
                if (!mailInfo.getEmailToUser().isEmpty()){
                    emailService.setToEmails(mailInfo.getEmailToUser());
                    // 给用户发送更简洁的内容
                    String userEmailContent = buildUserEmailContent(success, validationSummary,
                            monthSummaryList, feeErrorList, payErrorList);
                    emailService.sendBatchNotification(jobName, description, success, userEmailContent);
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

    /**
     * 构建给开发者的邮件内容
     */
    private String buildEmailContent(boolean success, String logContent,
            KNDB2020ValidationSummaryEntity validationSummary,
            List<KNDB2020MonthSummaryEntity> monthSummaryList,
            List<KNDB2020FeeErrorEntity> feeErrorList,
            List<KNDB2020PayErrorEntity> payErrorList) {

        StringBuilder content = new StringBuilder();

        content.append("========== 年度月收入报告数据监视执行报告 ==========\n\n");

        if (!success || validationSummary == null) {
            content.append("执行状态: 失败\n\n");
            content.append("详细日志:\n");
            content.append(logContent);
            return content.toString();
        }

        // 验证结果汇总
        content.append("【验证结果汇总】\n");
        content.append("  总月份数: ").append(validationSummary.getTotalMonthCount()).append("\n");
        content.append("  正确月份数: ").append(validationSummary.getCorrectMonthCount()).append("\n");
        content.append("  错误月份数: ").append(validationSummary.getErrorMonthCount()).append("\n");
        content.append("  最终结果: ").append(validationSummary.getFinalResult()).append("\n\n");

        // 如果有错误月份，显示月度明细
        if (validationSummary.getErrorMonthCount() > 0 && monthSummaryList != null) {
            content.append("【月度汇总明细】\n");
            content.append(String.format("%-10s %15s %15s %15s %10s\n",
                    "月份", "应收", "已支付", "未支付", "验证"));
            content.append("─".repeat(70)).append("\n");

            for (KNDB2020MonthSummaryEntity month : monthSummaryList) {
                content.append(String.format("%-10s %15.2f %15.2f %15.2f %10s\n",
                        month.getLsnMonth(),
                        month.getShouldPayLsnFee(),
                        month.getHasPaidLsnFee(),
                        month.getUnpaidLsnFee(),
                        month.getVerification()));
            }
            content.append("\n");
        }

        // 显示费用表错误记录
        if (feeErrorList != null && !feeErrorList.isEmpty()) {
            content.append("【费用表错误记录】一个lesson_id对应了多个lsn_fee_id【正确的业务逻辑：t_info_lesson_fee表里，一个lsn_fee_id可以对应多个lesson_id；但是，一个lesson_id只能对应一个lsn_fee_id】\n");
            content.append(String.format("%-20s %10s %s\n", "课程ID", "费用ID数", "费用ID列表"));
            content.append("─".repeat(70)).append("\n");

            for (KNDB2020FeeErrorEntity error : feeErrorList) {
                content.append(String.format("%-20s %10d %s\n",
                        error.getLessonId(),
                        error.getFeeCount(),
                        error.getFeeIds()));
            }
            content.append("\n");
        }

        // 显示支付表错误记录
        if (payErrorList != null && !payErrorList.isEmpty()) {
            content.append("【支付表错误记录】一个lsn_fee_id对应了多个lsn_pay_id【正确的业务逻辑：t_info_lesson_pay表里，一个lsn_pay_id只能对应一个lsn_fee_id，只能是1:1的关系】\n");
            content.append(String.format("%-20s %10s %s\n", "课费ID", "支付ID数", "支付ID列表"));
            content.append("─".repeat(70)).append("\n");

            for (KNDB2020PayErrorEntity error : payErrorList) {
                content.append(String.format("%-20s %10d %s\n",
                        error.getLsnFeeId(),
                        error.getPayCount(),
                        error.getPayIds()));
            }
            content.append("\n");
        }

        // 如果没有发现错误表记录但有错误月份
        if (validationSummary.getErrorMonthCount() > 0 &&
            (feeErrorList == null || feeErrorList.isEmpty()) &&
            (payErrorList == null || payErrorList.isEmpty())) {
            content.append("【警告】发现错误月份，但费用表和支付表未发现明显错误记录。\n");
            content.append("这可能表示存在新的数据问题或未发现的程序BUG，请仔细检查！\n\n");
        }

        content.append("================================================\n\n");
        content.append("详细执行日志:\n");
        content.append(logContent);

        return content.toString();
    }

    /**
     * 构建给用户的邮件内容（更简洁）
     */
    private String buildUserEmailContent(boolean success,
            KNDB2020ValidationSummaryEntity validationSummary,
            List<KNDB2020MonthSummaryEntity> monthSummaryList,
            List<KNDB2020FeeErrorEntity> feeErrorList,
            List<KNDB2020PayErrorEntity> payErrorList) {

        StringBuilder content = new StringBuilder();

        content.append("年度月收入报告数据监视结果\n\n");

        if (!success || validationSummary == null) {
            content.append("系统执行异常，请联系管理员。\n");
            return content.toString();
        }

        if (validationSummary.getErrorMonthCount() == 0) {
            content.append("验证结果: ").append(validationSummary.getFinalResult()).append("\n");
            content.append("所有月份的课费数据均正确无误。\n");
        } else {
            content.append("验证结果: ").append(validationSummary.getFinalResult()).append("\n\n");
            content.append("发现 ").append(validationSummary.getErrorMonthCount())
                    .append(" 个月份的数据存在异常。\n\n");

            if (feeErrorList != null && !feeErrorList.isEmpty()) {
                content.append("费用表错误记录数: ").append(feeErrorList.size()).append("\n");
            }

            if (payErrorList != null && !payErrorList.isEmpty()) {
                content.append("支付表错误记录数: ").append(payErrorList.size()).append("\n");
            }

            content.append("\n详细信息请联系管理员查看。\n");
        }

        return content.toString();
    }
}
