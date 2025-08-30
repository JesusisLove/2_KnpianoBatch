package com.liu.knbatch.config;

/**
 * 批处理邮件配置信息实体类
 * 对应表：t_batch_mail_config
 */
public class BatchMailInfo {
    
    private String jobId;                    // 作业ID
    private String emailFrom;               // 发送方邮箱
    private String mailToDevloper;          // 开发者邮箱(多个邮箱用逗号分隔)
    private String emailToUser;             // 用户邮箱(多个邮箱用逗号分隔)
    private String mailContentForUser;      // 给用户的邮件内容

    // 默认构造函数
    public BatchMailInfo() {
    }

    // 带参构造函数
    public BatchMailInfo(String jobId, String emailFrom, String mailToDevloper, 
                   String emailToUser, String mailContentForUser) {
        this.jobId = jobId;
        this.emailFrom = emailFrom;
        this.mailToDevloper = mailToDevloper;
        this.emailToUser = emailToUser;
        this.mailContentForUser = mailContentForUser;
    }

    // Getter和Setter方法
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public String getMailToDevloper() {
        return mailToDevloper;
    }

    public void setMailToDevloper(String mailToDevloper) {
        this.mailToDevloper = mailToDevloper;
    }

    public String getEmailToUser() {
        return emailToUser;
    }

    public void setEmailToUser(String emailToUser) {
        this.emailToUser = emailToUser;
    }

    public String getMailContentForUser() {
        return mailContentForUser;
    }

    public void setMailContentForUser(String mailContentForUser) {
        this.mailContentForUser = mailContentForUser;
    }

    @Override
    public String toString() {
        return "BatchMailInfo{" +
                "jobId='" + jobId + '\'' +
                ", emailFrom='" + emailFrom + '\'' +
                ", mailToDevloper='" + mailToDevloper + '\'' +
                ", emailToUser='" + emailToUser + '\'' +
                ", mailContentForUser='" + mailContentForUser + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BatchMailInfo mailInfo = (BatchMailInfo) o;

        return jobId != null ? jobId.equals(mailInfo.jobId) : mailInfo.jobId == null;
    }

    @Override
    public int hashCode() {
        return jobId != null ? jobId.hashCode() : 0;
    }
}