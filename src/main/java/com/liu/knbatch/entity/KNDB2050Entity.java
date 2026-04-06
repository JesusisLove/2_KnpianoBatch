package com.liu.knbatch.entity;

/**
 * KNDB2050 计划课转换成加课 实体类
 *
 * @author Liu
 * @version 1.0.0
 */
public class KNDB2050Entity {

    private String lessonId;     // 课程ID
    private String lsnFeeId;     // 课费ID
    private int    lessonType;   // 课程种别（1:计划课 / 2:加课）
    private String stuId;        // 学生ID
    private String subjectId;    // 科目ID
    private String schedualDate; // 上课日期（预览画面表示用）
    private String stuName;      // 学生姓名（预览画面表示用）

    public String getLessonId() {
        return lessonId;
    }

    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }

    public String getLsnFeeId() {
        return lsnFeeId;
    }

    public void setLsnFeeId(String lsnFeeId) {
        this.lsnFeeId = lsnFeeId;
    }

    public int getLessonType() {
        return lessonType;
    }

    public void setLessonType(int lessonType) {
        this.lessonType = lessonType;
    }

    public String getStuId() {
        return stuId;
    }

    public void setStuId(String stuId) {
        this.stuId = stuId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSchedualDate() {
        return schedualDate;
    }

    public void setSchedualDate(String schedualDate) {
        this.schedualDate = schedualDate;
    }

    public String getStuName() {
        return stuName;
    }

    public void setStuName(String stuName) {
        this.stuName = stuName;
    }
}
