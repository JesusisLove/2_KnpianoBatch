package com.liu.knbatch.dao;

import com.liu.knbatch.entity.KNDB1010Entity;
import com.liu.knbatch.entity.KNDB4010Entity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * KNDB4010 钢琴课程级别矫正 数据访问接口
 * 
 * @author Liu
 * @version 1.0.0
 */
@Mapper
public interface KNDB4010Dao {
    
    /**
     * 获取一周固定排课状态表（t_fixedlesson_status）表，一周排课状态值（fixed_status 0:未排课  1:已排课）
     * 
     * @param baseDate 日期 (格式: yyyy-MM-dd)
     * @return 排课状态值
     */
    KNDB4010Entity getFixedStatusInfo(@Param("baseDate") String baseDate);
    
    /**
     * 执行一周计划排课的Batch处理
     * 
     * @param targetMonth 目标月份 (格式: yyyy-MM)
     * @return 更新的记录数
     */

    public void doLsnWeeklySchedual(@Param("weekStart")String weekStart, 
                                    @Param("weekEnd")String weekEnd, 
                                    @Param("SEQCode")String SEQCode);

    // 更新周次排课表里的排课状态
    public int updateWeeklyBatchStatus(@Param("weekStart")String weekStart, 
                                        @Param("weekEnd")String weekEnd);
}