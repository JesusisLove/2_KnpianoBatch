package com.liu.knbatch.tasklet;

import com.liu.knbatch.dao.KNDB1010Dao;
import com.liu.knbatch.entity.KNDB1010Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB1010";
        
        logger.info("========== {} 批处理开始执行 ==========", batchName);
        
        try {
            // 获取作业参数
            String baseDate = (String) chunkContext.getStepContext()
                    .getJobParameters().get("baseDate");
            String jobMode = (String) chunkContext.getStepContext()
                    .getJobParameters().get("jobMode");
            
            logger.info("批处理参数 - 基准日期: {}, 执行模式: {}", baseDate, jobMode);
            
            // 转换日期格式 yyyyMMdd -> yyyy-MM
            LocalDate date = LocalDate.parse(baseDate, DATE_FORMATTER);
            String targetMonth = date.format(MONTH_FORMATTER);
            
            logger.info("目标处理月份: {}", targetMonth);
            
            // 步骤1: 获取排课钢琴错误级别的课程记录
            logger.info("步骤1: 开始获取排课钢琴错误级别的课程记录...");
            List<KNDB1010Entity> incorrectLessons = kndb1010Dao.selectIncorrectPianoLevelLessons(targetMonth);
            int incorrectCount = incorrectLessons.size();
            
            logger.info("步骤1: 完成 - 发现错误级别课程记录数: {}", incorrectCount);
            
            if (incorrectCount == 0) {
                logger.info("未发现错误的钢琴级别课程记录，批处理正常结束");
                logExecutionResult(batchName, "SUCCESS", 0, 0, startTime);
                return RepeatStatus.FINISHED;
            }
            
            // 记录错误课程的详细信息
            if (logger.isDebugEnabled()) {
                logger.debug("错误课程记录详情:");
                for (int i = 0; i < Math.min(incorrectLessons.size(), 10); i++) { // 最多显示10条
                    KNDB1010Entity lesson = incorrectLessons.get(i);
                    logger.debug("  - 学生ID: {}, 科目ID: {}, 当前级别: {}, 排课日期: {}", 
                            lesson.getStuId(), lesson.getSubjectId(), 
                            lesson.getSubjectSubId(), lesson.getSchedualDate());
                }
                if (incorrectLessons.size() > 10) {
                    logger.debug("  ... 还有 {} 条记录", incorrectLessons.size() - 10);
                }
            }
            
            // 步骤2: 执行数据矫正操作
            logger.info("步骤2: 开始执行钢琴级别课程数据矫正...");
            int updatedCount = kndb1010Dao.updateIncorrectPianoLevelLessons(targetMonth);
            logger.info("步骤2: 完成 - 成功矫正课程记录数: {}", updatedCount);
            
            // 验证矫正结果
            logger.info("验证: 开始验证矫正结果...");
            int remainingIncorrectCount = kndb1010Dao.countIncorrectPianoLevelLessons(targetMonth);
            
            if (remainingIncorrectCount == 0) {
                logger.info("验证: 成功 - 所有错误级别课程已完成矫正");
                logExecutionResult(batchName, "SUCCESS", incorrectCount, updatedCount, startTime);
            } else {
                logger.warn("验证: 警告 - 仍有 {} 条错误级别课程未能矫正", remainingIncorrectCount);
                logExecutionResult(batchName, "WARNING", incorrectCount, updatedCount, startTime);
            }
            
            // 更新贡献统计
            contribution.incrementReadCount();
            contribution.incrementWriteCount(updatedCount);
            
            return RepeatStatus.FINISHED;
            
        } catch (Exception e) {
            logger.error("========== {} 批处理执行异常 ==========", batchName, e);
            logExecutionResult(batchName, "ERROR", 0, 0, startTime);
            throw e;
        }
    }
    
    /**
     * 记录执行结果日志
     * 
     * @param batchName 批处理名称
     * @param status 执行状态
     * @param readCount 读取记录数
     * @param writeCount 写入记录数
     * @param startTime 开始时间
     */
    private void logExecutionResult(String batchName, String status, int readCount, int writeCount, long startTime) {
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        logger.info("========== {} 批处理执行完成 ==========", batchName);
        logger.info("批处理名称: {}", batchName);
        logger.info("执行状态: {}", status);
        logger.info("处理数据条数: {}", readCount);
        logger.info("更新数据条数: {}", writeCount);
        logger.info("执行时间: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
        logger.info("执行结束时间: {}", java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("================================================");
    }
}