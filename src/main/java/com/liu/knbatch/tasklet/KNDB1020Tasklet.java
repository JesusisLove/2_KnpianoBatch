package com.liu.knbatch.tasklet;

import com.liu.knbatch.dao.KNDB1020Dao;
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

/**
 * KNDB1020 学生信息同步 业务处理任务
 * 
 * 业务逻辑：
 * 1. 获取需要同步的学生信息
 * 2. 执行学生信息同步操作
 * 3. 验证同步结果
 * 
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDB1020Tasklet implements Tasklet {
    
    private static final Logger logger = LoggerFactory.getLogger(KNDB1020Tasklet.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    @Autowired
    private KNDB1020Dao kndb1020Dao;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB1020";
        
        logger.info("========== {} 批处理开始执行 ==========", batchName);
        
        try {
            // 获取作业参数
            String baseDate = (String) chunkContext.getStepContext()
                    .getJobParameters().get("baseDate");
            String jobMode = (String) chunkContext.getStepContext()
                    .getJobParameters().get("jobMode");
            String businessModule = (String) chunkContext.getStepContext()
                    .getJobParameters().get("businessModule");
            
            logger.info("批处理参数 - 基准日期: {}, 执行模式: {}, 业务模块: {}", baseDate, jobMode, businessModule);
            
            // 转换日期格式
            LocalDate date = LocalDate.parse(baseDate, DATE_FORMATTER);
            
            logger.info("处理日期: {}", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            
            // 步骤1: 获取需要同步的学生信息记录
            logger.info("步骤1: 开始获取需要同步的学生信息记录...");
            int pendingSyncCount = kndb1020Dao.selectPendingSyncStudentCount();
            
            logger.info("步骤1: 完成 - 发现待同步学生信息记录数: {}", pendingSyncCount);
            
            if (pendingSyncCount == 0) {
                logger.info("未发现需要同步的学生信息记录，批处理正常结束");
                logExecutionResult(batchName, "SUCCESS", 0, 0, startTime);
                return RepeatStatus.FINISHED;
            }
            
            // 步骤2: 执行学生信息同步操作
            logger.info("步骤2: 开始执行学生信息同步...");
            int syncedCount = kndb1020Dao.syncStudentInformation();
            logger.info("步骤2: 完成 - 成功同步学生信息记录数: {}", syncedCount);
            
            // 验证同步结果
            logger.info("验证: 开始验证同步结果...");
            int remainingCount = kndb1020Dao.selectPendingSyncStudentCount();
            
            if (remainingCount == 0) {
                logger.info("验证: 成功 - 所有学生信息已完成同步");
                logExecutionResult(batchName, "SUCCESS", pendingSyncCount, syncedCount, startTime);
            } else {
                logger.warn("验证: 警告 - 仍有 {} 条学生信息记录未能同步", remainingCount);
                logExecutionResult(batchName, "WARNING", pendingSyncCount, syncedCount, startTime);
            }
            
            // 更新贡献统计
            // contribution.incrementReadCount(pendingSyncCount);
            // contribution.incrementWriteCount(syncedCount);
            
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
        logger.info("同步数据条数: {}", writeCount);
        logger.info("执行时间: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
        logger.info("执行结束时间: {}", java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("================================================");
    }
}