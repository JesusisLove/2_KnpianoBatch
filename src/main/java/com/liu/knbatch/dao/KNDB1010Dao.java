package com.liu.knbatch.dao;

import com.liu.knbatch.entity.Lesson;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * KNDB1010 钢琴课程级别矫正 数据访问接口
 * 
 * @author Liu
 * @version 1.0.0
 */
@Mapper
public interface KNDB1010Dao {
    
    /**
     * 获取排课钢琴错误级别的课程记录
     * 
     * @param targetMonth 目标月份 (格式: yyyy-MM)
     * @return 错误级别的课程记录列表
     */
    List<Lesson> selectIncorrectPianoLevelLessons(@Param("targetMonth") String targetMonth);
    
    /**
     * 给错误的钢琴级别课程进行数据矫正（更新操作）
     * 
     * @param targetMonth 目标月份 (格式: yyyy-MM)
     * @return 更新的记录数
     */
    int updateIncorrectPianoLevelLessons(@Param("targetMonth") String targetMonth);
    
    /**
     * 验证矫正结果 - 检查是否还有错误的级别课程
     * 
     * @param targetMonth 目标月份 (格式: yyyy-MM)
     * @return 剩余错误记录数
     */
    int countIncorrectPianoLevelLessons(@Param("targetMonth") String targetMonth);
}