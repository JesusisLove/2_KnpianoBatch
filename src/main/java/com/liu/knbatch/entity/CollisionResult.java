package com.liu.knbatch.entity;

import java.time.LocalDate;

/**
 * 碰撞检测结果实体类
 * 存储自动排课与手动排课之间的碰撞信息
 *
 * @author Liu
 * @version 1.0.0
 */
public class CollisionResult {

    /**
     * 碰撞类型枚举
     */
    public enum CollisionType {
        /** 部分重叠 */
        PARTIAL_OVERLAP("部分重叠"),

        /** 课时不同 */
        DURATION_DIFFERENT("完全覆盖+课时不同"),

        /** 科目不同 */
        SUBJECT_DIFFERENT("完全覆盖+课时相同+科目不同");

        private final String description;

        CollisionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /** 自动排课信息 */
    private KNDB4020Entity autoLesson;

    /** 手动排课信息 */
    private KNDB4020Entity manualLesson;

    /** 碰撞类型 */
    private CollisionType collisionType;

    /** 重叠时长（分钟） */
    private Integer overlapMinutes;

    // ========== 构造方法 ==========

    public CollisionResult() {
    }

    public CollisionResult(KNDB4020Entity autoLesson, KNDB4020Entity manualLesson,
                          CollisionType collisionType, Integer overlapMinutes) {
        this.autoLesson = autoLesson;
        this.manualLesson = manualLesson;
        this.collisionType = collisionType;
        this.overlapMinutes = overlapMinutes;
    }

    // ========== Getter/Setter ==========

    public KNDB4020Entity getAutoLesson() {
        return autoLesson;
    }

    public void setAutoLesson(KNDB4020Entity autoLesson) {
        this.autoLesson = autoLesson;
    }

    public KNDB4020Entity getManualLesson() {
        return manualLesson;
    }

    public void setManualLesson(KNDB4020Entity manualLesson) {
        this.manualLesson = manualLesson;
    }

    public CollisionType getCollisionType() {
        return collisionType;
    }

    public void setCollisionType(CollisionType collisionType) {
        this.collisionType = collisionType;
    }

    public Integer getOverlapMinutes() {
        return overlapMinutes;
    }

    public void setOverlapMinutes(Integer overlapMinutes) {
        this.overlapMinutes = overlapMinutes;
    }

    // ========== 工具方法 ==========

    /**
     * 获取碰撞日期
     */
    public LocalDate getCollisionDate() {
        if (autoLesson == null || autoLesson.getSchedualDateTime() == null) {
            return null;
        }
        return autoLesson.getSchedualDateTime().toLocalDate();
    }

    /**
     * 获取碰撞原因描述
     */
    public String getCollisionReason() {
        if (collisionType == null) {
            return "未知碰撞类型";
        }

        switch (collisionType) {
            case PARTIAL_OVERLAP:
                return String.format("时间部分重叠（交集%d分钟）", overlapMinutes);
            case DURATION_DIFFERENT:
                return String.format("日期相同+时刻相同+课时不同（%d分钟 vs %d分钟）",
                        autoLesson.getClassDuration(),
                        manualLesson.getClassDuration());
            case SUBJECT_DIFFERENT:
                return String.format("日期相同+时刻相同+课时相同+科目不同（%s vs %s）",
                        autoLesson.getSubjectName(),
                        manualLesson.getSubjectName());
            default:
                return "未知碰撞类型";
        }
    }

    /**
     * Builder模式构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private KNDB4020Entity autoLesson;
        private KNDB4020Entity manualLesson;
        private CollisionType collisionType;
        private Integer overlapMinutes;

        public Builder autoLesson(KNDB4020Entity autoLesson) {
            this.autoLesson = autoLesson;
            return this;
        }

        public Builder manualLesson(KNDB4020Entity manualLesson) {
            this.manualLesson = manualLesson;
            return this;
        }

        public Builder collisionType(CollisionType collisionType) {
            this.collisionType = collisionType;
            return this;
        }

        public Builder overlapMinutes(Integer overlapMinutes) {
            this.overlapMinutes = overlapMinutes;
            return this;
        }

        public CollisionResult build() {
            return new CollisionResult(autoLesson, manualLesson, collisionType, overlapMinutes);
        }
    }

    @Override
    public String toString() {
        return "CollisionResult{" +
                "autoLesson=" + (autoLesson != null ? autoLesson.getStudentName() + " " + autoLesson.getSubjectName() : "null") +
                ", manualLesson=" + (manualLesson != null ? manualLesson.getStudentName() + " " + manualLesson.getSubjectName() : "null") +
                ", collisionType=" + collisionType +
                ", overlapMinutes=" + overlapMinutes +
                '}';
    }
}
