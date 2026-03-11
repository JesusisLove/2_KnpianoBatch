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
     * 统计既存课记录数（有效时间在本周内，排除本周新生成的自动排课）
     * 既存课 = 手动排课 + 调课进来的课（含从其他周调课到本周的记录）
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 记录数
     */
    int countExistingLessons(@Param("startDate") String startDate,
                              @Param("endDate") String endDate);

    /**
     * 查询自动排课列表（本周新生成的，schedual_type=1 且未被调课）
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 自动排课列表
     */
    List<KNDB4020Entity> getAutoLessons(@Param("startDate") String startDate,
                                         @Param("endDate") String endDate);

    /**
     * 查询既存课列表（碰撞检测对象：手动排课 + 调课进来的课，排除本周新自动排课）
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 既存课列表
     */
    List<KNDB4020Entity> getExistingLessons(@Param("startDate") String startDate,
                                             @Param("endDate") String endDate);
}
