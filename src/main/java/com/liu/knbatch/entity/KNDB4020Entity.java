package com.liu.knbatch.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * KNDB4020 课程数据实体类
 * 用于存储自动排课和手动排课的课程信息
 *
 * @author Liu
 * @version 1.0.0
 */
public class KNDB4020Entity {

    /** 课程ID */
    private String lessonId;

    /** 学生ID */
    private String stuId;

    /** 学生姓名 */
    private String studentName;

    /** 科目ID */
    private String subjectId;

    /** 科目名称 */
    private String subjectName;

    /** 子科目名称 */
    private String subjectSubName;

    /** 排课日期时间 */
    private LocalDateTime schedualDateTime;

    /** 课时长度（分钟） */
    private Integer classDuration;

    /** 排课类型: 0=手动, 1=自动 */
    private Integer schedualType;

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

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
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

    public LocalDateTime getSchedualDateTime() {
        return schedualDateTime;
    }

    public void setSchedualDateTime(LocalDateTime schedualDateTime) {
        this.schedualDateTime = schedualDateTime;
    }

    public Integer getClassDuration() {
        return classDuration;
    }

    public void setClassDuration(Integer classDuration) {
        this.classDuration = classDuration;
    }

    public Integer getSchedualType() {
        return schedualType;
    }

    public void setSchedualType(Integer schedualType) {
        this.schedualType = schedualType;
    }

    // ========== 工具方法 ==========

    /**
     * 获取格式化的开始时间字符串 (HH:mm)
     */
    public String getTimeString() {
        if (schedualDateTime == null) {
            return "";
        }
        return schedualDateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * 获取结束时间
     */
    public LocalDateTime getEndDateTime() {
        if (schedualDateTime == null || classDuration == null) {
            return null;
        }
        return schedualDateTime.plusMinutes(classDuration);
    }

    /**
     * 获取格式化的结束时间字符串 (HH:mm)
     */
    public String getEndTimeString() {
        LocalDateTime endTime = getEndDateTime();
        if (endTime == null) {
            return "";
        }
        return endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * 获取格式化的日期字符串 (yyyy-MM-dd)
     */
    public String getDateString() {
        if (schedualDateTime == null) {
            return "";
        }
        return schedualDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Override
    public String toString() {
        return "KNDB4020Entity{" +
                "lessonId='" + lessonId + '\'' +
                ", stuId='" + stuId + '\'' +
                ", studentName='" + studentName + '\'' +
                ", subjectId='" + subjectId + '\'' +
                ", subjectName='" + subjectName + '\'' +
                ", schedualDateTime=" + schedualDateTime +
                ", classDuration=" + classDuration +
                ", schedualType=" + schedualType +
                '}';
    }
}
