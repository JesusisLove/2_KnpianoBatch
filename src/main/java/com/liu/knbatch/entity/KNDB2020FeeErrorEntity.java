package com.liu.knbatch.entity;

/**
 * 费用表错误记录实体类
 * 用于存储一个lesson_id对应多个lsn_fee_id的错误数据
 *
 * @author Liu
 * @version 1.0.0
 */
public class KNDB2020FeeErrorEntity {

    private String lessonId;        // 课程ID
    private Integer feeCount;       // 费用ID数量
    private String feeIds;          // 费用ID列表（逗号分隔）

    public KNDB2020FeeErrorEntity() {}

    public KNDB2020FeeErrorEntity(String lessonId, Integer feeCount, String feeIds) {
        this.lessonId = lessonId;
        this.feeCount = feeCount;
        this.feeIds = feeIds;
    }

    public String getLessonId() {
        return lessonId;
    }

    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }

    public Integer getFeeCount() {
        return feeCount;
    }

    public void setFeeCount(Integer feeCount) {
        this.feeCount = feeCount;
    }

    public String getFeeIds() {
        return feeIds;
    }

    public void setFeeIds(String feeIds) {
        this.feeIds = feeIds;
    }
}
