package com.liu.knbatch.entity;

import java.sql.Date;

/**
 * KNDB2040 学生指定月课费金额修正 实体类
 *
 * @author Liu
 * @version 1.0.0
 */
public class KNDB2040Entity {

    // --- 画面/Job 参数传递用 ---
    private String stuId;                    // 学生 ID
    private String subjectId;               // 科目 ID
    private String currentSubjectSubId;     // 当前子科目 ID
    private String currentSubjectSubName;   // 当前子科目名称
    private double currentLessonFee;        // 当前标准价格
    private double currentLessonFeeAdjusted; // 当前调整价格
    private String newSubjectSubId;         // 升级后子科目 ID
    private double newLessonFee;            // 升级后标准价格
    private double newLessonFeeAdjusted;    // 升级后调整价格
    private String startYearMonth;          // 起价年月（yyyy-MM）
    private Date adjustedDate;              // 档案 adjusted_date（起价月1日）

    // --- 从当前档案复制的字段 ---
    private int payStyle;                   // 付款方式
    private int minutesPerLsn;              // 课时分钟数
    private int yearLsnCnt;                 // 年度总课时

    // --- 课费修正用 ---
    private String lsnFeeId;                // 课费 ID
    private String lessonId;                // 课程 ID
    private int lessonType;                 // 课程种别（0=课结算 / 1=月计划课 / 2=月加课）
    private double lsnFee;                  // 当前课费金额（用于判断第5节是否为0）
    private String lsnMonth;                // 课费月份（yyyy-MM）

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

    public String getCurrentSubjectSubId() {
        return currentSubjectSubId;
    }

    public void setCurrentSubjectSubId(String currentSubjectSubId) {
        this.currentSubjectSubId = currentSubjectSubId;
    }

    public String getCurrentSubjectSubName() {
        return currentSubjectSubName;
    }

    public void setCurrentSubjectSubName(String currentSubjectSubName) {
        this.currentSubjectSubName = currentSubjectSubName;
    }

    public double getCurrentLessonFee() {
        return currentLessonFee;
    }

    public void setCurrentLessonFee(double currentLessonFee) {
        this.currentLessonFee = currentLessonFee;
    }

    public double getCurrentLessonFeeAdjusted() {
        return currentLessonFeeAdjusted;
    }

    public void setCurrentLessonFeeAdjusted(double currentLessonFeeAdjusted) {
        this.currentLessonFeeAdjusted = currentLessonFeeAdjusted;
    }

    public String getNewSubjectSubId() {
        return newSubjectSubId;
    }

    public void setNewSubjectSubId(String newSubjectSubId) {
        this.newSubjectSubId = newSubjectSubId;
    }

    public double getNewLessonFee() {
        return newLessonFee;
    }

    public void setNewLessonFee(double newLessonFee) {
        this.newLessonFee = newLessonFee;
    }

    public double getNewLessonFeeAdjusted() {
        return newLessonFeeAdjusted;
    }

    public void setNewLessonFeeAdjusted(double newLessonFeeAdjusted) {
        this.newLessonFeeAdjusted = newLessonFeeAdjusted;
    }

    public String getStartYearMonth() {
        return startYearMonth;
    }

    public void setStartYearMonth(String startYearMonth) {
        this.startYearMonth = startYearMonth;
    }

    public Date getAdjustedDate() {
        return adjustedDate;
    }

    public void setAdjustedDate(Date adjustedDate) {
        this.adjustedDate = adjustedDate;
    }

    public int getPayStyle() {
        return payStyle;
    }

    public void setPayStyle(int payStyle) {
        this.payStyle = payStyle;
    }

    public int getMinutesPerLsn() {
        return minutesPerLsn;
    }

    public void setMinutesPerLsn(int minutesPerLsn) {
        this.minutesPerLsn = minutesPerLsn;
    }

    public int getYearLsnCnt() {
        return yearLsnCnt;
    }

    public void setYearLsnCnt(int yearLsnCnt) {
        this.yearLsnCnt = yearLsnCnt;
    }

    public String getLsnFeeId() {
        return lsnFeeId;
    }

    public void setLsnFeeId(String lsnFeeId) {
        this.lsnFeeId = lsnFeeId;
    }

    public String getLessonId() {
        return lessonId;
    }

    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }

    public int getLessonType() {
        return lessonType;
    }

    public void setLessonType(int lessonType) {
        this.lessonType = lessonType;
    }

    public double getLsnFee() {
        return lsnFee;
    }

    public void setLsnFee(double lsnFee) {
        this.lsnFee = lsnFee;
    }

    public String getLsnMonth() {
        return lsnMonth;
    }

    public void setLsnMonth(String lsnMonth) {
        this.lsnMonth = lsnMonth;
    }
}
