package com.liu.knbatch.tasklet;

import com.liu.knbatch.config.BatchMailInfo;
import com.liu.knbatch.dao.BatchMailConfigDao;
import com.liu.knbatch.dao.KNDB5000Dao;
import com.liu.knbatch.entity.KNDB5000Entity;
import com.liu.knbatch.service.SimpleEmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * KNDB5000 数据库备份 业务处理任务
 * 
 * 业务逻辑：
 * 1. 检查数据库连接状态
 * 2. 创建备份目录（如果不存在）
 * 3. 执行mysqldump命令备份数据库
 * 4. 验证备份文件完整性
 * 5. 清理过期备份文件（保留最近30天）
 * 6. 发送备份结果邮件通知
 * 
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDB5000Tasklet implements Tasklet {
    
    private static final Logger logger = LoggerFactory.getLogger(KNDB5000Tasklet.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private String jobId = "KNDB5000";

    @Autowired
    private KNDB5000Dao kndb5000Dao;
    @Autowired
    private BatchMailConfigDao mailDao;

    @Autowired(required = false)
    private SimpleEmailService emailService;
    
    // 从配置文件读取数据库连接信息
    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    
    @Value("${spring.datasource.username}")
    private String dbUsername;
    
    @Value("${spring.datasource.password}")
    private String dbPassword;
    
    // 备份路径配置
    @Value("${knbatch.backup.production.path:#{null}}")
    private String productionBackupPath;
    
    @Value("${knbatch.backup.local.path:/Users/${user.name}/SynologyDrive/knpiano-backup}")
    private String localBackupPath;
    
    // 备份保留天数
    @Value("${knbatch.backup.retention.days:30}")
    private int retentionDays;

    @Value("${knbatch.deploy.enviroment}")
    private String deployEnvironment;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDB5000";
        String description = "数据库定期备份";
        boolean success = false;
        StringBuilder logContent = new StringBuilder();
        KNDB5000Entity backupInfo = null;
        
        addLog(logContent, "========== " + batchName + " 数据库备份开始执行 ==========");
        logger.info("========== {} 数据库备份开始执行 ==========", batchName);
        
        try {
            // 获取作业参数
            String baseDate = (String) chunkContext.getStepContext()
                    .getJobParameters().get("baseDate");
            String jobMode = (String) chunkContext.getStepContext()
                    .getJobParameters().get("jobMode");
            
            addLog(logContent, "批处理参数 - 基准日期: " + baseDate + ", 执行模式: " + jobMode);
            logger.info("批处理参数 - 基准日期: {}, 执行模式: {}", baseDate, jobMode);
            
            // 步骤1: 检查数据库连接
            addLog(logContent, "步骤1: 检查数据库连接状态...");
            logger.info("步骤1: 检查数据库连接状态...");
            
            Integer connectionCheck = kndb5000Dao.checkDatabaseConnection();
            if (connectionCheck == null || connectionCheck != 1) {
                throw new RuntimeException("数据库连接检查失败");
            }
            
            // 获取数据库信息
            String databaseName = extractDatabaseName(datasourceUrl);
            Long dbSize = kndb5000Dao.getDatabaseSize(databaseName);
            Integer tableCount = kndb5000Dao.getTableCount(databaseName);
            
            addLog(logContent, "数据库名称: " + databaseName);
            addLog(logContent, "数据库大小: " + formatBytes(dbSize));
            addLog(logContent, "表数量: " + tableCount);
            logger.info("数据库连接正常 - 数据库: {}, 大小: {}, 表数量: {}", 
                       databaseName, formatBytes(dbSize), tableCount);
            
            // 步骤2: 确定备份路径和文件名
            addLog(logContent, "步骤2: 准备备份路径和文件名...");
            logger.info("步骤2: 准备备份路径和文件名...");
            
            String backupPath = determineBackupPath();
            String timestamp = LocalDateTime.now().format(DATETIME_FORMATTER);
            String backupFileName = String.format("%s_bk_%s.sql", databaseName, timestamp);
            String fullBackupPath = Paths.get(backupPath, backupFileName).toString();
            
            backupInfo = new KNDB5000Entity(backupFileName, fullBackupPath);
            
            addLog(logContent, "备份路径: " + backupPath);
            addLog(logContent, "备份文件: " + backupFileName);
            logger.info("备份文件路径: {}", fullBackupPath);
            
            // 步骤3: 创建备份目录
            addLog(logContent, "步骤3: 创建备份目录...");
            logger.info("步骤3: 创建备份目录...");
            
            Path backupDir = Paths.get(backupPath);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
                addLog(logContent, "备份目录创建成功: " + backupPath);
                logger.info("备份目录创建成功: {}", backupPath);
            } else {
                addLog(logContent, "备份目录已存在: " + backupPath);
                logger.info("备份目录已存在: {}", backupPath);
            }
            
            // 步骤4: 执行数据库备份
            addLog(logContent, "步骤4: 开始执行数据库备份...");
            logger.info("步骤4: 开始执行数据库备份...");
            
            boolean backupResult = performDatabaseBackup(databaseName, fullBackupPath, logContent);
            
            if (!backupResult) {
                throw new RuntimeException("数据库备份执行失败");
            }
            
            // 步骤5: 验证备份文件
            addLog(logContent, "步骤5: 验证备份文件...");
            logger.info("步骤5: 验证备份文件...");
            
            File backupFile = new File(fullBackupPath);
            if (!backupFile.exists() || backupFile.length() == 0) {
                throw new RuntimeException("备份文件不存在或为空");
            }
            
            backupInfo.setBackupFileSize(backupFile.length());
            backupInfo.setBackupEndTime(LocalDateTime.now());
            backupInfo.setBackupStatus("SUCCESS");
            
            addLog(logContent, "备份文件验证成功");
            addLog(logContent, "备份文件大小: " + formatBytes(backupFile.length()));
            logger.info("备份文件验证成功，大小: {}", formatBytes(backupFile.length()));
            
            // 步骤6: 清理过期备份文件
            addLog(logContent, "步骤6: 清理过期备份文件...");
            logger.info("步骤6: 清理过期备份文件...");
            
            int deletedFiles = cleanupOldBackups(backupPath, databaseName,logContent);
            addLog(logContent, "清理过期备份文件数量: " + deletedFiles);
            logger.info("清理过期备份文件数量: {}", deletedFiles);
            
            success = true;
            addLog(logContent, "========== 数据库备份执行成功 ==========");
            logger.info("========== 数据库备份执行成功 ==========");
            
            // 更新贡献统计
            contribution.incrementReadCount();
            contribution.incrementWriteCount(1);
            
            return RepeatStatus.FINISHED;
            
        } catch (Exception e) {
            addLog(logContent, "========== " + batchName + " 数据库备份执行异常 ==========");
            addLog(logContent, "错误信息: " + e.getMessage());
            logger.error("========== {} 数据库备份执行异常 ==========", batchName, e);
            
            if (backupInfo != null) {
                backupInfo.setBackupEndTime(LocalDateTime.now());
                backupInfo.setBackupStatus("FAILED");
                backupInfo.setErrorMessage(e.getMessage());
            }
            
            success = false;
            logExecutionResult(batchName, "ERROR", startTime, logContent);
            throw e;
        } finally {
            // 发送邮件通知
            sendEmailNotification(batchName, description, success, logContent.toString(), backupInfo);
        }
    }
    
    /**
     * 从数据源URL中提取数据库名称
     */
    private String extractDatabaseName(String url) {
        // jdbc:mysql://192.168.50.101:*****/KNStudent
        String[] parts = url.split("/");
        String dbNameWithParams = parts[parts.length - 1];
        return dbNameWithParams.split("\\?")[0]; // 去掉参数部分
    }
    
    /**
     * 确定备份路径
     */
    private String determineBackupPath() {
        StringBuilder logContent = new StringBuilder(); // 如果方法外部没有logContent，需要创建
        
        // 记录配置信息
        // addLog(logContent, "生产环境备份路径配置: " + (productionBackupPath != null ? productionBackupPath : "未配置"));
        addLog(logContent, deployEnvironment + "备份路径配置: " + (productionBackupPath != null ? productionBackupPath : "未配置"));
        addLog(logContent, "本地备份路径配置: " + localBackupPath);
        
        // 如果配置了生产环境路径，直接使用（不检查父目录是否存在）
        if (productionBackupPath != null && !productionBackupPath.trim().isEmpty()) {
            // addLog(logContent, "使用生产环境备份路径: " + productionBackupPath);
            // logger.info("使用生产环境备份路径: {}", productionBackupPath);
            addLog(logContent, String.format("使用%s备份路径: %s", deployEnvironment, productionBackupPath));
            logger.info("使用{}备份路径: {}", deployEnvironment, productionBackupPath);

            return productionBackupPath;
        }
        
        // 否则使用本地路径
        addLog(logContent, "使用本地备份路径: " + localBackupPath);
        logger.info("使用本地备份路径: {}", localBackupPath);
        return localBackupPath;
    }
    
    /**
     * 执行数据库备份
     */
    private boolean performDatabaseBackup(String databaseName, String backupFilePath, StringBuilder logContent) {
        try {
            // 从URL中提取主机和端口
            String host = extractHostFromUrl(datasourceUrl);
            String port = extractPortFromUrl(datasourceUrl);
            
            // 构建mysqldump命令
            String[] command = {
                "mysqldump",
                "--host=" + host,
                "--port=" + port,
                "--user=" + dbUsername,
                "--password=" + dbPassword,
                "--single-transaction",
                "--routines",
                "--triggers",
                "--quick",
                "--lock-tables=false",
                databaseName
            };
            
            addLog(logContent, "执行备份命令: mysqldump --host=" + host + " --port=" + port + " --user=" + dbUsername + " " + databaseName);
            logger.info("执行备份命令: mysqldump --host={} --port={} --user={} {}", host, port, dbUsername, databaseName);
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectOutput(new File(backupFilePath));
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // 等待备份完成
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                addLog(logContent, "mysqldump执行成功");
                logger.info("mysqldump执行成功");
                return true;
            } else {
                addLog(logContent, "mysqldump执行失败，退出代码: " + exitCode);
                logger.error("mysqldump执行失败，退出代码: {}", exitCode);
                return false;
            }
            
        } catch (Exception e) {
            addLog(logContent, "执行mysqldump时发生异常: " + e.getMessage());
            logger.error("执行mysqldump时发生异常", e);
            return false;
        }
    }
    
    /**
     * 从URL中提取主机地址
     */
    private String extractHostFromUrl(String url) {
        // jdbc:mysql://192.168.50.101:*****/KNStudent
        String hostPart = url.substring(url.indexOf("://") + 3);
        return hostPart.substring(0, hostPart.indexOf(":"));
    }
    
    /**
     * 从URL中提取端口号
     */
    private String extractPortFromUrl(String url) {
        // jdbc:mysql://192.168.50.101:*****/KNStudent
        String hostPart = url.substring(url.indexOf("://") + 3);
        String portPart = hostPart.substring(hostPart.indexOf(":") + 1);
        return portPart.substring(0, portPart.indexOf("/"));
    }
    
    /**
     * 清理过期备份文件 - 修复资源泄漏问题
     */
    private int cleanupOldBackups(String backupPath, String databaseName, StringBuilder logContent) {
        int deletedCount = 0;
        try {
            Path backupDir = Paths.get(backupPath);
            if (!Files.exists(backupDir)) {
                addLog(logContent, "备份目录不存在，跳过清理: " + backupPath);
                return 0;
            }
            
            long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L);
            
            // 使用 try-with-resources 确保资源正确关闭，修复连接泄漏问题
            try (Stream<Path> files = Files.list(backupDir)) {
                List<Path> oldBackups = files
                    .filter(path -> path.toString().contains(databaseName + "_bk_"))
                    .filter(path -> path.toString().endsWith(".sql"))
                    .filter(path -> {
                        try {
                            long lastModified = Files.getLastModifiedTime(path).toMillis();
                            return lastModified < cutoffTime;
                        } catch (Exception e) {
                            logger.warn("获取文件修改时间失败: {}", path.getFileName(), e);
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
                    
                for (Path path : oldBackups) {
                    try {
                        Files.delete(path);
                        deletedCount++;
                        addLog(logContent, "删除过期备份文件: " + path.getFileName());
                        logger.info("删除过期备份文件: {}", path.getFileName());
                    } catch (Exception e) {
                        addLog(logContent, "删除文件失败: " + path.getFileName() + ", 错误: " + e.getMessage());
                        logger.warn("删除过期备份文件失败: {}", path.getFileName(), e);
                    }
                }
                
                addLog(logContent, "清理完成，删除文件数: " + deletedCount);
            }
                
        } catch (Exception e) {
            addLog(logContent, "清理过期备份文件时发生异常: " + e.getMessage());
            logger.error("清理过期备份文件时发生异常", e);
        }
        return deletedCount;
    }
    
    /**
     * 格式化字节数
     */
    private String formatBytes(Long bytes) {
        if (bytes == null) return "Unknown";
        
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * 添加日志条目（带时间戳）
     */
    private void addLog(StringBuilder logContent, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logContent.append(String.format("[%s] %s\n", timestamp, message));
    }
    
    /**
     * 记录执行结果日志
     */
    private void logExecutionResult(String batchName, String status, long startTime, StringBuilder logContent) {
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        addLog(logContent, "========== " + batchName + " 数据库备份执行完成 ==========");
        addLog(logContent, "批处理名称: " + batchName);
        addLog(logContent, "执行状态: " + status);
        addLog(logContent, "执行时间: " + executionTime + " ms (" + (executionTime / 1000.0) + " 秒)");
        addLog(logContent, "执行结束时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        addLog(logContent, "================================================");
        
        logger.info("========== {} 数据库备份执行完成 ==========", batchName);
        logger.info("批处理名称: {}", batchName);
        logger.info("执行状态: {}", status);
        logger.info("执行时间: {} ms ({} 秒)", executionTime, executionTime / 1000.0);
        logger.info("执行结束时间: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("================================================");
    }
    
    /**
     * 发送邮件通知
     */
    private void sendEmailNotification(String jobName, String description, boolean success, String logContent, KNDB5000Entity backupInfo) {
        try {
            if (emailService != null) {

                // 从数据库邮件管理表提取邮件管理信息
                BatchMailInfo mailInfo = mailDao.selectMailInfo(jobId);
                // 给程序维护者发送邮件
                emailService.setFromEmail(mailInfo.getEmailFrom());
                emailService.setToEmails(mailInfo.getMailToDevloper());

                String content = buildEmailContent(success, logContent, backupInfo);
                emailService.sendBatchNotification(jobName, description,success, content);

                // 如果用户邮件不为空，则给用户发送邮件 Testing...
                // if (!mailInfo.getEmailToUser().isEmpty()){
                //     emailService.setFromEmail(mailInfo.getEmailFrom());
                //     emailService.setToEmails(mailInfo.getEmailToUser());
                //     content = buildEmailContent(success, mailInfo.getMailContentForUser(), backupInfo);
                //     emailService.sendBatchNotification(jobName, description, success, content);
                // }

                logger.info("邮件通知发送完成 - jobName: {}, success: {}", jobName, success);
            } else {
                logger.info("邮件服务未启用，跳过邮件发送 - jobName: {}", jobName);
            }
        } catch (Exception e) {
            logger.error("发送邮件通知时出错 - jobName: {}, error: {}", jobName, e.getMessage(), e);
            // 不要因为邮件发送失败而影响批处理任务的状态
        }
    }
    
    /**
     * 构建邮件内容
     */
    private String buildEmailContent(boolean success, String logContent, KNDB5000Entity backupInfo) {
        StringBuilder content = new StringBuilder();
        content.append("数据库备份执行结果通知\n");
        content.append("========================================\n");
        content.append("执行状态: ").append(success ? "成功" : "失败").append("\n");
        
        if (backupInfo != null) {
            content.append("备份文件: ").append(backupInfo.getBackupFileName()).append("\n");
            content.append("备份路径: ").append(backupInfo.getBackupFilePath()).append("\n");
            if (backupInfo.getBackupFileSize() != null) {
                content.append("文件大小: ").append(formatBytes(backupInfo.getBackupFileSize())).append("\n");
            }
            if (backupInfo.getBackupStartTime() != null) {
                content.append("开始时间: ").append(backupInfo.getBackupStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            }
            if (backupInfo.getBackupEndTime() != null) {
                content.append("结束时间: ").append(backupInfo.getBackupEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            }
        }
        
        content.append("========================================\n");
        content.append("详细日志:\n");
        content.append(logContent);
        
        return content.toString();
    }
}