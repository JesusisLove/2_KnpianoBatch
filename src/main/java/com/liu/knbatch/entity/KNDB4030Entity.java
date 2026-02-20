package com.liu.knbatch.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * KNDB4030 碎片課数据实体類
 * 碎片課只存在于加課（lesson_type=2）中，正課不存在碎片課
 * 用于存儲碎片加課信息，供合併検出和郵件報告使用
 *
 * @author Liu
 * @version 1.0.0
 */
public class KNDB4030Entity {

    /** 課程ID */
    private String lessonId;

    /** 学生ID */
    private String stuId;

    /** 学生姓名 */
    private String stuName;

    /** 科目ID */
    private String subjectId;

    /** 科目名稱 */
    private String subjectName;

    /** 子科目名稱 */
    private String subjectSubName;

    /** 碎片課時長（分鐘） */
    private Integer classDuration;

    /** 標準課時（分鐘） */
    private Integer minutesPerLsn;

    /** 簽到日期 */
    private LocalDateTime scanqrDate;

    /** 排課日期 */
    private LocalDateTime schedualDate;

    // ========== Getter/Setter ==========

    public String getLessonId() {
        return lessonId;
    }

    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }

    public String getStuId() {
        return stuId;
    }

    public void setStuId(String stuId) {
        this.stuId = stuId;
    }

    public String getStuName() {
        return stuName;
    }

    public void setStuName(String stuName) {
        this.stuName = stuName;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectSubName() {
        return subjectSubName;
    }

    public void setSubjectSubName(String subjectSubName) {
        this.subjectSubName = subjectSubName;
    }

    public Integer getClassDuration() {
        return classDuration;
    }

    public void setClassDuration(Integer classDuration) {
        this.classDuration = classDuration;
    }

    public Integer getMinutesPerLsn() {
        return minutesPerLsn;
    }

    public void setMinutesPerLsn(Integer minutesPerLsn) {
        this.minutesPerLsn = minutesPerLsn;
    }

    public LocalDateTime getScanqrDate() {
        return scanqrDate;
    }

    public void setScanqrDate(LocalDateTime scanqrDate) {
        this.scanqrDate = scanqrDate;
    }

    public LocalDateTime getSchedualDate() {
        return schedualDate;
    }

    public void setSchedualDate(LocalDateTime schedualDate) {
        this.schedualDate = schedualDate;
    }

    // ========== 工具方法 ==========

    /**
     * 獲取簽到日期格式化字符串（MM-dd）
     */
    public String getScanqrDateShort() {
        if (scanqrDate == null) return "";
        return scanqrDate.format(DateTimeFormatter.ofPattern("MM-dd"));
    }

    /**
     * 獲取簽到日期格式化字符串（yyyy-MM-dd）
     */
    public String getScanqrDateFull() {
        if (scanqrDate == null) return "";
        return scanqrDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Override
    public String toString() {
        return "KNDB4030Entity{" +
                "lessonId='" + lessonId + '\'' +
                ", stuId='" + stuId + '\'' +
                ", stuName='" + stuName + '\'' +
                ", subjectId='" + subjectId + '\'' +
                ", subjectName='" + subjectName + '\'' +
                ", classDuration=" + classDuration +
                ", minutesPerLsn=" + minutesPerLsn +
                ", scanqrDate=" + scanqrDate +
                '}';
    }
}
