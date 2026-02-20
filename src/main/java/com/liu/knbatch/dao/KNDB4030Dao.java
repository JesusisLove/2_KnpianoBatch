package com.liu.knbatch.dao;

import com.liu.knbatch.entity.KNDB4030Entity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * KNDB4030 零碎課検出 Mapper接口
 * 対応XML文件：KNDB4030Mapper.xml
 *
 * 碎片課只存在于加課（lesson_type=2）中。
 * 数据来源視図 v_info_all_extra_lsns 已限定 lesson_type=2。
 *
 * @author Liu
 * @version 1.0.0
 */
@Mapper
public interface KNDB4030Dao {

    /**
     * 查詢碎片課列表（碎片課只存在于加課 lesson_type=2 中）
     * 数据来源: v_info_all_extra_lsns（視図已限定 lesson_type=2）
     * 条件: class_duration < minutes_per_lsn AND pay_flg = 0
     * 排序: stu_id, subject_id, scanqr_date ASC
     *
     * @param year 年份 (yyyy)
     * @return 碎片課列表（僅包含加課類型的碎片記録）
     */
    List<KNDB4030Entity> getFragmentLessons(@Param("year") String year);
}
