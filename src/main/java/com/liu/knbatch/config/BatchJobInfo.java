package com.liu.knbatch.config;

/**
 * 批处理作业信息实体类
 * 用于存储从batch-jobs.xml配置文件中读取的作业定义信息
 * 
 * @author liu
 * @version 1.0.0
 */
public class BatchJobInfo {
    
    private String jobId;           // 作业ID，如 KNDB1010
    private String beanName;        // Spring Bean名称，如 kndb1010Job
    private String description;     // 作业描述
    private String cronExpression;  // 定时表达式（可选）
    private String cronDescription; // 定时表达式描述（可选）
    private String targetDescription; // 处理目标描述（可选）
    private boolean enabled;        // 是否启用
    
    public BatchJobInfo() {}
    
    public BatchJobInfo(String jobId, String beanName, String description) {
        this.jobId = jobId;
        this.beanName = beanName;
        this.description = description;
        this.enabled = true;
    }
    
    public BatchJobInfo(String jobId, String beanName, String description, String cronExpression) {
        this.jobId = jobId;
        this.beanName = beanName;
        this.description = description;
        this.cronExpression = cronExpression;
        this.enabled = true;
    }
    
    // Getters and Setters
    public String getJobId() {
        return jobId;
    }
    
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    
    public String getBeanName() {
        return beanName;
    }
    
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCronExpression() {
        return cronExpression;
    }
    
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
    
    public String getCronDescription() {
        return cronDescription;
    }
    
    public void setCronDescription(String cronDescription) {
        this.cronDescription = cronDescription;
    }
    
    public String getTargetDescription() {
        return targetDescription;
    }
    
    public void setTargetDescription(String targetDescription) {
        this.targetDescription = targetDescription;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return "BatchJobInfo{" +
                "jobId='" + jobId + '\'' +
                ", beanName='" + beanName + '\'' +
                ", description='" + description + '\'' +
                ", cronExpression='" + cronExpression + '\'' +
                ", cronDescription='" + cronDescription + '\'' +
                ", targetDescription='" + targetDescription + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}