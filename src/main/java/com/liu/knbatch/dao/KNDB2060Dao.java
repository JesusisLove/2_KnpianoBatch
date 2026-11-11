package com.liu.knbatch.dao;

import com.liu.knbatch.entity.KNDB2060Entity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * KNDB2060 空月课费自动生成 Mapper 接口
 *
 * @author Liu
 * @version 1.0.0
 */
@Mapper
public interface KNDB2060Dao {

    /** 收集：本年度上满 43 节、且不在清单表里的学生 + 科目 */
    List<KNDB2060Entity> getNewlyReachedStudents(@Param("year") String year);

    /** 生成：调用存储过程生成指定学生、科目、月份的空月课费 */
    void callInsertTmpLessonInfo(@Param("stuId") String stuId,
                                 @Param("subjectId") String subjectId,
                                 @Param("targetDate") String targetDate);

    /** 登记清单表 */
    int insertEmptyMonthStuList(@Param("stuId") String stuId,
                                @Param("subjectId") String subjectId,
                                @Param("lsnYear") String lsnYear,
                                @Param("monthsCompleted") Integer monthsCompleted,
                                @Param("emptyMonths") String emptyMonths);
}
