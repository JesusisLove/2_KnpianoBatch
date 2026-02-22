package com.liu.knbatch.tasklet;

import com.liu.knbatch.config.BatchMailInfo;
import com.liu.knbatch.dao.BatchMailConfigDao;
import com.liu.knbatch.dao.KNDB4030Dao;
import com.liu.knbatch.entity.KNDB4030Entity;
import com.liu.knbatch.service.SimpleEmailService;
import com.liu.knbatch.util.FragmentMergeReportBuilder;

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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * KNDB4030 零碎課補整節課郵件提醒 業務処理任務
 * 概要：毎日碎片加課（lesson_type=2）を検出し、標準課時に達する組み合わせがあれば
 *       ピアノ教師にメール通知を送信する
 *
 * 碎片課只存在于加課（lesson_type=2）中，正課不存在碎片課。
 *
 * 業務邏輯：
 * 1. 從baseDate提取年份
 * 2. 查詢所有碎片加課（class_duration < minutes_per_lsn, pay_flg=0）
 * 3. 按学生+科目分組
 * 4. 每組内按簽到時間排序，貪心算法検出可湊成整課的組合
 * 5. 若有可合併碎片課，生成報告並發送郵件通知
 *
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDB4030Tasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(KNDB4030Tasklet.class);
    private static final String JOB_ID = "KNDB4030";

    @Autowired
    private KNDB4030Dao kndb4030Dao;

    @Autowired
    private BatchMailConfigDao mailDao;

    @Autowired(required = false)
    private SimpleEmailService emailService;

    @Autowired
    private FragmentMergeReportBuilder reportBuilder;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB4030";
        String description = "零碎课补整节课邮件提醒";
        boolean success = false;
        StringBuilder logContent = new StringBuilder();

        addLog(logContent, "========== " + batchName + " 批处理开始执行 ==========");
        logger.info("========== {} 批处理开始执行 ==========", batchName);

        try {
            // ====== STEP 1: 獲取基準日期参数 ======
            String baseDate = (String) chunkContext.getStepContext()
                    .getJobParameters().get("baseDate");
            String jobMode = (String) chunkContext.getStepContext()
                    .getJobParameters().get("jobMode");

            // 從baseDate提取年份
            String year = baseDate.substring(0, 4);

            addLog(logContent, "批处理参数 - 基准日期: " + baseDate
                    + ", 年份: " + year + ", 执行模式: " + jobMode);
            logger.info("批处理参数 - 基准日期: {}, 年份: {}, 执行模式: {}",
                    baseDate, year, jobMode);

            // ====== STEP 2: 查詢所有碎片課 ======
            addLog(logContent, "步骤1: 查询碎片课列表...");
            logger.info("步骤1: 查询碎片课列表...");

            List<KNDB4030Entity> allFragments = kndb4030Dao.getFragmentLessons(year);

            addLog(logContent, "碎片课总记录数: " + allFragments.size());
            logger.info("碎片课总记录数: {}", allFragments.size());

            // ====== STEP 3: 判断是否有碎片課 ======
            if (allFragments.isEmpty()) {
                addLog(logContent, "无碎片课记录，跳过检测");
                logger.info("无碎片课记录，跳过检测");
                success = true;
                logExecutionResult(batchName, "SUCCESS", 0, 0, startTime, logContent);
                sendEmailNotification(batchName, description, success,
                        logContent.toString(), null);
                return RepeatStatus.FINISHED;
            }

            // ====== STEP 4: 按学生+科目分組 ======
            addLog(logContent, "步骤2: 按学生+科目分组...");
            logger.info("步骤2: 按学生+科目分组...");

            Map<String, List<KNDB4030Entity>> grouped = allFragments.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getStuId() + "|" + e.getSubjectId(),
                            LinkedHashMap::new,
                            Collectors.toList()));

            addLog(logContent, "分组数量: " + grouped.size());
            logger.info("分组数量: {}", grouped.size());

            // ====== STEP 5: 每组执行贪心检测 ======
            addLog(logContent, "步骤3: 执行贪心合并检测...");
            logger.info("步骤3: 执行贪心合并检测...");

            List<MergeableGroup> mergeableGroups = new ArrayList<>();

            for (Map.Entry<String, List<KNDB4030Entity>> entry : grouped.entrySet()) {
                List<KNDB4030Entity> fragments = entry.getValue();

                // 組内按簽到日期排序
                fragments.sort(Comparator.comparing(KNDB4030Entity::getScanqrDate));

                // 獲取標準課時
                int standardDuration = fragments.get(0).getMinutesPerLsn();

                // 貪心検出
                List<MergeableGroup> groups = detectMergeableGroups(
                        fragments, standardDuration);

                mergeableGroups.addAll(groups);
            }

            int totalMergeableLessons = mergeableGroups.size();
            addLog(logContent, "可合并整课数量: " + totalMergeableLessons);
            logger.info("可合并整课数量: {}", totalMergeableLessons);

            // ====== STEP 6: 判断是否有可合并的组合 ======
            String mergeReport = null;
            if (mergeableGroups.isEmpty()) {
                addLog(logContent, "无可合并的碎片课，正常结束");
                logger.info("无可合并的碎片课，正常结束");
            } else {
                // ====== STEP 7: 生成合并提醒报告 ======
                addLog(logContent, "步骤4: 生成合并提醒报告...");
                logger.info("步骤4: 生成合并提醒报告...");

                mergeReport = reportBuilder.buildReport(mergeableGroups, baseDate);

                // 记录详情到日志
                for (MergeableGroup group : mergeableGroups) {
                    addLog(logContent, String.format("  %s %s: %d个碎片 → 1节整课",
                            group.getStuName(),
                            group.getSubjectName(),
                            group.getFragments().size()));
                }
            }

            success = true;
            logExecutionResult(batchName, "SUCCESS", allFragments.size(),
                    totalMergeableLessons, startTime, logContent);

            // ====== STEP 8: 发送邮件 ======
            sendEmailNotification(batchName, description, success,
                    logContent.toString(), mergeReport);

        } catch (Exception e) {
            addLog(logContent, "========== " + batchName + " 批处理执行异常 ==========");
            addLog(logContent, "错误信息: " + e.getMessage());
            logger.error("========== {} 批处理执行异常 ==========", batchName, e);

            success = false;
            logExecutionResult(batchName, "ERROR", 0, 0, startTime, logContent);
            sendEmailNotification(batchName, description, success,
                    logContent.toString(), null);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * 貪心算法検出可合併的碎片課組合
     *
     * @param fragments        碎片課列表（已按scanqr_date排序）
     * @param standardDuration 標準課時（分鐘）
     * @return 可合併的碎片組列表
     */
    List<MergeableGroup> detectMergeableGroups(
            List<KNDB4030Entity> fragments, int standardDuration) {

        List<MergeableGroup> result = new ArrayList<>();

        int currentTotal = 0;
        List<KNDB4030Entity> currentGroup = new ArrayList<>();

        for (KNDB4030Entity fragment : fragments) {
            int newTotal = currentTotal + fragment.getClassDuration();

            if (newTotal == standardDuration) {
                // 恰好湊成整課
                currentGroup.add(fragment);

                MergeableGroup group = new MergeableGroup();
                group.setStuId(fragment.getStuId());
                group.setStuName(fragment.getStuName());
                group.setSubjectId(fragment.getSubjectId());
                group.setSubjectName(fragment.getSubjectName());
                group.setMinutesPerLsn(standardDuration);
                group.setFragments(new ArrayList<>(currentGroup));
                result.add(group);

                // 重置
                currentTotal = 0;
                currentGroup.clear();

            } else if (newTotal < standardDuration) {
                // 還不夠，繼続累加
                currentGroup.add(fragment);
                currentTotal = newTotal;

            } else {
                // 超過標準課時，放棄當前組，從該碎片重新開始
                currentTotal = fragment.getClassDuration();
                currentGroup.clear();
                currentGroup.add(fragment);
            }
        }

        return result;
    }

    /**
     * 發送郵件通知
     * 参考KNDB4020Tasklet的sendEmailNotification方法
     */
    private void sendEmailNotification(String jobName, String description,
                                       boolean success, String logContent,
                                       String mergeReport) {
        BatchMailInfo mailInfo = mailDao.selectMailInfo(JOB_ID);

        if (mailInfo == null) {
            logger.warn("未找到邮件配置: jobId={}", JOB_ID);
            return;
        }

        try {
            if (emailService != null) {
                // 1. 给程序维护者发送执行日志邮件
                emailService.setFromEmail(mailInfo.getEmailFrom());
                emailService.setToEmails(mailInfo.getMailToDevloper());
                emailService.sendBatchNotification(jobName, description,
                        success, logContent);

                // 2. 老师提醒邮件：仅当有可合并碎片课时发送（mergeReport != null）
                if (mergeReport != null && mailInfo.getEmailToUser() != null
                        && !mailInfo.getEmailToUser().isEmpty()) {
                    emailService.setFromEmail(mailInfo.getEmailFrom());
                    emailService.setToEmails(mailInfo.getEmailToUser());

                    emailService.sendBatchNotification(jobName, description,
                            success, mergeReport);
                    logger.info("老师提醒邮件已发送 - jobName: {}", jobName);
                } else {
                    logger.info("无可合并碎片课，跳过老师提醒邮件 - jobName: {}", jobName);
                }

                logger.info("邮件通知发送完成 - jobName: {}, success: {}",
                        jobName, success);
            } else {
                logger.info("邮件服务未启用，跳过邮件发送 - jobName: {}", jobName);
            }
        } catch (Exception e) {
            logger.error("发送邮件通知时出错 - jobName: {}, error: {}",
                    jobName, e.getMessage(), e);
            // 不要因为邮件发送失败而影响批处理任务的状态
        }
    }

    /**
     * 添加日誌条目（帶時間戳）
     */
    private void addLog(StringBuilder logContent, String message) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logContent.append(String.format("[%s] %s\n", timestamp, message));
    }

    /**
     * 記録執行結果日誌
     */
    private void logExecutionResult(String batchName, String status,
                                    int processedCount, int mergeableCount,
                                    long startTime, StringBuilder logContent) {
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        addLog(logContent, "========== " + batchName + " 批处理执行完成 ==========");
        addLog(logContent, "批处理名称: " + batchName);
        addLog(logContent, "执行状态: " + status);
        addLog(logContent, "碎片课总数: " + processedCount);
        addLog(logContent, "可合并整课数: " + mergeableCount);
        addLog(logContent, "执行时间: " + executionTime + " ms ("
                + (executionTime / 1000.0) + " 秒)");
        addLog(logContent, "执行结束时间: " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        addLog(logContent, "================================================");

        logger.info("========== {} 批处理执行完成 ==========", batchName);
        logger.info("执行状态: {}", status);
        logger.info("碎片课总数: {}", processedCount);
        logger.info("可合并整课数: {}", mergeableCount);
        logger.info("执行时间: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
    }

    // ========== 内部類: 可合併碎片組 ==========

    /**
     * 可合併碎片組（用于報告生成）
     */
    public static class MergeableGroup {

        /** 学生ID */
        private String stuId;

        /** 学生姓名 */
        private String stuName;

        /** 科目ID */
        private String subjectId;

        /** 科目名稱 */
        private String subjectName;

        /** 標準課時 */
        private int minutesPerLsn;

        /** 碎片課列表 */
        private List<KNDB4030Entity> fragments;

        public String getStuId() {
            return stuId;
        }

        public void setStuId(String stuId) {
            this.stuId = stuId;
        }

        public String getStuName() {
            return stuName;
        }

        public void setStuName(String stuName) {
            this.stuName = stuName;
        }

        public String getSubjectId() {
            return subjectId;
        }

        public void setSubjectId(String subjectId) {
            this.subjectId = subjectId;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public int getMinutesPerLsn() {
            return minutesPerLsn;
        }

        public void setMinutesPerLsn(int minutesPerLsn) {
            this.minutesPerLsn = minutesPerLsn;
        }

        public List<KNDB4030Entity> getFragments() {
            return fragments;
        }

        public void setFragments(List<KNDB4030Entity> fragments) {
            this.fragments = fragments;
        }
    }
}
