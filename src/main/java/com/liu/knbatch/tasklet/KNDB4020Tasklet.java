package com.liu.knbatch.tasklet;

import com.liu.knbatch.config.BatchMailInfo;
import com.liu.knbatch.dao.BatchMailConfigDao;
import com.liu.knbatch.dao.KNDB4020Dao;
import com.liu.knbatch.entity.CollisionResult;
import com.liu.knbatch.entity.CollisionResult.CollisionType;
import com.liu.knbatch.entity.KNDB4020Entity;
import com.liu.knbatch.service.SimpleEmailService;
import com.liu.knbatch.util.CollisionReportBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KNDB4020 自动排课与手动排课碰撞检测 业务处理任务
 * 概要：检测KNDB4010自动生成的课程与老师手动安排的课程之间是否存在时间冲突
 * 业务逻辑：
 * 1. 获取检测周期范围
 * 2. 统计手动排课记录数，若为0则直接结束
 * 3. 查询自动排课和手动排课列表
 * 4. 执行碰撞检测
 * 5. 若有碰撞，生成报告并发送邮件通知
 *
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDB4020Tasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(KNDB4020Tasklet.class);
    private static final String JOB_ID = "KNDB4020";

    @Autowired
    private KNDB4020Dao kndb4020Dao;

    @Autowired
    private BatchMailConfigDao mailDao;

    @Autowired(required = false)
    private SimpleEmailService emailService;

    @Autowired
    private CollisionReportBuilder reportBuilder;

    // 成员变量：保存检测周期（邮件模板替换用）
    private String startWeekDate;
    private String endWeekDate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB4020";
        String description = "自动排课与手动排课碰撞检测";
        boolean success = false;
        int collisionCount = 0;
        StringBuilder logContent = new StringBuilder();

        addLog(logContent, "========== " + batchName + " 批处理开始执行 ==========");
        logger.info("========== {} 批处理开始执行 ==========", batchName);

        try {
            // ====== STEP 1: 获取基准日期参数 ======
            String baseDate = (String) chunkContext.getStepContext()
                    .getJobParameters().get("baseDate");
            String jobMode = (String) chunkContext.getStepContext()
                    .getJobParameters().get("jobMode");

            // 日期格式化器
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            // 将 baseDate 字符串转为 LocalDate
            LocalDate baseLocalDate = LocalDate.parse(baseDate, formatter);

            // 获取次日（与KNDB4010保持一致）
            LocalDate nextDate = baseLocalDate.plusDays(1);
            baseDate = nextDate.format(formatter);

            addLog(logContent, "批处理参数 - 基准日期: " + baseDate + ", 执行模式: " + jobMode);
            logger.info("批处理参数 - 基准日期: {}, 执行模式: {}", baseDate, jobMode);

            // ====== STEP 2: 获取检测周期 ======
            addLog(logContent, "步骤1: 获取检测周期范围...");
            logger.info("步骤1: 获取检测周期范围...");

            // 转换日期格式
            String baseDateFormatted = baseDate.replaceAll("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3");
            Map<String, String> weekRange = kndb4020Dao.getWeekRange(baseDateFormatted);

            if (weekRange == null || weekRange.isEmpty()) {
                addLog(logContent, "未找到周期范围记录，批处理结束");
                logger.warn("未找到周期范围记录: baseDate={}", baseDate);
                success = true;
                logExecutionResult(batchName, "SUCCESS", 0, 0, startTime, logContent);
                return RepeatStatus.FINISHED;
            }

            String startDate = weekRange.get("startDate");
            String endDate = weekRange.get("endDate");

            // 保存到成员变量（邮件模板用）
            this.startWeekDate = startDate;
            this.endWeekDate = endDate;

            addLog(logContent, "检测周期: " + startDate + " ~ " + endDate);
            logger.info("检测周期: {} ~ {}", startDate, endDate);

            // ====== STEP 3: 查询手动排课记录数 ======
            addLog(logContent, "步骤2: 查询手动排课记录数...");
            logger.info("步骤2: 查询手动排课记录数...");

            int manualCount = kndb4020Dao.countManualLessons(startDate, endDate);
            addLog(logContent, "手动排课记录数: " + manualCount);
            logger.info("手动排课记录数: {}", manualCount);

            // 无手动排课，直接结束
            if (manualCount == 0) {
                addLog(logContent, "无手动排课记录，无碰撞风险，跳过检测");
                logger.info("无手动排课记录，无碰撞风险，跳过检测");
                success = true;
                logExecutionResult(batchName, "SUCCESS", 0, 0, startTime, logContent);
                return RepeatStatus.FINISHED;
            }

            // ====== STEP 4: 查询自动排课列表 ======
            addLog(logContent, "步骤3: 查询自动排课列表...");
            logger.info("步骤3: 查询自动排课列表...");

            List<KNDB4020Entity> autoLessons = kndb4020Dao.getAutoLessons(startDate, endDate);
            addLog(logContent, "自动排课记录数: " + autoLessons.size());
            logger.info("自动排课记录数: {}", autoLessons.size());

            // ====== STEP 5: 查询手动排课列表 ======
            addLog(logContent, "步骤4: 查询手动排课列表...");
            logger.info("步骤4: 查询手动排课列表...");

            List<KNDB4020Entity> manualLessons = kndb4020Dao.getManualLessons(startDate, endDate);

            // ====== STEP 6: 执行碰撞检测 ======
            addLog(logContent, "步骤5: 执行碰撞检测...");
            logger.info("步骤5: 执行碰撞检测...");

            List<CollisionResult> collisions = detectCollisions(autoLessons, manualLessons);
            collisionCount = collisions.size();

            addLog(logContent, "检测到碰撞数: " + collisionCount);
            logger.info("检测到碰撞数: {}", collisionCount);

            // ====== STEP 7: 处理检测结果 ======
            String collisionReport = null;
            if (collisions.isEmpty()) {
                addLog(logContent, "无碰撞，正常结束");
                logger.info("无碰撞，正常结束");
            } else {
                addLog(logContent, "步骤6: 生成碰撞报告...");
                logger.info("步骤6: 生成碰撞报告...");

                // 生成碰撞报告
                collisionReport = reportBuilder.buildReport(collisions, startDate, endDate);

                // 记录碰撞详情到日志
                for (int i = 0; i < collisions.size(); i++) {
                    CollisionResult c = collisions.get(i);
                    addLog(logContent, String.format("  碰撞 #%d: %s %s vs %s %s - %s",
                            i + 1,
                            c.getAutoLesson().getStudentName(),
                            c.getAutoLesson().getSubjectName(),
                            c.getManualLesson().getStudentName(),
                            c.getManualLesson().getSubjectName(),
                            c.getCollisionType().getDescription()));
                }
            }

            success = true;
            logExecutionResult(batchName, "SUCCESS", autoLessons.size() + manualLessons.size(), collisionCount, startTime, logContent);

            // 发送邮件（在finally之前，因为这里可以区分是否有碰撞报告）
            sendEmailNotification(batchName, description, success, logContent.toString(), collisionReport);

        } catch (Exception e) {
            addLog(logContent, "========== " + batchName + " 批处理执行异常 ==========");
            addLog(logContent, "错误信息: " + e.getMessage());
            logger.error("========== {} 批处理执行异常 ==========", batchName, e);

            success = false;
            logExecutionResult(batchName, "ERROR", 0, 0, startTime, logContent);

            // 发送错误邮件
            sendEmailNotification(batchName, description, success, logContent.toString(), null);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * 执行碰撞检测
     *
     * @param autoLessons   自动排课列表
     * @param manualLessons 手动排课列表
     * @return 碰撞结果列表
     */
    private List<CollisionResult> detectCollisions(List<KNDB4020Entity> autoLessons,
                                                    List<KNDB4020Entity> manualLessons) {
        List<CollisionResult> collisions = new ArrayList<>();

        for (KNDB4020Entity auto : autoLessons) {
            for (KNDB4020Entity manual : manualLessons) {
                // 检测碰撞
                CollisionResult result = checkCollision(auto, manual);
                if (result != null) {
                    collisions.add(result);
                }
            }
        }

        return collisions;
    }

    /**
     * 检测两节课是否碰撞
     *
     * @param auto   自动排课
     * @param manual 手动排课
     * @return 碰撞结果，若无碰撞返回null
     */
    private CollisionResult checkCollision(KNDB4020Entity auto, KNDB4020Entity manual) {
        // 获取时间信息
        LocalDateTime autoStart = auto.getSchedualDateTime();
        LocalDateTime autoEnd = auto.getEndDateTime();
        LocalDateTime manualStart = manual.getSchedualDateTime();
        LocalDateTime manualEnd = manual.getEndDateTime();

        if (autoStart == null || manualStart == null) {
            return null;
        }

        // 判断日期是否相同
        if (!autoStart.toLocalDate().equals(manualStart.toLocalDate())) {
            return null; // 不同日期，无碰撞
        }

        // ====== STEP 1: 判断是否部分重叠 ======
        if (isPartialOverlap(autoStart, autoEnd, manualStart, manualEnd)) {
            int overlapMinutes = calculateOverlapMinutes(autoStart, autoEnd, manualStart, manualEnd);
            return CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(manual)
                    .collisionType(CollisionType.PARTIAL_OVERLAP)
                    .overlapMinutes(overlapMinutes)
                    .build();
        }

        // ====== STEP 2: 判断开始时刻是否相同 ======
        if (!autoStart.toLocalTime().equals(manualStart.toLocalTime())) {
            return null; // 开始时刻不同且无部分重叠，无碰撞
        }

        // ====== STEP 3: 判断课时是否相同 ======
        if (!auto.getClassDuration().equals(manual.getClassDuration())) {
            int overlapMinutes = Math.min(auto.getClassDuration(), manual.getClassDuration());
            return CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(manual)
                    .collisionType(CollisionType.DURATION_DIFFERENT)
                    .overlapMinutes(overlapMinutes)
                    .build();
        }

        // ====== STEP 4: 判断科目是否相同 ======
        if (!auto.getSubjectId().equals(manual.getSubjectId())) {
            return CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(manual)
                    .collisionType(CollisionType.SUBJECT_DIFFERENT)
                    .overlapMinutes(auto.getClassDuration())
                    .build();
        }

        // ====== 忽略：集体上课场景 ======
        // 同时间 + 同课时 + 同科目 = 集体上课，不视为碰撞
        return null;
    }

    /**
     * 判断是否部分重叠（有交集但不完全相同）
     */
    private boolean isPartialOverlap(LocalDateTime start1, LocalDateTime end1,
                                     LocalDateTime start2, LocalDateTime end2) {
        // 判断是否有交集
        boolean hasOverlap = start1.isBefore(end2) && start2.isBefore(end1);

        if (!hasOverlap) {
            return false;
        }

        // 判断是否开始时刻相同
        boolean sameStart = start1.toLocalTime().equals(start2.toLocalTime());

        // 开始时刻相同不算部分重叠（走完全覆盖逻辑）
        return !sameStart;
    }

    /**
     * 计算重叠时长（分钟）
     */
    private int calculateOverlapMinutes(LocalDateTime start1, LocalDateTime end1,
                                        LocalDateTime start2, LocalDateTime end2) {
        LocalDateTime overlapStart = start1.isAfter(start2) ? start1 : start2;
        LocalDateTime overlapEnd = end1.isBefore(end2) ? end1 : end2;

        return (int) Duration.between(overlapStart, overlapEnd).toMinutes();
    }

    /**
     * 发送邮件通知
     * 参考KNDB4010Tasklet的sendEmailNotification方法
     */
    private void sendEmailNotification(String jobName, String description,
                                       boolean success, String logContent,
                                       String collisionReport) {
        // 从数据库邮件管理表提取邮件配置
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
                emailService.sendBatchNotification(jobName, description, success, logContent);

                // 2. 如果有碰撞且用户邮件不为空，给用户（钢琴老师）发送碰撞报告
                if (collisionReport != null && mailInfo.getEmailToUser() != null
                        && !mailInfo.getEmailToUser().isEmpty()) {
                    emailService.setFromEmail(mailInfo.getEmailFrom());
                    emailService.setToEmails(mailInfo.getEmailToUser());

                    // 替换邮件模板中的日期占位符
                    String mailContent = mailInfo.getMailContentForUser();
                    if (mailContent != null) {
                        mailContent = mailContent
                                .replace("FROMDATE", this.startWeekDate != null ? this.startWeekDate : "")
                                .replace("TODATE", this.endWeekDate != null ? this.endWeekDate : "");

                        // 追加碰撞报告详情
                        mailContent = mailContent + "\n\n" + collisionReport;
                    } else {
                        mailContent = collisionReport;
                    }

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
    private void logExecutionResult(String batchName, String status, int processedCount,
                                    int collisionCount, long startTime, StringBuilder logContent) {
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        addLog(logContent, "========== " + batchName + " 批处理执行完成 ==========");
        addLog(logContent, "批处理名称: " + batchName);
        addLog(logContent, "执行状态: " + status);
        addLog(logContent, "处理数据条数: " + processedCount);
        addLog(logContent, "碰撞数量: " + collisionCount);
        addLog(logContent, "执行时间: " + executionTime + " ms (" + (executionTime / 1000.0) + " 秒)");
        addLog(logContent, "执行结束时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        addLog(logContent, "================================================");

        logger.info("========== {} 批处理执行完成 ==========", batchName);
        logger.info("批处理名称: {}", batchName);
        logger.info("执行状态: {}", status);
        logger.info("处理数据条数: {}", processedCount);
        logger.info("碰撞数量: {}", collisionCount);
        logger.info("执行时间: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
        logger.info("执行结束时间: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("================================================");
    }
}
