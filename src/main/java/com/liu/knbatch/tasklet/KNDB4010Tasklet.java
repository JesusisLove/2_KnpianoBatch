package com.liu.knbatch.tasklet;

import com.liu.knbatch.dao.KNDB4010Dao;
import com.liu.knbatch.entity.KNDB4010Entity;
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
 * KNDB4010 次周自动排课正 业务处理任务
 * 
 * 业务逻辑：
 * 1. 获取自动排课更新记录
 * 2. 如果对象周已经排课了，则停止处理
 * 3. 如果对象周尚未排课，则执行对象周自动排课操作
 * 
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDB4010Tasklet implements Tasklet {
    
    private static final Logger logger = LoggerFactory.getLogger(KNDB4010Tasklet.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    @Autowired
    private KNDB4010Dao kndb4010Dao;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB4010";
        
        logger.info("========== {} 批处理开始执行 ==========", batchName);
        
        try {
            // 获取作业参数
            String baseDate = (String) chunkContext.getStepContext()
                    .getJobParameters().get("baseDate");
            String jobMode = (String) chunkContext.getStepContext()
                    .getJobParameters().get("jobMode");
            
            logger.info("批处理参数 - 基准日期: {}, 执行模式: {}", baseDate, jobMode);
            
            // 步骤1: 获取排课钢琴错误级别的课程记录
            logger.info("步骤1: 查看该日期所在的星期的排课状态信息...");
            KNDB4010Entity kndb4010Entity = kndb4010Dao.getFixedStatusInfo(baseDate.replaceAll("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3"));
        
            int fixedStatus = kndb4010Entity.getFixedStatus();
            
            logger.info("步骤1: 查实该星期的排课状态是 - : {}", fixedStatus);
            
            if (fixedStatus == 1) {
                logger.info("该星期的课程，即{}至{}的课程已经排课完了.",kndb4010Entity.getStartWeekDate(), kndb4010Entity.getEndWeekDate());
                logExecutionResult(batchName, "SUCCESS", 0, 0, startTime);
                return RepeatStatus.FINISHED;
            }
            
            // 步骤2: 执行下一周的一周排课作业
            String startDate = kndb4010Entity.getStartWeekDate();
            String endDate = kndb4010Entity.getEndWeekDate();
            logger.info("步骤2: 开始执行下一周的一周排课作业...");
            kndb4010Dao.doLsnWeeklySchedual(startDate, endDate,"kn-lsn-");

            logger.info("步骤2: 下一周的一周排课作业完成");
            
            // 更新对象排课周的排课状态
            logger.info("更新: 排课状态表更新开始...");
            int cnt = kndb4010Dao.updateWeeklyBatchStatus(startDate, endDate);
            logger.info("更新: 完成，有{}条记录被更姓", cnt);
            
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