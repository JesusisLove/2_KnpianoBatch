package com.liu.knbatch.controller;

import com.liu.knbatch.dao.KNDB2040Dao;
import com.liu.knbatch.entity.KNDB2040Entity;
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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KNDB2040 学生指定月课费金额修正 Web 控制器
 *
 * @author Liu
 * @version 1.0.0
 */
@Controller
public class KNDB2040Controller {

    private static final Logger logger = LoggerFactory.getLogger(KNDB2040Controller.class);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("kndb2040Job")
    private Job kndb2040Job;

    @Autowired
    private KNDB2040Dao kndb2040Dao;

    /**
     * 显示操作画面
     */
    @GetMapping("/kndb2040")
    public String showPage(Model model) {
        model.addAttribute("studentList", kndb2040Dao.getActiveStudentList());
        model.addAttribute("currentYear", LocalDate.now().getYear());
        model.addAttribute("currentMonth", LocalDate.now().getMonthValue());
        return "kndb2040";
    }

    /**
     * AJAX: 根据学生ID取该生正在学习的科目列表
     */
    @GetMapping("/kndb2040/subjects")
    @ResponseBody
    public List<Map<String, String>> getSubjects(@RequestParam String stuId) {
        return kndb2040Dao.getSubjectListByStuId(stuId);
    }

    /**
     * AJAX: 根据学生ID + 科目ID 取当前子科目信息（子科目名/标准价格/调整价格/pay_style等）
     */
    @GetMapping("/kndb2040/currentSubInfo")
    @ResponseBody
    public KNDB2040Entity getCurrentSubInfo(
            @RequestParam String stuId,
            @RequestParam String subjectId) {
        return kndb2040Dao.getCurrentSubjectInfo(stuId, subjectId);
    }

    /**
     * AJAX: 根据科目ID + 当前子科目ID 取升级候补子科目列表（数值比较剔除 ≤ 当前子科目）
     */
    @GetMapping("/kndb2040/subEdaList")
    @ResponseBody
    public List<Map<String, Object>> getSubEdaList(
            @RequestParam String subjectId,
            @RequestParam String currentSubjectSubId) {
        return kndb2040Dao.getCandidateSubEdaList(subjectId, currentSubjectSubId);
    }

    /**
     * AJAX: 根据科目ID + 子科目ID 取标准价格
     */
    @GetMapping("/kndb2040/subPrice")
    @ResponseBody
    public Map<String, Object> getSubPrice(
            @RequestParam String subjectId,
            @RequestParam String subjectSubId) {
        return kndb2040Dao.getSubjectPrice(subjectId, subjectSubId);
    }

    /**
     * 执行修正：接收 AJAX 请求，触发 Spring Batch Job，返回 JSON 结果
     */
    @PostMapping("/kndb2040/execute")
    @ResponseBody
    public Map<String, Object> execute(
            @RequestParam String stuId,
            @RequestParam String subjectId,
            @RequestParam String currentSubjectSubId,
            @RequestParam String newSubjectSubId,
            @RequestParam double newLessonFee,
            @RequestParam(required = false, defaultValue = "0") double newLessonFeeAdjusted,
            @RequestParam int startYear,
            @RequestParam int startMonth) {

        String startYearMonth = String.format("%04d-%02d", startYear, startMonth);
        Map<String, Object> result = new HashMap<>();

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("baseDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                    .addString("jobMode", "MANUAL")
                    .addString("businessModule", "KNDB2040")
                    .addString("stuId", stuId)
                    .addString("subjectId", subjectId)
                    .addString("currentSubjectSubId", currentSubjectSubId)
                    .addString("newSubjectSubId", newSubjectSubId)
                    .addDouble("newLessonFee", newLessonFee)
                    .addDouble("newLessonFeeAdjusted", newLessonFeeAdjusted)
                    .addString("startYearMonth", startYearMonth)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(kndb2040Job, params);

            logger.info("KNDB2040 执行完了 - stuId={}, subjectId={}, 起价年月={}", stuId, subjectId, startYearMonth);
            result.put("success", true);
            result.put("message", "修正执行完了。起价年月：" + startYearMonth);

        } catch (Exception e) {
            logger.error("KNDB2040 执行失败 - stuId={}, subjectId={}, 起价年月={}, error={}",
                    stuId, subjectId, startYearMonth, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "修正执行失败：" + e.getMessage());
        }

        return result;
    }
}
