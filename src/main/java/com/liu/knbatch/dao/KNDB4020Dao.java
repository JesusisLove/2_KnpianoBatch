package com.liu.knbatch.dao;

import com.liu.knbatch.entity.KNDB4020Entity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * KNDB4020 碰撞检测 Mapper接口
 * 对应XML文件：KNDB4020Mapper.xml
 *
 * @author Liu
 * @version 1.0.0
 */
@Mapper
public interface KNDB4020Dao {

    /**
     * 获取周期范围
     * @param baseDate 基准日期 (yyyyMMdd 或 yyyy-MM-dd)
     * @return 周期范围 {startDate, endDate}
     */
    Map<String, String> getWeekRange(@Param("baseDate") String baseDate);

    /**
     * 统计手动排课记录数
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 记录数
     */
    int countManualLessons(@Param("startDate") String startDate,
                           @Param("endDate") String endDate);

    /**
     * 查询自动排课列表
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 自动排课列表 (schedual_type = 1)
     */
    List<KNDB4020Entity> getAutoLessons(@Param("startDate") String startDate,
                                         @Param("endDate") String endDate);

    /**
     * 查询手动排课列表
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 手动排课列表 (schedual_type = 0)
     */
    List<KNDB4020Entity> getManualLessons(@Param("startDate") String startDate,
                                           @Param("endDate") String endDate);
}
