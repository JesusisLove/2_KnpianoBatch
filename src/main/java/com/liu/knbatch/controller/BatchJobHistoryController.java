package com.liu.knbatch.controller;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class BatchJobHistoryController {

    @Autowired
    private JobExplorer jobExplorer;

    // 作业执行历史一览（默认当前年月）
    @GetMapping("/batch/history")
    public String list(Model model) {
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1;
        List<Map<String, Object>> historyList = loadAllJobExecutions(null, year, month);
        model.addAttribute("historyList", historyList);
        model.addAttribute("searchYear", year);
        model.addAttribute("searchMonth", month);
        return "batch_job_history_list";
    }

    // 按条件检索（year=0 表示全部年份，month=0 表示全部月份）
    @GetMapping("/batch/history/search")
    public String search(@RequestParam(required = false) String jobName,
                         @RequestParam(required = false) Integer searchYear,
                         @RequestParam(required = false) Integer searchMonth,
                         Model model) {
        Calendar now = Calendar.getInstance();
        int year = (searchYear != null) ? searchYear : now.get(Calendar.YEAR);
        int month = (searchMonth != null) ? searchMonth : now.get(Calendar.MONTH) + 1;

        // 年份"全部"时，月份强制"全部"
        if (year == 0) {
            month = 0;
        }

        String filterJobName = (jobName != null && !jobName.trim().isEmpty()) ? jobName.trim() : null;
        List<Map<String, Object>> historyList = loadAllJobExecutions(filterJobName, year, month);

        model.addAttribute("historyList", historyList);
        model.addAttribute("searchYear", year);
        model.addAttribute("searchMonth", month);
        if (filterJobName != null) {
            model.addAttribute("searchJobName", jobName);
        }
        return "batch_job_history_list";
    }

    // 加载作业执行记录（year=0:全部年份, month=0:全部月份）
    private List<Map<String, Object>> loadAllJobExecutions(String filterJobName, int year, int month) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 计算过滤时间范围
        Date dateFrom = null;
        Date dateTo = null;

        if (year == 0) {
            // 全部年份：从2025年1月1日到现在
            Calendar calFrom = Calendar.getInstance();
            calFrom.set(2025, 0, 1, 0, 0, 0);
            calFrom.set(Calendar.MILLISECOND, 0);
            dateFrom = calFrom.getTime();
            // dateTo 为 null 表示不限上界
        } else if (month == 0) {
            // 指定年份 + 全部月份：该年1月1日 ~ 次年1月1日
            Calendar calFrom = Calendar.getInstance();
            calFrom.set(year, 0, 1, 0, 0, 0);
            calFrom.set(Calendar.MILLISECOND, 0);
            dateFrom = calFrom.getTime();

            Calendar calTo = Calendar.getInstance();
            calTo.set(year + 1, 0, 1, 0, 0, 0);
            calTo.set(Calendar.MILLISECOND, 0);
            dateTo = calTo.getTime();
        } else {
            // 指定年份 + 指定月份
            Calendar calFrom = Calendar.getInstance();
            calFrom.set(year, month - 1, 1, 0, 0, 0);
            calFrom.set(Calendar.MILLISECOND, 0);
            dateFrom = calFrom.getTime();

            Calendar calTo = Calendar.getInstance();
            calTo.set(year, month - 1, 1, 0, 0, 0);
            calTo.set(Calendar.MILLISECOND, 0);
            calTo.add(Calendar.MONTH, 1);
            dateTo = calTo.getTime();
        }

        try {
            List<String> jobNames;
            if (filterJobName != null) {
                jobNames = List.of(filterJobName);
            } else {
                jobNames = jobExplorer.getJobNames();
            }

            for (String jobName : jobNames) {
                List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, 100);
                for (JobInstance instance : instances) {
                    List<JobExecution> executions = jobExplorer.getJobExecutions(instance);
                    for (JobExecution execution : executions) {
                        // 按年月过滤
                        Date startTime = execution.getStartTime();
                        if (startTime == null || startTime.before(dateFrom)
                                || (dateTo != null && !startTime.before(dateTo))) {
                            continue;
                        }

                        Map<String, Object> record = new HashMap<>();
                        record.put("executionId", execution.getId());
                        record.put("jobName", jobName);
                        record.put("startTime", startTime);
                        record.put("endTime", execution.getEndTime());
                        record.put("status", execution.getStatus().toString());
                        record.put("exitCode", execution.getExitStatus().getExitCode());

                        // 计算耗时
                        Date endTime = execution.getEndTime();
                        if (endTime != null) {
                            long durationMs = endTime.getTime() - startTime.getTime();
                            record.put("duration", formatDuration(durationMs));
                        } else if (execution.getStatus() == BatchStatus.STARTED) {
                            record.put("duration", "执行中...");
                        } else {
                            record.put("duration", "-");
                        }

                        // 星期
                        record.put("dayOfWeek", getDayOfWeek(startTime));

                        // 状态颜色CSS类
                        record.put("statusClass", getStatusClass(execution.getStatus()));
                        result.add(record);
                    }
                }
            }

            // 按执行ID倒序排列（最新的在前面）
            result.sort(Comparator.comparing((Map<String, Object> m) ->
                    (Long) m.get("executionId")).reversed());

        } catch (Exception e) {
            // 如果查询失败（如表还不存在），返回空列表
        }

        return result;
    }

    // 格式化耗时
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + "秒";
        }
        long minutes = seconds / 60;
        long remainSeconds = seconds % 60;
        if (minutes < 60) {
            return minutes + "分" + remainSeconds + "秒";
        }
        long hours = minutes / 60;
        long remainMinutes = minutes % 60;
        return hours + "时" + remainMinutes + "分" + remainSeconds + "秒";
    }

    // 获取星期
    private String getDayOfWeek(java.util.Date date) {
        String[] weekdays = {"日", "月", "火", "水", "木", "金", "土"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayIndex = cal.get(Calendar.DAY_OF_WEEK) - 1;
        return weekdays[dayIndex];
    }

    // 状态对应CSS类
    private String getStatusClass(BatchStatus status) {
        switch (status) {
            case COMPLETED:
                return "status-completed";
            case FAILED:
                return "status-failed";
            case STARTED:
            case STARTING:
                return "status-started";
            case STOPPED:
            case STOPPING:
                return "status-stopped";
            default:
                return "status-unknown";
        }
    }
}
