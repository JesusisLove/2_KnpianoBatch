package com.liu.knbatch.tasklet;

import com.liu.knbatch.config.BatchMailInfo;
import com.liu.knbatch.dao.BatchMailConfigDao;
import com.liu.knbatch.dao.KNDB2060Dao;
import com.liu.knbatch.entity.KNDB2060Entity;
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
import java.util.ArrayList;
import java.util.List;

/**
 * KNDB2060 空月课费自动生成 业务处理任务
 *
 * 业务逻辑：
 * 1. 收集：本年度上满 43 节、且不在清单表（t_batch_empty_month_stu_list）里的按月付费学生 + 科目
 * 2. 生成：为每个学生一次性生成剩下的全部空月（达成月份的下个月 ～ 12 月）
 *    - Web 画面已手动生成的月份跳过
 *    - 全部成功的学生登记到清单表；失败的学生不登记，继续处理下一个学生
 * 3. 邮件通知处理结果
 *
 * 不加 @Transactional：存储过程 sp_insert_tmp_lesson_info 内部自己提交，
 * 已生成的空月无法在外层回滚（详见详细设计书 3.4）。
 *
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDB2060Tasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(KNDB2060Tasklet.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final String jobId = "KNDB2060";

    @Autowired
    private KNDB2060Dao kndb2060Dao;

    @Autowired
    private BatchMailConfigDao mailDao;

    @Autowired(required = false)
    private SimpleEmailService emailService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String batchName = "KNDB2060";
        String description = "空月课费自动生成";
        StringBuilder logContent = new StringBuilder();

        List<String> successList = new ArrayList<>();   // 成功名单（邮件用）
        List<String> skipList = new ArrayList<>();      // 跳过的月份（邮件用）
        List<String> failureList = new ArrayList<>();   // 失败名单（邮件用）

        addLog(logContent, "========== " + batchName + " 批处理开始执行 ==========");
        logger.info("========== {} 批处理开始执行 ==========", batchName);

        try {
            // 基准日期 → 处理年度
            String baseDate = (String) chunkContext.getStepContext()
                    .getJobParameters().get("baseDate");
            String year = String.valueOf(LocalDate.parse(baseDate, DATE_FORMATTER).getYear());
            addLog(logContent, "基准日期: " + baseDate + "，处理年度: " + year);
            logger.info("基准日期: {}，处理年度: {}", baseDate, year);

            // STEP 1: 收集（上满 43 节、且不在清单表里的学生 + 科目）
            List<KNDB2060Entity> targetList = kndb2060Dao.getNewlyReachedStudents(year);
            addLog(logContent, "步骤1: 收集完了 - 新上满的学生件数: " + targetList.size());
            logger.info("步骤1: 收集完了 - 新上满的学生件数: {}", targetList.size());

            // STEP 2: 按学生逐个生成
            for (KNDB2060Entity target : targetList) {
                List<Integer> generatedNow = new ArrayList<>();   // 本次生成的月份
                try {
                    for (Integer month : target.emptyMonths()) {
                        if (target.isMonthGenerated(month)) {
                            // Web 画面已手动生成 → 跳过
                            skipList.add(target.label() + " " + month + "月");
                            continue;
                        }
                        String targetDate = String.format("%s-%02d-01", year, month);
                        kndb2060Dao.callInsertTmpLessonInfo(
                                target.getStuId(), target.getSubjectId(), targetDate);
                        generatedNow.add(month);
                    }

                    // 全部空月处理成功 → 登记清单表
                    kndb2060Dao.insertEmptyMonthStuList(
                            target.getStuId(), target.getSubjectId(), year,
                            target.getMonthsCompleted(), target.emptyMonthsText());

                    String msg = target.label() + "  达成" + target.getMonthsCompleted()
                            + "月，生成：" + joinMonths(generatedNow);
                    successList.add(msg);
                    addLog(logContent, "  成功: " + msg);
                    logger.info("  成功: {}", msg);

                } catch (Exception e) {
                    // 该学生中止，不登记清单表，继续下一个学生
                    String msg = target.label() + "  原因：" + rootMessage(e)
                            + "，已生成：" + joinMonths(generatedNow);
                    failureList.add(msg);
                    addLog(logContent, "  失败: " + msg);
                    logger.error("  失败: {}", msg, e);
                }
            }

            addLog(logContent, "步骤2: 生成完了 - 成功: " + successList.size()
                    + " 件，失败: " + failureList.size() + " 件");
            logger.info("步骤2: 生成完了 - 成功: {} 件，失败: {} 件", successList.size(), failureList.size());

            // STEP 3: 邮件通知（有失败学生时，按失败发送）
            boolean success = failureList.isEmpty();
            addLog(logContent, "========== " + batchName + " 批处理执行完成 ==========");
            sendEmailNotification(batchName, description, success,
                    buildEmailContent(year, targetList.size(), successList, skipList, failureList,
                            logContent.toString()));

            contribution.incrementWriteCount(successList.size());
            return RepeatStatus.FINISHED;

        } catch (Exception e) {
            // 收集查询失败、数据库连接失败等：发送失败邮件后异常结束
            addLog(logContent, "========== " + batchName + " 批处理执行异常 ==========");
            addLog(logContent, "错误信息: " + rootMessage(e));
            logger.error("========== {} 批处理执行异常 ==========", batchName, e);
            sendEmailNotification(batchName, description, false, logContent.toString());
            throw e;
        }
    }

    /**
     * 组装邮件正文
     */
    private String buildEmailContent(String year, int targetCount, List<String> successList,
            List<String> skipList, List<String> failureList, String logContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("【KNDB2060 空月课费自动生成】\n");
        sb.append("处理年度：").append(year).append("\n");
        sb.append("新上满的学生：").append(targetCount).append(" 件（成功 ")
          .append(successList.size()).append(" 件，失败 ").append(failureList.size()).append(" 件）\n");

        if (!successList.isEmpty()) {
            sb.append("\n■ 生成成功\n");
            for (String line : successList) {
                sb.append("・").append(line).append("\n");
            }
        }
        if (!skipList.isEmpty()) {
            sb.append("\n■ 跳过（Web 画面已手动生成）\n");
            for (String line : skipList) {
                sb.append("・").append(line).append("\n");
            }
        }
        if (!failureList.isEmpty()) {
            sb.append("\n■ 生成失败（请到 Web 画面「空课学费按月支付」确认并手动处理）\n");
            for (String line : failureList) {
                sb.append("・").append(line).append("\n");
            }
        }

        sb.append("\n■ 执行日志\n").append(logContent);
        return sb.toString();
    }

    /**
     * 月份列表转为「10月、11月」形式，空列表返回「无」
     */
    private String joinMonths(List<Integer> months) {
        if (months.isEmpty()) {
            return "无";
        }
        StringBuilder sb = new StringBuilder();
        for (Integer month : months) {
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append(month).append("月");
        }
        return sb.toString();
    }

    /**
     * 取最底层的错误信息（存储过程的报错文字在最底层，便于邮件阅读）
     */
    private String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    /**
     * 添加日志条目（带时间戳）
     */
    private void addLog(StringBuilder logContent, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logContent.append(String.format("[%s] %s\n", timestamp, message));
    }

    /**
     * 发送邮件通知（发送失败只记录日志，不影响批处理结果）
     */
    private void sendEmailNotification(String jobName, String description, boolean success, String content) {
        try {
            if (emailService == null) {
                logger.info("邮件服务未启用，跳过邮件发送 - jobName: {}", jobName);
                return;
            }

            // 从数据库邮件管理表提取邮件管理信息
            BatchMailInfo mailInfo = mailDao.selectMailInfo(jobId);
            emailService.setFromEmail(mailInfo.getEmailFrom());

            // 给程序维护者发送邮件
            emailService.setToEmails(mailInfo.getMailToDevloper());
            emailService.sendBatchNotification(jobName, description, success, content);

            // 如果用户邮件不为空，则给用户发送邮件
            if (mailInfo.getEmailToUser() != null && !mailInfo.getEmailToUser().isEmpty()) {
                emailService.setToEmails(mailInfo.getEmailToUser());
                emailService.sendBatchNotification(jobName, description, success, content);
            }

            logger.info("邮件通知发送完成 - jobName: {}, success: {}", jobName, success);
        } catch (Exception e) {
            logger.error("发送邮件通知时出错 - jobName: {}, error: {}", jobName, e.getMessage(), e);
        }
    }
}
