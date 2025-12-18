package com.liu.knbatch.entity;

import java.math.BigDecimal;

/**
 * 月度收入报告汇总实体类
 * 用于存储每月的应收、已支付、未支付课费数据验证结果
 *
 * @author Liu
 * @version 1.0.0
 */
public class KNDB2020MonthSummaryEntity {

    private String lsnMonth;                    // 月份（YYYY-MM格式）
    private BigDecimal shouldPayLsnFee;         // 应收课费
    private BigDecimal hasPaidLsnFee;           // 已支付课费
    private BigDecimal unpaidLsnFee;            // 未支付课费
    private BigDecimal totalPaidAndUnpaid;      // 已支付+未支付合计
    private BigDecimal difference;              // 差额（应收 - 合计）
    private String verification;                // 验证结果（✓或✗）

    public KNDB2020MonthSummaryEntity() {}

    public KNDB2020MonthSummaryEntity(String lsnMonth, BigDecimal shouldPayLsnFee,
                                      BigDecimal hasPaidLsnFee, BigDecimal unpaidLsnFee,
                                      BigDecimal totalPaidAndUnpaid, BigDecimal difference,
                                      String verification) {
        this.lsnMonth = lsnMonth;
        this.shouldPayLsnFee = shouldPayLsnFee;
        this.hasPaidLsnFee = hasPaidLsnFee;
        this.unpaidLsnFee = unpaidLsnFee;
        this.totalPaidAndUnpaid = totalPaidAndUnpaid;
        this.difference = difference;
        this.verification = verification;
    }

    public String getLsnMonth() {
        return lsnMonth;
    }

    public void setLsnMonth(String lsnMonth) {
        this.lsnMonth = lsnMonth;
    }

    public BigDecimal getShouldPayLsnFee() {
        return shouldPayLsnFee;
    }

    public void setShouldPayLsnFee(BigDecimal shouldPayLsnFee) {
        this.shouldPayLsnFee = shouldPayLsnFee;
    }

    public BigDecimal getHasPaidLsnFee() {
        return hasPaidLsnFee;
    }

    public void setHasPaidLsnFee(BigDecimal hasPaidLsnFee) {
        this.hasPaidLsnFee = hasPaidLsnFee;
    }

    public BigDecimal getUnpaidLsnFee() {
        return unpaidLsnFee;
    }

    public void setUnpaidLsnFee(BigDecimal unpaidLsnFee) {
        this.unpaidLsnFee = unpaidLsnFee;
    }

    public BigDecimal getTotalPaidAndUnpaid() {
        return totalPaidAndUnpaid;
    }

    public void setTotalPaidAndUnpaid(BigDecimal totalPaidAndUnpaid) {
        this.totalPaidAndUnpaid = totalPaidAndUnpaid;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }

    public String getVerification() {
        return verification;
    }

    public void setVerification(String verification) {
        this.verification = verification;
    }
}
