package com.liu.knbatch.entity;

/**
 * 支付表错误记录实体类
 * 用于存储一个lsn_fee_id对应多个lsn_pay_id的错误数据
 *
 * @author Liu
 * @version 1.0.0
 */
public class KNDB2020PayErrorEntity {

    private String lsnFeeId;        // 课费ID
    private Integer payCount;       // 支付ID数量
    private String payIds;          // 支付ID列表（逗号分隔）

    public KNDB2020PayErrorEntity() {}

    public KNDB2020PayErrorEntity(String lsnFeeId, Integer payCount, String payIds) {
        this.lsnFeeId = lsnFeeId;
        this.payCount = payCount;
        this.payIds = payIds;
    }

    public String getLsnFeeId() {
        return lsnFeeId;
    }

    public void setLsnFeeId(String lsnFeeId) {
        this.lsnFeeId = lsnFeeId;
    }

    public Integer getPayCount() {
        return payCount;
    }

    public void setPayCount(Integer payCount) {
        this.payCount = payCount;
    }

    public String getPayIds() {
        return payIds;
    }

    public void setPayIds(String payIds) {
        this.payIds = payIds;
    }
}
