package com.liu.knbatch.dao;

import com.liu.knbatch.entity.KNDB2050Entity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * KNDB2050 计划课转换成加课 Mapper 接口
 *
 * @author Liu
 * @version 1.0.0
 */
@Mapper
public interface KNDB2050Dao {

    /** 取在课学生列表（del_flg=0），画面初期表示用 */
    List<Map<String, String>> getActiveStudentList();

    /** 取学生正在学习的科目列表，画面科目下拉联动用 */
    List<Map<String, String>> getSubjectListByStuId(@Param("stuId") String stuId);

    /** 取执行对象课程列表（lessonType=1 + 已签到 + 指定学生/科目/年月），Tasklet 和预览共用 */
    List<KNDB2050Entity> getTargetLessonList(
            @Param("stuId") String stuId,
            @Param("subjectId") String subjectId,
            @Param("yearMonth") String yearMonth);

    /** 检查课费是否已结算（存在于支付表则返回 true） */
    boolean checkIfFeeAlreadyPaid(@Param("lsnFeeId") String lsnFeeId);

    /** 更新课程表：lessonType 1 → 2 */
    int updateLessonType(@Param("lessonId") String lessonId);

    /** 课费ID自动采番（调用数据库 nextval 函数） */
    void getNextSequence(Map<String, Object> map);

    /** INSERT 新课费记录（复制原记录，lesson_id 和 lsn_fee_id 替换为新值） */
    int insertNewLsnFee(
            @Param("lessonId") String lessonId,
            @Param("newLsnFeeId") String newLsnFeeId,
            @Param("srcLsnFeeId") String srcLsnFeeId);

    /** DELETE 原课费表中指定 lesson_id + lsn_fee_id 的旧记录 */
    int deleteOldLsnFee(
            @Param("lessonId") String lessonId,
            @Param("lsnFeeId") String lsnFeeId);
}
