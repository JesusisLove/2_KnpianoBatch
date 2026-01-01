package com.liu.knbatch.entity;

/**
 * 课程信息实体类
 * 
 * @author Liu
 * @version 1.0.0
 */
public class KNDB4010Entity {
    
    private Integer     weekNumber;
    private String      startWeekDate;
    private String        endWeekDate;
    private Integer     fixedStatus;
    
    // 构造函数
    public KNDB4010Entity() {}
    
    public KNDB4010Entity(Integer weekNumber, String startWeekDate, String endWeekDate, Integer fixedStatus) {
        this.weekNumber = weekNumber;
        this.startWeekDate = startWeekDate;
        this.endWeekDate = endWeekDate;
        this.fixedStatus = fixedStatus;
    }
    
    // Getter和Setter方法
    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public String getStartWeekDate() {
        return startWeekDate;
    }

    public void setStartWeekDate(String startWeekDate) {
        this.startWeekDate = startWeekDate;
    }

    public String getEndWeekDate() {
        return endWeekDate;
    }

    public void setEndWeekDate(String endWeekDate) {
        this.endWeekDate = endWeekDate;
    }

    public Integer getFixedStatus() {
        return fixedStatus;
    }

    public void setFixedStatus(Integer fixedStatus) {
        this.fixedStatus = fixedStatus;
    }

    @Override
    public String toString() {
        return "KNDB4010Entity{" +
                "weekNumber='" + weekNumber + '\'' +
                ", startWeekDate='" + startWeekDate + '\'' +
                ", endWeekDate='" + endWeekDate + '\'' +
                ", fixedStatus='" + fixedStatus + '\'' +
                '}';
    }
}