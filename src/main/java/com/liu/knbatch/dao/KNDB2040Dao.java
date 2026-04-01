package com.liu.knbatch.dao;

import com.liu.knbatch.entity.KNDB2040Entity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * KNDB2040 学生指定月课费金额修正 Mapper 接口
 *
 * @author Liu
 * @version 1.0.0
 */
@Mapper
public interface KNDB2040Dao {

    /** 取在课学生列表（del_flg=0） */
    List<Map<String, String>> getActiveStudentList();

    /** 取学生正在学习的科目列表 */
    List<Map<String, String>> getSubjectListByStuId(@Param("stuId") String stuId);

    /** 取学生当前子科目信息（pay_style / minutes_per_lsn / year_lsn_cnt 等） */
    KNDB2040Entity getCurrentSubjectInfo(
            @Param("stuId") String stuId,
            @Param("subjectId") String subjectId);

    /** 取升级候补子科目列表（数值比较，剔除 ≤ 当前子科目） */
    List<Map<String, Object>> getCandidateSubEdaList(
            @Param("subjectId") String subjectId,
            @Param("currentSubjectSubId") String currentSubjectSubId);

    /** 取枝番标准价格 */
    Map<String, Object> getSubjectPrice(
            @Param("subjectId") String subjectId,
            @Param("subjectSubId") String subjectSubId);

    /** INSERT 学生档案新记录（操作1） */
    void insertStudentDocument(@Param("doc") KNDB2040Entity doc);

    /** 取需要修正的课费记录列表（操作2 用） */
    List<KNDB2040Entity> getLsnFeeListToFix(
            @Param("stuId") String stuId,
            @Param("subjectId") String subjectId,
            @Param("startYearMonth") String startYearMonth);

    /** UPDATE 课费金额（操作2） */
    void updateLsnFee(
            @Param("lsnFeeId") String lsnFeeId,
            @Param("lessonId") String lessonId,
            @Param("newFee") double newFee);

    /** UPDATE 课程表子科目ID（操作3） */
    void updateLessonSubjectSubId(
            @Param("lessonId") String lessonId,
            @Param("newSubjectSubId") String newSubjectSubId);
}
