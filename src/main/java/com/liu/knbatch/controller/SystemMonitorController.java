package com.liu.knbatch.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.liu.knbatch.config.BatchJobInfo;
import com.liu.knbatch.dao.BatchJobConfigDao;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SystemMonitorController {

    @Autowired
    private BatchJobConfigDao batchJobConfigDao;

    @GetMapping("/batch/monitor")
    public String monitor(Model model) {
        // JVM 内存信息
        model.addAttribute("memory", getMemoryInfo());

        // 系统信息
        model.addAttribute("system", getSystemInfo());

        // 线程信息
        model.addAttribute("thread", getThreadInfo());

        // 作业注册状态
        model.addAttribute("jobStats", getJobStats());

        return "batch_system_monitor";
    }

    private Map<String, Object> getMemoryInfo() {
        Map<String, Object> info = new HashMap<>();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        long usedMB = heapUsage.getUsed() / (1024 * 1024);
        long maxMB = heapUsage.getMax() / (1024 * 1024);
        long committedMB = heapUsage.getCommitted() / (1024 * 1024);
        int usagePercent = maxMB > 0 ? (int) (usedMB * 100 / maxMB) : 0;

        info.put("usedMB", usedMB);
        info.put("maxMB", maxMB);
        info.put("committedMB", committedMB);
        info.put("usagePercent", usagePercent);

        // 进度条颜色类
        if (usagePercent < 60) {
            info.put("barClass", "bg-success");
        } else if (usagePercent < 80) {
            info.put("barClass", "bg-warning");
        } else {
            info.put("barClass", "bg-danger");
        }

        // 非堆内存
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        info.put("nonHeapUsedMB", nonHeapUsage.getUsed() / (1024 * 1024));

        return info;
    }

    private Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        info.put("osName", osBean.getName());
        info.put("osArch", osBean.getArch());
        info.put("osVersion", osBean.getVersion());
        info.put("cpuCount", osBean.getAvailableProcessors());
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("jvmName", runtimeBean.getVmName());

        // 启动时间
        long startTime = runtimeBean.getStartTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        info.put("startTime", sdf.format(new Date(startTime)));

        // 运行时长
        long uptimeMs = runtimeBean.getUptime();
        info.put("uptime", formatUptime(uptimeMs));

        return info;
    }

    private Map<String, Object> getThreadInfo() {
        Map<String, Object> info = new HashMap<>();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        info.put("activeCount", threadBean.getThreadCount());
        info.put("peakCount", threadBean.getPeakThreadCount());
        info.put("daemonCount", threadBean.getDaemonThreadCount());
        info.put("totalStarted", threadBean.getTotalStartedThreadCount());

        return info;
    }

    private Map<String, Object> getJobStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<BatchJobInfo> jobs = batchJobConfigDao.loadBatchJobs();
            int total = jobs.size();
            long enabled = jobs.stream().filter(BatchJobInfo::isEnabled).count();
            long disabled = total - enabled;

            stats.put("total", total);
            stats.put("enabled", enabled);
            stats.put("disabled", disabled);
            stats.put("jobs", jobs);
        } catch (Exception e) {
            stats.put("total", 0);
            stats.put("enabled", 0);
            stats.put("disabled", 0);
            stats.put("jobs", List.of());
        }
        return stats;
    }

    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天 ");
        }
        sb.append(hours).append("时 ");
        sb.append(minutes).append("分 ");
        sb.append(secs).append("秒");
        return sb.toString();
    }
}
