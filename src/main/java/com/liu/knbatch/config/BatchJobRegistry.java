package com.liu.knbatch.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.liu.knbatch.dao.BatchJobConfigDao;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 批处理作业注册表
 * 负责从配置文件中动态加载所有批处理作业信息
 * 实现零代码修改的作业管理
 * 
 * @author liu
 * @version 1.0.0
 */
@Component
public class BatchJobRegistry {
    
    @Autowired
    // private BatchJobConfigLoader configLoader; //使用BatchJobConfigDao来替代Batch作业在batch-jobs.xml里的配置
    private BatchJobConfigDao configLoader;
    
    private Map<String, BatchJobInfo> jobRegistry = new HashMap<>();
    
    /**
     * 应用启动时自动加载所有批处理作业配置
     */
    @PostConstruct
    public void initializeRegistry() {
        try {
            System.out.println("开始初始化批处理作业注册表...");
            
            List<BatchJobInfo> jobs = configLoader.loadBatchJobs();
            for (BatchJobInfo job : jobs) {
                if (job.isEnabled()) {
                    jobRegistry.put(job.getJobId(), job);
                    System.out.println("已注册批处理作业: " + job.getJobId() + " - " + job.getDescription() + 
                                     " (Bean: " + job.getBeanName() + ")");
                } else {
                    System.out.println("跳过禁用的批处理作业: " + job.getJobId() + " - " + job.getDescription());
                }
            }
            
            System.out.println("批处理作业注册完成，共注册 " + jobRegistry.size() + " 个启用的作业");
            System.out.println("================================");
            
        } catch (Exception e) {
            System.err.println("批处理作业注册失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("批处理作业注册失败", e);
        }
    }
    
    /**
     * 根据作业ID获取作业信息
     */
    public BatchJobInfo getJobInfo(String jobId) {
        return jobRegistry.get(jobId);
    }
    
    /**
     * 获取所有已注册的作业信息
     */
    public List<BatchJobInfo> getAllJobs() {
        return new ArrayList<>(jobRegistry.values());
    }
    
    /**
     * 获取所有启用的作业ID列表
     */
    public Set<String> getEnabledJobIds() {
        return jobRegistry.keySet();
    }
    
    /**
     * 检查指定作业是否已注册
     */
    public boolean isJobRegistered(String jobId) {
        return jobRegistry.containsKey(jobId);
    }
    
    /**
     * 获取已注册作业的数量
     */
    public int getRegisteredJobCount() {
        return jobRegistry.size();
    }
    
    /**
     * 根据Bean名称查找作业信息
     */
    public BatchJobInfo getJobInfoByBeanName(String beanName) {
        return jobRegistry.values().stream()
                .filter(job -> beanName.equals(job.getBeanName()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取具有定时表达式的作业（用于定时任务调度）
     */
    public List<BatchJobInfo> getScheduledJobs() {
        return jobRegistry.values().stream()
                .filter(job -> job.getCronExpression() != null && !job.getCronExpression().trim().isEmpty())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 打印所有已注册作业的详细信息（用于调试）
     */
    public void printAllJobs() {
        System.out.println("========== 所有已注册的批处理作业 ==========");
        for (BatchJobInfo job : getAllJobs()) {
            System.out.println("作业ID: " + job.getJobId());
            System.out.println("Bean名称: " + job.getBeanName());
            System.out.println("描述: " + job.getDescription());
            System.out.println("定时表达式: " + (job.getCronExpression() != null ? job.getCronExpression() : "无"));
            System.out.println("是否启用: " + job.isEnabled());
            System.out.println("---");
        }
        System.out.println("=========================================");
    }
}