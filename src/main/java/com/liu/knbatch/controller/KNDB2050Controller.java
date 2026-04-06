package com.liu.knbatch.controller;

import com.liu.knbatch.dao.KNDB2050Dao;
import com.liu.knbatch.entity.KNDB2050Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KNDB2050 计划课转换成加课 Web 控制器
 *
 * @author Liu
 * @version 1.0.0
 */
@Controller
public class KNDB2050Controller {

    private static final Logger logger = LoggerFactory.getLogger(KNDB2050Controller.class);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("kndb2050Job")
    private Job kndb2050Job;

    @Autowired
    private KNDB2050Dao kndb2050Dao;

    /**
     * 显示操作画面
     */
    @GetMapping("/kndb2050")
    public String showPage(Model model) {
        model.addAttribute("studentList", kndb2050Dao.getActiveStudentList());
        model.addAttribute("currentYear", LocalDate.now().getYear());
        model.addAttribute("currentMonth", LocalDate.now().getMonthValue());
        return "kndb2050";
    }

    /**
     * AJAX: 根据学生ID取该生正在学习的科目列表
     */
    @GetMapping("/kndb2050/subjects")
    @ResponseBody
    public List<Map<String, String>> getSubjects(@RequestParam String stuId) {
        return kndb2050Dao.getSubjectListByStuId(stuId);
    }

    /**
     * AJAX: 预览 — 取执行对象的计划课列表 + 结算状态确认
     * 画面在此时将课程信息显示给用户确认，若已结算则前端将「执行」按钮设为不可用
     */
    @GetMapping("/kndb2050/preview")
    @ResponseBody
    public Map<String, Object> preview(
            @RequestParam String stuId,
            @RequestParam String subjectId,
            @RequestParam String yearMonth) {

        Map<String, Object> result = new HashMap<>();

        List<KNDB2050Entity> targetList =
                kndb2050Dao.getTargetLessonList(stuId, subjectId, yearMonth);
        result.put("targetList", targetList);

        if (targetList.isEmpty()) {
            result.put("alreadyPaid", false);
            result.put("canExecute", false);
            result.put("message", "没有符合条件的计划课（lessonType=1 + 已签到）。");
            logger.info("KNDB2050 预览: 无执行对象 - stuId={}, subjectId={}, yearMonth={}", stuId, subjectId, yearMonth);
            return result;
        }

        String lsnFeeId = targetList.get(0).getLsnFeeId();
        boolean alreadyPaid = kndb2050Dao.checkIfFeeAlreadyPaid(lsnFeeId);
        result.put("alreadyPaid", alreadyPaid);
        result.put("canExecute", !alreadyPaid);
        result.put("lsnFeeId", lsnFeeId);
        result.put("message", alreadyPaid
                ? "该月份课费已经结算（lsnFeeId=" + lsnFeeId + "），不能执行。"
                : "共 " + targetList.size() + " 件计划课，确认无误后请点击「执行」。");

        logger.info("KNDB2050 预览: stuId={}, subjectId={}, yearMonth={}, 件数={}, alreadyPaid={}",
                stuId, subjectId, yearMonth, targetList.size(), alreadyPaid);

        return result;
    }

    /**
     * 执行: 触发 Spring Batch Job
     */
    @PostMapping("/kndb2050/execute")
    @ResponseBody
    public Map<String, Object> execute(
            @RequestParam String stuId,
            @RequestParam String subjectId,
            @RequestParam String yearMonth) {

        Map<String, Object> result = new HashMap<>();

        try {
            // yearMonth（yyyy-MM）→ yyyyMMdd（月1日）として渡す
            String baseDate = yearMonth.replace("-", "") + "01";

            JobParameters params = new JobParametersBuilder()
                    .addString("baseDate", baseDate)
                    .addString("jobMode", "MANUAL")
                    .addString("businessModule", "KNDB2050")
                    .addString("stuId", stuId)
                    .addString("subjectId", subjectId)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(kndb2050Job, params);

            logger.info("KNDB2050 执行完了 - stuId={}, subjectId={}, 对象年月={}", stuId, subjectId, yearMonth);
            result.put("success", true);
            result.put("message", "执行完了。stuId=" + stuId
                    + ", subjectId=" + subjectId + ", 对象年月=" + yearMonth);

        } catch (Exception e) {
            logger.error("KNDB2050 执行失败 - stuId={}, subjectId={}, 对象年月={}, error={}",
                    stuId, subjectId, yearMonth, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "执行失败：" + e.getMessage());
        }

        return result;
    }
}
