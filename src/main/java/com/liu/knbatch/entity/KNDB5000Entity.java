package com.liu.knbatch.entity;

import java.time.LocalDateTime;

/**
 * KNDB5000 数据库备份实体类
 * 
 * @author Liu
 * @version 1.0.0
 */
public class KNDB5000Entity {
    
    private String backupFileName;      // 备份文件名
    private String backupFilePath;      // 备份文件完整路径
    private Long backupFileSize;        // 备份文件大小（字节）
    private LocalDateTime backupStartTime;  // 备份开始时间
    private LocalDateTime backupEndTime;    // 备份结束时间
    private String backupStatus;       // 备份状态：SUCCESS/FAILED
    private String errorMessage;       // 错误信息
    
    // 构造函数
    public KNDB5000Entity() {}
    
    public KNDB5000Entity(String backupFileName, String backupFilePath) {
        this.backupFileName = backupFileName;
        this.backupFilePath = backupFilePath;
        this.backupStartTime = LocalDateTime.now();
        this.backupStatus = "PROCESSING";
    }
    
    // Getter and Setter methods
    public String getBackupFileName() {
        return backupFileName;
    }
    
    public void setBackupFileName(String backupFileName) {
        this.backupFileName = backupFileName;
    }
    
    public String getBackupFilePath() {
        return backupFilePath;
    }
    
    public void setBackupFilePath(String backupFilePath) {
        this.backupFilePath = backupFilePath;
    }
    
    public Long getBackupFileSize() {
        return backupFileSize;
    }
    
    public void setBackupFileSize(Long backupFileSize) {
        this.backupFileSize = backupFileSize;
    }
    
    public LocalDateTime getBackupStartTime() {
        return backupStartTime;
    }
    
    public void setBackupStartTime(LocalDateTime backupStartTime) {
        this.backupStartTime = backupStartTime;
    }
    
    public LocalDateTime getBackupEndTime() {
        return backupEndTime;
    }
    
    public void setBackupEndTime(LocalDateTime backupEndTime) {
        this.backupEndTime = backupEndTime;
    }
    
    public String getBackupStatus() {
        return backupStatus;
    }
    
    public void setBackupStatus(String backupStatus) {
        this.backupStatus = backupStatus;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}