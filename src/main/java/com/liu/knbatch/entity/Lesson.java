package com.liu.knbatch.entity;

import java.util.Date;

/**
 * 课程信息实体类
 * 
 * @author Liu
 * @version 1.0.0
 */
public class Lesson {
    
    private String stuId;           // 学生ID
    private String subjectId;       // 科目ID
    private String subjectSubId;    // 科目子ID（级别）
    private String schedualDate;    // 排课日期
    private String lessonId;        // 课程ID
    private String teacherId;       // 教师ID
    private String status;          // 状态
    private Date createTime;        // 创建时间
    private Date updateTime;        // 更新时间
    
    // 构造函数
    public Lesson() {}
    
    public Lesson(String stuId, String subjectId, String subjectSubId) {
        this.stuId = stuId;
        this.subjectId = subjectId;
        this.subjectSubId = subjectSubId;
    }
    
    // Getter和Setter方法
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
    
    public String getSubjectSubId() {
        return subjectSubId;
    }
    
    public void setSubjectSubId(String subjectSubId) {
        this.subjectSubId = subjectSubId;
    }
    
    public String getSchedualDate() {
        return schedualDate;
    }
    
    public void setSchedualDate(String schedualDate) {
        this.schedualDate = schedualDate;
    }
    
    public String getLessonId() {
        return lessonId;
    }
    
    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }
    
    public String getTeacherId() {
        return teacherId;
    }
    
    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    public Date getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
    
    @Override
    public String toString() {
        return "Lesson{" +
                "stuId='" + stuId + '\'' +
                ", subjectId='" + subjectId + '\'' +
                ", subjectSubId='" + subjectSubId + '\'' +
                ", schedualDate='" + schedualDate + '\'' +
                ", lessonId='" + lessonId + '\'' +
                ", teacherId='" + teacherId + '\'' +
                ", status='" + status + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}