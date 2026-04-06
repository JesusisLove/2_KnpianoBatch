package com.liu.knbatch.tasklet;

import com.liu.knbatch.dao.KNDB2050Dao;
import com.liu.knbatch.entity.KNDB2050Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KNDB2050 计划课转换成加课 业务处理任务
 *
 * 业务逻辑：
 * 1. 取得指定学生/科目/年月的计划课列表（lessonType=1 + 已签到）
 * 2. 若无执行对象，正常结束
 * 3. 前置检查：确认课费ID是否已在支付表中存在（已结算则不能执行）
 * 4. 遍历课程记录：
 *    - 更新课程表：lessonType 1 → 2
 *    - 课费表拆分：第一条保留原课费ID，其余采番新ID后 INSERT 新记录 + DELETE 旧记录
 *
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDB2050Tasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(KNDB2050Tasklet.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String LSN_FEE_SEQ_PREFIX = "kn-fee-";

    @Autowired
    private KNDB2050Dao kndb2050Dao;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB2050";
        int updatedCount = 0;
        StringBuilder logContent = new StringBuilder();

        addLog(logContent, "========== " + batchName + " 批处理开始执行 ==========");
        logger.info("========== {} 批处理开始执行 ==========", batchName);

        try {
            // 从 JobParameters 取参数
            Map<String, Object> params = chunkContext.getStepContext().getJobParameters();
            String stuId     = (String) params.get("stuId");
            String subjectId = (String) params.get("subjectId");
            String baseDate  = (String) params.get("baseDate");

            // yyyyMMdd → yyyy-MM
            LocalDate date = LocalDate.parse(baseDate, DATE_FORMATTER);
            String yearMonth = date.format(MONTH_FORMATTER);

            addLog(logContent, "处理参数 - stuId: " + stuId
                    + ", subjectId: " + subjectId + ", 对象年月: " + yearMonth);
            logger.info("处理参数 - stuId: {}, subjectId: {}, 对象年月: {}", stuId, subjectId, yearMonth);

            // STEP 1: 取得执行对象课程列表（lessonType=1 + 已签到）
            addLog(logContent, "步骤1: 取得执行对象课程列表...");
            logger.info("步骤1: 取得执行对象课程列表...");

            List<KNDB2050Entity> targetList =
                    kndb2050Dao.getTargetLessonList(stuId, subjectId, yearMonth);

            addLog(logContent, "步骤1: 完了 - 执行对象课程件数: " + targetList.size());
            logger.info("步骤1: 完了 - 执行对象课程件数: {}", targetList.size());

            if (targetList.isEmpty()) {
                addLog(logContent, "[KNDB2050] 无执行对象，正常结束。");
                logger.info("[KNDB2050] 无执行对象，正常结束。");
                logExecutionResult(batchName, "SUCCESS", 0, 0, startTime, logContent);
                return RepeatStatus.FINISHED;
            }

            // STEP 2: 前置检查 — 确认课费是否已结算
            addLog(logContent, "步骤2: 前置检查 - 确认课费结算状态...");
            logger.info("步骤2: 前置检查 - 确认课费结算状态...");

            String srcLsnFeeId = targetList.get(0).getLsnFeeId();
            boolean isAlreadyPaid = kndb2050Dao.checkIfFeeAlreadyPaid(srcLsnFeeId);

            if (isAlreadyPaid) {
                String msg = "[KNDB2050] 该月份课费已经结算，不能执行。lsnFeeId=" + srcLsnFeeId;
                addLog(logContent, msg);
                logger.warn(msg);
                logExecutionResult(batchName, "SKIPPED", targetList.size(), 0, startTime, logContent);
                return RepeatStatus.FINISHED;
            }

            addLog(logContent, "步骤2: 完了 - 课费未结算，可以执行。lsnFeeId=" + srcLsnFeeId);
            logger.info("步骤2: 完了 - 课费未结算，可以执行。lsnFeeId={}", srcLsnFeeId);

            // STEP 3: 遍历处理
            addLog(logContent, "步骤3: 开始遍历处理课程记录...");
            logger.info("步骤3: 开始遍历处理课程记录...");

            boolean isFirst = true;
            for (KNDB2050Entity entity : targetList) {
                String lessonId = entity.getLessonId();

                // 更新课程表：lessonType 1 → 2
                kndb2050Dao.updateLessonType(lessonId);
                logger.debug("  课程种别更新完了: lessonId={}, lessonType 1→2", lessonId);

                if (isFirst) {
                    // 第一条：保留原课费记录，不做任何处理
                    addLog(logContent, "  [第1条] lessonId=" + lessonId + " - 保留原课费记录（lsnFeeId=" + srcLsnFeeId + "）");
                    logger.info("  [第1条] lessonId={} - 保留原课费记录（lsnFeeId={}）", lessonId, srcLsnFeeId);
                    isFirst = false;
                } else {
                    // 第二条以降：采番新课费ID → INSERT 新课费记录 + DELETE 旧记录
                    Map<String, Object> seqMap = new HashMap<>();
                    seqMap.put("parm_in", LSN_FEE_SEQ_PREFIX);
                    kndb2050Dao.getNextSequence(seqMap);
                    String newLsnFeeId = LSN_FEE_SEQ_PREFIX + (Integer) seqMap.get("parm_out");

                    kndb2050Dao.insertNewLsnFee(lessonId, newLsnFeeId, srcLsnFeeId);
                    kndb2050Dao.deleteOldLsnFee(lessonId, srcLsnFeeId);
                    addLog(logContent, "  lessonId=" + lessonId + " - INSERT新课费（newLsnFeeId=" + newLsnFeeId + "）+ DELETE旧课费完了");
                    logger.info("  lessonId={} - INSERT新课费（newLsnFeeId={}）+ DELETE旧课费完了", lessonId, newLsnFeeId);
                }
                updatedCount++;
            }

            addLog(logContent, "步骤3: 完了 - 更新课程件数: " + updatedCount);
            logger.info("步骤3: 完了 - 更新课程件数: {}", updatedCount);

            contribution.incrementWriteCount(updatedCount);
            logExecutionResult(batchName, "SUCCESS", targetList.size(), updatedCount, startTime, logContent);

            return RepeatStatus.FINISHED;

        } catch (Exception e) {
            addLog(logContent, "========== " + batchName + " 批处理执行异常 ==========");
            addLog(logContent, "错误信息: " + e.getMessage());
            logger.error("========== {} 批处理执行异常 ==========", batchName, e);

            logExecutionResult(batchName, "ERROR", 0, updatedCount, startTime, logContent);
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
        addLog(logContent, "执行对象件数: " + readCount);
        addLog(logContent, "实际更新件数: " + writeCount);
        addLog(logContent, "执行时间: " + executionTime + " ms (" + (executionTime / 1000.0) + " 秒)");
        addLog(logContent, "执行结束时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        addLog(logContent, "================================================");

        logger.info("========== {} 批处理执行完成 ==========", batchName);
        logger.info("执行状态: {}", status);
        logger.info("执行对象件数: {}", readCount);
        logger.info("实际更新件数: {}", writeCount);
        logger.info("执行时间: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
        logger.info("执行结束时间: {}",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("================================================");
    }
}
