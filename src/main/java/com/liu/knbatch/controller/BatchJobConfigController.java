package com.liu.knbatch.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.liu.knbatch.config.BatchJobInfo;
import com.liu.knbatch.dao.BatchJobConfigDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Controller
public class BatchJobConfigController {

    @Autowired
    BatchJobConfigDao batchJobConfigDao;

    // 页面加载时显示所有Job信息
    @GetMapping("/batch/job")
    public String list(Model model) {
        Collection<BatchJobInfo> collection = batchJobConfigDao.loadBatchJobs();
        model.addAttribute("jobList", collection);
        return "batch_job_config_list";
    }

    // 检索按钮 - 根据job_id精确查询
    @GetMapping("/batch/job/search")
    public String search(@RequestParam String jobId, Model model) {
        List<BatchJobInfo> searchResults = new ArrayList<>();
        
        if (jobId != null && !jobId.trim().isEmpty()) {
            BatchJobInfo jobInfo = batchJobConfigDao.findByJobId(jobId.trim());
            if (jobInfo != null) {
                searchResults.add(jobInfo);
            }
        }
        
        model.addAttribute("jobList", searchResults);
        model.addAttribute("searchJobId", jobId); // 保持检索条件
        return "batch_job_config_list";
    }

    // 追加按钮 - 跳转到新增页面
    @GetMapping("/batch/job/add")
    public String toJobAdd(Model model) {
        return "batch_job_info";
    }

    // 编辑按钮 - 跳转到编辑页面
    @GetMapping("/batch/job/{jobId}")
    public String toJobEdit(@PathVariable("jobId") String jobId, Model model) {
        BatchJobInfo batchJobInfo = batchJobConfigDao.findByJobId(jobId);
        model.addAttribute("selectedJob", batchJobInfo);
        return "batch_job_info";
    }

    // 保存Job信息
    @PostMapping("/batch/job")
    public String executeJobAddEdit(Model model, @ModelAttribute BatchJobInfo batchJobInfo) {
        // 画面数据有效性校验
        if (validateHasError(model, batchJobInfo)) {
            model.addAttribute("selectedJob", batchJobInfo);
            return "batch_job_info";
        }

        BatchJobInfo jobInfo = batchJobConfigDao.findByJobId(batchJobInfo.getJobId());
        if (jobInfo != null) {
            batchJobConfigDao.updateJobInfo(batchJobInfo);
        } else {
            batchJobConfigDao.insertJobInfo(batchJobInfo);
        }
        return "redirect:/batch/job";
    }

    // 数据校验
    private boolean validateHasError(Model model, BatchJobInfo batchJobInfo) {
        boolean hasError = false;
        List<String> msgList = new ArrayList<String>();
        hasError = inputDataHasError(batchJobInfo, msgList);
        
        if (hasError) {
            model.addAttribute("errorMessageList", msgList);
        }
        return hasError;
    }

    // 输入数据校验
    private boolean inputDataHasError(BatchJobInfo batchJobInfo, List<String> msgList) {
        if (batchJobInfo.getJobId() == null || batchJobInfo.getJobId().trim().isEmpty()) {
            msgList.add("请输入作业ID");
        }
        
        if (batchJobInfo.getBeanName() == null || batchJobInfo.getBeanName().trim().isEmpty()) {
            msgList.add("请输入Bean名称");
        }
        
        if (batchJobInfo.getDescription() == null || batchJobInfo.getDescription().trim().isEmpty()) {
            msgList.add("请输入作业描述");
        }
        
        if (batchJobInfo.getCronExpression() == null || batchJobInfo.getCronExpression().trim().isEmpty()) {
            msgList.add("请输入Cron表达式");
        }
        
        return (msgList.size() != 0);
    }
}