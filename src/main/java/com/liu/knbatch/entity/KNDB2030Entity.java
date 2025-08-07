package com.liu.knbatch.entity;

/**
 * 课程信息实体类
 * 
 * @author Liu
 * @version 1.0.0
 */
public class KNDB2030Entity {
    
    private String stuId;       // 学生ID
    private String subjectId;   // 科目ID
    private String lsnFeeId;    // 课费ID
    private String lsnPayId;    // 支付ID
    private String lessonId;    // 课程ID

    public KNDB2030Entity() {}

    public KNDB2030Entity(String stuId, String subjectId, String lsnFeeId, String lsnPayId, String lessonId) {
        this.stuId = stuId;
        this.subjectId = subjectId;
        this.lsnFeeId = lsnFeeId;
        this.lsnPayId = lsnPayId;
        this.lessonId = lessonId;
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


    public String getLsnFeeId() {
        return lsnFeeId;
    }


    public void setLsnFeeId(String lsnFeeId) {
        this.lsnFeeId = lsnFeeId;
    }


    public String getLsnPayId() {
        return lsnPayId;
    }


    public void setLsnPayId(String lsnPayId) {
        this.lsnPayId = lsnPayId;
    }


    public String getLessonId() {
        return lessonId;
    }


    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }
}