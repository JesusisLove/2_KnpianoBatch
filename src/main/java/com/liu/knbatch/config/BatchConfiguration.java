package com.liu.knbatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

/**
 * Spring Batch 主配置类
 * 导入各个业务模块的批处理配置
 * 
 * @author Liu
 * @version 1.0.0
 */
@Configuration
@Import({
    KNDB1010Config.class,  // 钢琴课程级别矫正配置
    KNDB1020Config.class   // 学生信息同步配置
})
public class BatchConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);
    
    @PostConstruct
    public void init() {
        logger.info("========================================");
        logger.info("KnPiano Batch 批处理配置初始化完成");
        logger.info("已加载的批处理模块:");
        logger.info("  - KNDB1010: 钢琴课程级别矫正");
        logger.info("  - KNDB1020: 学生信息同步");
        logger.info("配置文件结构:");
        logger.info("  - BatchConfiguration.java (主配置)");
        logger.info("  - KNDB1010Config.java (钢琴课程级别矫正)");
        logger.info("  - KNDB1020Config.java (学生信息同步)");
        logger.info("========================================");
    }
}