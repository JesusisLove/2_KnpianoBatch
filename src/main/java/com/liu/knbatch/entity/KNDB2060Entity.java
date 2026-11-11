package com.liu.knbatch.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * KNDB2060 空月课费自动生成 实体类（收集结果）
 *
 * @author Liu
 * @version 1.0.0
 */
public class KNDB2060Entity {

    private String  stuId;            // 学生ID
    private String  stuName;          // 学生姓名（邮件用）
    private String  subjectId;        // 科目ID
    private String  subjectName;      // 科目名称（邮件用）
    private Integer monthsCompleted;  // 达成月份
    private String  generatedMonths;  // 已生成的月份（逗号分隔，Web 手动生成的部分，可为 null）

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

    public Integer getMonthsCompleted() {
        return monthsCompleted;
    }

    public void setMonthsCompleted(Integer monthsCompleted) {
        this.monthsCompleted = monthsCompleted;
    }

    public String getGeneratedMonths() {
        return generatedMonths;
    }

    public void setGeneratedMonths(String generatedMonths) {
        this.generatedMonths = generatedMonths;
    }

    /** 空月：达成月份的下个月 ～ 12 月 */
    public List<Integer> emptyMonths() {
        List<Integer> months = new ArrayList<>();
        for (int month = monthsCompleted + 1; month <= 12; month++) {
            months.add(month);
        }
        return months;
    }

    /** 空月（逗号分隔，例：10,11,12），登记清单表用 */
    public String emptyMonthsText() {
        StringBuilder sb = new StringBuilder();
        for (Integer month : emptyMonths()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(month);
        }
        return sb.toString();
    }

    /** 该月是否已生成（Web 画面手动生成） */
    public boolean isMonthGenerated(int month) {
        return generatedMonths != null
                && Arrays.asList(generatedMonths.split(",")).contains(String.valueOf(month));
    }

    /** 邮件、日志用的表示：学生姓名（学生ID）／科目名称 */
    public String label() {
        return stuName + "（" + stuId + "）／" + subjectName;
    }
}
