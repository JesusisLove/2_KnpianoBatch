package com.liu.knbatch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@ConditionalOnProperty(prefix = "knbatch.email", name = "enabled", havingValue = "true")
public class SimpleEmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleEmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    // @Value("${knbatch.email.from:}")
    private String fromEmail;
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    // 修改：改为String类型，支持逗号分隔的多个邮箱
    // @Value("${knbatch.email.to}")
    private String toEmails;
    public void setToEmails(String toEmails) {
        this.toEmails = toEmails;
    }

    @Value("${knbatch.email.send-on-success:true}")
    private boolean sendOnSuccess;
    
    @Value("${knbatch.email.send-on-failure:true}")
    private boolean sendOnFailure;

    /**
     * 发送批处理执行结果邮件（日志内容直接在邮件正文中）
     */
    public void sendBatchNotification(String jobName, String jobDiscription, boolean success, String logContent) {
        try {
            // 检查必要配置
            if (fromEmail == null || fromEmail.trim().isEmpty() || 
                toEmails == null || toEmails.trim().isEmpty()) {
                logger.warn("邮件配置不完整，跳过邮件发送 - jobName: {}", jobName);
                return;
            }
            
            // 根据配置决定是否发送邮件
            if (!shouldSendEmail(success)) {
                logger.info("根据配置跳过邮件发送 - jobName: {}, success: {}", jobName, success);
                return;
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            
            // 处理多个收件人 - 支持逗号分隔
            String[] recipients = toEmails.split(",");
            for (int i = 0; i < recipients.length; i++) {
                recipients[i] = recipients[i].trim();
            }
            message.setTo(recipients);
            
            // 设置邮件主题
            String status = success ? "成功" : "失败";
            String subject = String.format("[KNBatch] %s 执行%s", jobName, status);
            message.setSubject(subject);
            
            // 设置邮件正文（直接包含日志内容）
            String emailContent = buildEmailContent(jobName, jobDiscription, success, logContent);
            message.setText(emailContent);
            
            // 发送邮件
            mailSender.send(message);
            logger.info("邮件发送成功 - jobName: {}, status: {}, recipients: {}", 
                       jobName, status, toEmails);
            
        } catch (Exception e) {
            logger.error("邮件发送失败 - jobName: {}, error: {}", jobName, e.getMessage(), e);
        }
    }
    
    /**
     * 构建邮件正文内容
     */
    private String buildEmailContent(String jobName, String jobDiscription, boolean success, String logContent) {
        StringBuilder content = new StringBuilder();
        
        // 邮件头部信息
        content.append("KNPiano 批处理系统执行通知\n");
        content.append("========================================\n\n");
        
        // 基本信息
        content.append("任务名称: ").append(jobName).append(" " + jobDiscription).append("\n");
        content.append("执行状态: ").append(success ? "成功" : "失败").append("\n");
        content.append("执行时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE"))).append("\n");
        content.append("系统环境: 生产环境\n\n");
        
        // 分隔线
        content.append("执行日志:\n");
        content.append("★----------------------------------------\n");
        
        // 日志内容（直接放在邮件正文中）
        if (logContent != null && !logContent.trim().isEmpty()) {
            content.append(logContent);
        } else {
            content.append("无日志内容");
        }
        
        // 邮件尾部
        content.append("\n★----------------------------------------\n");
        content.append("此邮件由 KNPiano 批处理系统自动发送\n");
        content.append("发送时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return content.toString();
    }
    
    /**
     * 根据配置决定是否应该发送邮件
     */
    private boolean shouldSendEmail(boolean success) {
        return (success && sendOnSuccess) || (!success && sendOnFailure);
    }
}