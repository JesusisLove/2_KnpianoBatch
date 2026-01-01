package com.liu.knbatch.entity;

/**
 * 验证结果汇总实体类
 * 用于存储整体验证结果的统计信息
 *
 * @author Liu
 * @version 1.0.0
 */
public class KNDB2020ValidationSummaryEntity {

    private Integer totalMonthCount;        // 总月份数
    private Integer correctMonthCount;      // 正确月份数
    private Integer errorMonthCount;        // 错误月份数
    private String finalResult;             // 最终结果（✓ 全部正确！ 或 ✗ 仍有错误月份）

    public KNDB2020ValidationSummaryEntity() {}

    public KNDB2020ValidationSummaryEntity(Integer totalMonthCount, Integer correctMonthCount,
                                           Integer errorMonthCount, String finalResult) {
        this.totalMonthCount = totalMonthCount;
        this.correctMonthCount = correctMonthCount;
        this.errorMonthCount = errorMonthCount;
        this.finalResult = finalResult;
    }

    public Integer getTotalMonthCount() {
        return totalMonthCount;
    }

    public void setTotalMonthCount(Integer totalMonthCount) {
        this.totalMonthCount = totalMonthCount;
    }

    public Integer getCorrectMonthCount() {
        return correctMonthCount;
    }

    public void setCorrectMonthCount(Integer correctMonthCount) {
        this.correctMonthCount = correctMonthCount;
    }

    public Integer getErrorMonthCount() {
        return errorMonthCount;
    }

    public void setErrorMonthCount(Integer errorMonthCount) {
        this.errorMonthCount = errorMonthCount;
    }

    public String getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(String finalResult) {
        this.finalResult = finalResult;
    }
}
