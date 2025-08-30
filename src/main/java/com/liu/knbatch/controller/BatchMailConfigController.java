package com.liu.knbatch.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.liu.knbatch.config.BatchMailInfo;
import com.liu.knbatch.dao.BatchMailConfigDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Controller
public class BatchMailConfigController {

    @Autowired
    BatchMailConfigDao batchMailConfigDao;

    // 页面加载时显示所有邮件配置信息
    @GetMapping("/batch/mail")
    public String list(Model model) {
        Collection<BatchMailInfo> collection = batchMailConfigDao.selectAllMailInfo();
        model.addAttribute("mailList", collection);
        return "batch_mail_config_list";
    }

    // 检索按钮 - 根据job_id精确查询
    @GetMapping("/batch/mail/search")
    public String search(@RequestParam String jobId, Model model) {
        List<BatchMailInfo> searchResults = new ArrayList<>();
        
        if (jobId != null && !jobId.trim().isEmpty()) {
            BatchMailInfo mailInfo = batchMailConfigDao.selectMailInfo(jobId.trim());
            if (mailInfo != null) {
                searchResults.add(mailInfo);
            }
        }
        
        model.addAttribute("mailList", searchResults);
        model.addAttribute("searchJobId", jobId); // 保持检索条件
        return "batch_mail_config_list";
    }

    // 追加按钮 - 跳转到新增页面
    @GetMapping("/batch/mail/add")
    public String toMailAdd(Model model) {
        return "batch_mail_info";
    }

    // 编辑按钮 - 跳转到编辑页面
    @GetMapping("/batch/mail/{jobId}")
    public String toMailEdit(@PathVariable("jobId") String jobId, Model model) {
        BatchMailInfo mailInfo = batchMailConfigDao.selectMailInfo(jobId);
        model.addAttribute("selectedMail", mailInfo);
        return "batch_mail_info";
    }

    // 保存邮件配置信息
    @PostMapping("/batch/mail")
    public String executeMailAddEdit(Model model, @ModelAttribute BatchMailInfo mailInfo) {
        // 画面数据有效性校验
        if (validateHasError(model, mailInfo)) {
            model.addAttribute("selectedMail", mailInfo);
            return "batch_mail_info";
        }
        
        BatchMailInfo batchMailInfo = batchMailConfigDao.selectMailInfo(mailInfo.getJobId());
        if (batchMailInfo != null) {
            batchMailConfigDao.updateMailInfo(mailInfo);
        } else {
            batchMailConfigDao.insertMailInfo(mailInfo);
        }
        
        return "redirect:/batch/mail";
    }

    // 数据校验
    private boolean validateHasError(Model model, BatchMailInfo mailInfo) {
        boolean hasError = false;
        List<String> msgList = new ArrayList<String>();
        hasError = inputDataHasError(mailInfo, msgList);
        
        if (hasError) {
            model.addAttribute("errorMessageList", msgList);
        }
        return hasError;
    }

    // 输入数据校验
    private boolean inputDataHasError(BatchMailInfo mailInfo, List<String> msgList) {
        if (mailInfo.getJobId() == null || mailInfo.getJobId().trim().isEmpty()) {
            msgList.add("请输入作业ID");
        }
        
        if (mailInfo.getEmailFrom() == null || mailInfo.getEmailFrom().trim().isEmpty()) {
            msgList.add("请输入发送方邮箱");
        }
        
        // 邮箱格式验证
        if (mailInfo.getEmailFrom() != null && !mailInfo.getEmailFrom().trim().isEmpty()) {
            if (!isValidEmail(mailInfo.getEmailFrom())) {
                msgList.add("发送方邮箱格式不正确");
            }
        }
        
        // 开发者邮箱格式验证（如果不为空）
        if (mailInfo.getMailToDevloper() != null && !mailInfo.getMailToDevloper().trim().isEmpty()) {
            String[] emails = mailInfo.getMailToDevloper().split(",");
            for (String email : emails) {
                if (!isValidEmail(email.trim())) {
                    msgList.add("开发者邮箱格式不正确：" + email.trim());
                    break;
                }
            }
        }
        
        // 用户邮箱格式验证（如果不为空）
        if (mailInfo.getEmailToUser() != null && !mailInfo.getEmailToUser().trim().isEmpty()) {
            String[] emails = mailInfo.getEmailToUser().split(",");
            for (String email : emails) {
                if (!isValidEmail(email.trim())) {
                    msgList.add("用户邮箱格式不正确：" + email.trim());
                    break;
                }
            }
        }
        
        return (msgList.size() != 0);
    }

    // 邮箱格式验证
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}