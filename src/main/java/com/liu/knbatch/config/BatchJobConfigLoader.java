package com.liu.knbatch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 批处理作业配置加载器
 * 负责从XML配置文件中读取批处理作业定义
 * 
 * @author liu
 * @version 1.0.0
 */
@Component
public class BatchJobConfigLoader {
    
    @Value("${batch.job.config.file:batch-jobs.xml}")
    private String configFileName;
    
    /**
     * 从XML配置文件加载所有批处理作业定义
     */
    public List<BatchJobInfo> loadBatchJobs() throws Exception {
        List<BatchJobInfo> jobs = new ArrayList<>();
        
        try {
            System.out.println("正在加载批处理作业配置文件: " + configFileName);
            
            ClassPathResource resource = new ClassPathResource(configFileName);
            if (!resource.exists()) {
                throw new RuntimeException("批处理作业配置文件不存在: " + configFileName);
            }
            
            InputStream inputStream = resource.getInputStream();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();
            
            NodeList jobNodes = document.getElementsByTagName("batch-job");
            System.out.println("发现 " + jobNodes.getLength() + " 个批处理作业配置节点");
            
            for (int i = 0; i < jobNodes.getLength(); i++) {
                Element jobElement = (Element) jobNodes.item(i);
                try {
                    BatchJobInfo jobInfo = parseJobElement(jobElement);
                    jobs.add(jobInfo);
                    System.out.println("解析作业配置: " + jobInfo.getJobId() + " - " + jobInfo.getDescription() + 
                                     " (enabled=" + jobInfo.isEnabled() + ")");
                } catch (Exception e) {
                    System.err.println("解析第 " + (i+1) + " 个作业配置时出错: " + e.getMessage());
                    throw e;
                }
            }
            
            inputStream.close();
            System.out.println("成功加载 " + jobs.size() + " 个批处理作业配置");
            
        } catch (Exception e) {
            System.err.println("加载批处理作业配置失败: " + e.getMessage());
            throw e;
        }
        
        return jobs;
    }
    
    /**
     * 解析单个批处理作业XML元素
     */
    private BatchJobInfo parseJobElement(Element jobElement) {
        BatchJobInfo jobInfo = new BatchJobInfo();
        
        try {
            // 必需属性
            jobInfo.setJobId(getRequiredAttribute(jobElement, "id"));
            jobInfo.setBeanName(getRequiredAttribute(jobElement, "bean-name"));
            jobInfo.setDescription(getRequiredAttribute(jobElement, "description"));
            
            // 可选属性
            jobInfo.setCronExpression(getOptionalAttribute(jobElement, "cron-expression"));
            jobInfo.setCronDescription(getOptionalAttribute(jobElement, "cron-description"));
            jobInfo.setTargetDescription(getOptionalAttribute(jobElement, "target-description"));
            
            // 启用状态（默认为true）
            String enabledStr = getOptionalAttribute(jobElement, "enabled");
            jobInfo.setEnabled(enabledStr == null || Boolean.parseBoolean(enabledStr));
            
            // 验证Bean名称格式（可选验证）
            validateBeanName(jobInfo.getJobId(), jobInfo.getBeanName());
            
        } catch (Exception e) {
            throw new RuntimeException("解析作业配置 " + jobInfo.getJobId() + " 时出错: " + e.getMessage(), e);
        }
        
        return jobInfo;
    }
    
    /**
     * 验证Bean名称格式是否符合约定
     */
    private void validateBeanName(String jobId, String beanName) {
        if (jobId != null && beanName != null) {
            String expectedBeanName = jobId.toLowerCase() + "Job";
            if (!expectedBeanName.equals(beanName)) {
                System.out.println("警告: 作业 " + jobId + " 的Bean名称 '" + beanName + 
                                 "' 不符合约定，建议使用 '" + expectedBeanName + "'");
            }
        }
    }
    
    /**
     * 获取必需的XML属性值
     */
    private String getRequiredAttribute(Element element, String attributeName) {
        String value = element.getAttribute(attributeName);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "批处理作业配置错误: 缺少必需属性 '" + attributeName + "' in element " + element.getTagName());
        }
        return value.trim();
    }
    
    /**
     * 获取可选的XML属性值
     */
    private String getOptionalAttribute(Element element, String attributeName) {
        String value = element.getAttribute(attributeName);
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
}