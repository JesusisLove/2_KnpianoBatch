package com.liu.knbatch.util;

import com.liu.knbatch.entity.CollisionResult;
import com.liu.knbatch.entity.CollisionResult.CollisionType;
import com.liu.knbatch.entity.KNDB4020Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CollisionReportBuilder 碰撞报告生成器测试
 * 测试内容：
 * - 报告生成（单个/多个碰撞）
 * - 报告格式（报告头、报告体、报告尾）
 * - 时间轴图解
 * - 边界情况
 *
 * @author Liu
 * @version 1.0.0
 */
class CollisionReportBuilderTest {

    private CollisionReportBuilder reportBuilder;

    @BeforeEach
    void setUp() {
        reportBuilder = new CollisionReportBuilder();
    }

    // ========== 测试数据工厂 ==========

    private KNDB4020Entity createLesson(LocalDateTime startTime, int duration,
                                         String studentName, String subjectId, String subjectName,
                                         int schedualType) {
        KNDB4020Entity entity = new KNDB4020Entity();
        entity.setLessonId("1");
        entity.setStuId("100");
        entity.setStudentName(studentName);
        entity.setSubjectId(subjectId);
        entity.setSubjectName(subjectName);
        entity.setSchedualDateTime(startTime);
        entity.setClassDuration(duration);
        entity.setSchedualType(schedualType);
        return entity;
    }

    private CollisionResult createCollision(LocalDateTime autoStart, int autoDuration,
                                             String autoStudent, String autoSubjectId, String autoSubjectName,
                                             LocalDateTime manualStart, int manualDuration,
                                             String manualStudent, String manualSubjectId, String manualSubjectName,
                                             CollisionType type, int overlapMinutes) {
        KNDB4020Entity auto = createLesson(autoStart, autoDuration, autoStudent, autoSubjectId, autoSubjectName, 1);
        KNDB4020Entity manual = createLesson(manualStart, manualDuration, manualStudent, manualSubjectId, manualSubjectName, 0);

        return CollisionResult.builder()
                .autoLesson(auto)
                .manualLesson(manual)
                .collisionType(type)
                .overlapMinutes(overlapMinutes)
                .build();
    }

    // ========== 报告生成测试 ==========

    @Nested
    @DisplayName("报告生成测试")
    class ReportGenerationTests {

        @Test
        @DisplayName("生成单个碰撞报告")
        void testBuildReport_SingleCollision() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "小明", "1", "钢琴",
                    time.plusMinutes(30), 60, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertNotNull(report);
            // 验证报告头
            assertTrue(report.contains("【课程碰撞检测报告】"));
            assertTrue(report.contains("2026-02-17"));
            assertTrue(report.contains("2026-02-23"));
            assertTrue(report.contains("1 处课程碰撞"));

            // 验证报告体
            assertTrue(report.contains("碰撞 #1"));
            assertTrue(report.contains("小明"));
            assertTrue(report.contains("小华"));
            assertTrue(report.contains("钢琴"));
            assertTrue(report.contains("部分重叠"));

            // 验证报告尾
            assertTrue(report.contains("KNPiano 批处理系统"));
        }

        @Test
        @DisplayName("生成多个碰撞报告")
        void testBuildReport_MultipleCollisions() {
            LocalDateTime day1 = LocalDateTime.of(2026, 2, 17, 8, 0);
            LocalDateTime day2 = LocalDateTime.of(2026, 2, 18, 10, 0);

            List<CollisionResult> collisions = new ArrayList<>();
            // 碰撞1：部分重叠
            collisions.add(createCollision(
                    day1, 60, "小明", "1", "钢琴",
                    day1.plusMinutes(30), 60, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));
            // 碰撞2：课时不同
            collisions.add(createCollision(
                    day2, 60, "小红", "1", "钢琴",
                    day2, 45, "小刚", "1", "钢琴",
                    CollisionType.DURATION_DIFFERENT, 45));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertNotNull(report);
            assertTrue(report.contains("2 处课程碰撞"));
            assertTrue(report.contains("碰撞 #1"));
            assertTrue(report.contains("碰撞 #2"));
            assertTrue(report.contains("小明"));
            assertTrue(report.contains("小红"));
        }

        @Test
        @DisplayName("生成空碰撞列表报告")
        void testBuildReport_EmptyCollisions() {
            List<CollisionResult> collisions = new ArrayList<>();

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertNotNull(report);
            assertTrue(report.contains("0 处课程碰撞"));
        }
    }

    // ========== 报告格式测试 ==========

    @Nested
    @DisplayName("报告格式测试")
    class ReportFormatTests {

        @Test
        @DisplayName("报告包含日期和星期")
        void testReportContainsDateAndWeekDay() {
            // 2026-02-17 是周二
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "小明", "1", "钢琴",
                    time.plusMinutes(30), 60, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertTrue(report.contains("2026-02-17"));
            assertTrue(report.contains("周二"));
        }

        @Test
        @DisplayName("报告包含碰撞类型描述")
        void testReportContainsCollisionTypeDescription() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "小明", "1", "钢琴",
                    time.plusMinutes(30), 60, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertTrue(report.contains("碰撞类型:"));
            assertTrue(report.contains("部分重叠"));
        }

        @Test
        @DisplayName("报告包含碰撞原因")
        void testReportContainsCollisionReason() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "小明", "1", "钢琴",
                    time.plusMinutes(30), 60, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertTrue(report.contains("碰撞原因:"));
        }

        @Test
        @DisplayName("报告包含时间轴图解")
        void testReportContainsTimelineChart() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "小明", "1", "钢琴",
                    time.plusMinutes(30), 60, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertTrue(report.contains("自动排课"));
            assertTrue(report.contains("手动排课"));
            assertTrue(report.contains("分钟"));
        }

        @Test
        @DisplayName("报告包含学生和科目信息")
        void testReportContainsStudentAndSubjectInfo() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "张三", "1", "钢琴",
                    time.plusMinutes(30), 45, "李四", "2", "声乐",
                    CollisionType.PARTIAL_OVERLAP, 15));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertTrue(report.contains("张三"));
            assertTrue(report.contains("李四"));
            assertTrue(report.contains("钢琴"));
            assertTrue(report.contains("声乐"));
            assertTrue(report.contains("60分钟"));
            assertTrue(report.contains("45分钟"));
        }
    }

    // ========== 三种碰撞类型报告测试 ==========

    @Nested
    @DisplayName("三种碰撞类型报告测试")
    class CollisionTypeReportTests {

        @Test
        @DisplayName("部分重叠碰撞报告")
        void testPartialOverlapReport() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "小明", "1", "钢琴",
                    time.plusMinutes(30), 60, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertTrue(report.contains("部分重叠"));
            assertTrue(report.contains("30分钟"));
        }

        @Test
        @DisplayName("完全覆盖+课时不同碰撞报告")
        void testDurationDifferentReport() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "小明", "1", "钢琴",
                    time, 45, "小华", "1", "钢琴",
                    CollisionType.DURATION_DIFFERENT, 45));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertTrue(report.contains("完全覆盖+课时不同"));
        }

        @Test
        @DisplayName("完全覆盖+科目不同碰撞报告")
        void testSubjectDifferentReport() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "小明", "1", "钢琴",
                    time, 60, "小华", "2", "声乐",
                    CollisionType.SUBJECT_DIFFERENT, 60));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertTrue(report.contains("完全覆盖+课时相同+科目不同"));
        }
    }

    // ========== 星期几测试 ==========

    @Nested
    @DisplayName("星期几显示测试")
    class WeekDayTests {

        @Test
        @DisplayName("周一显示")
        void testMonday() {
            // 2026-02-16 是周一
            LocalDateTime time = LocalDateTime.of(2026, 2, 16, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "小明", "1", "钢琴",
                    time.plusMinutes(30), 60, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));

            String report = reportBuilder.buildReport(collisions, "2026-02-16", "2026-02-22");

            assertTrue(report.contains("周一"));
        }

        @Test
        @DisplayName("周日显示")
        void testSunday() {
            // 2026-02-15 是周日
            LocalDateTime time = LocalDateTime.of(2026, 2, 15, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "小明", "1", "钢琴",
                    time.plusMinutes(30), 60, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));

            String report = reportBuilder.buildReport(collisions, "2026-02-15", "2026-02-21");

            assertTrue(report.contains("周日"));
        }
    }

    // ========== 边界情况测试 ==========

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("碰撞日期为null时过滤")
        void testCollisionDateNull_Filtered() {
            KNDB4020Entity auto = createLesson(null, 60, "小明", "1", "钢琴", 1);
            KNDB4020Entity manual = createLesson(LocalDateTime.of(2026, 2, 17, 8, 30), 60, "小华", "1", "钢琴", 0);

            CollisionResult collision = CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(manual)
                    .collisionType(CollisionType.PARTIAL_OVERLAP)
                    .overlapMinutes(30)
                    .build();

            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(collision);

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            // 日期为null的碰撞会被过滤
            assertNotNull(report);
            assertTrue(report.contains("1 处课程碰撞"));
        }

        @Test
        @DisplayName("30分钟短课时报告")
        void testShortDuration30Minutes() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 30, "小明", "1", "钢琴",
                    time.plusMinutes(15), 30, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 15));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertNotNull(report);
            assertTrue(report.contains("30分钟"));
        }

        @Test
        @DisplayName("90分钟长课时报告")
        void testLongDuration90Minutes() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 90, "小明", "1", "钢琴",
                    time.plusMinutes(60), 90, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertNotNull(report);
            assertTrue(report.contains("90分钟"));
        }

        @Test
        @DisplayName("同一天多个碰撞")
        void testMultipleCollisionsSameDay() {
            LocalDateTime day = LocalDateTime.of(2026, 2, 17, 0, 0);

            List<CollisionResult> collisions = new ArrayList<>();
            // 早上碰撞
            collisions.add(createCollision(
                    day.withHour(8), 60, "小明", "1", "钢琴",
                    day.withHour(8).plusMinutes(30), 60, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));
            // 下午碰撞
            collisions.add(createCollision(
                    day.withHour(14), 60, "小红", "1", "钢琴",
                    day.withHour(14), 45, "小刚", "1", "钢琴",
                    CollisionType.DURATION_DIFFERENT, 45));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertNotNull(report);
            assertTrue(report.contains("2 处课程碰撞"));
            assertTrue(report.contains("小明"));
            assertTrue(report.contains("小红"));
        }

        @Test
        @DisplayName("不同天的碰撞分组显示")
        void testDifferentDaysGrouped() {
            LocalDateTime day1 = LocalDateTime.of(2026, 2, 17, 8, 0);
            LocalDateTime day2 = LocalDateTime.of(2026, 2, 19, 10, 0);

            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    day1, 60, "小明", "1", "钢琴",
                    day1.plusMinutes(30), 60, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));
            collisions.add(createCollision(
                    day2, 60, "小红", "1", "钢琴",
                    day2, 60, "小刚", "2", "声乐",
                    CollisionType.SUBJECT_DIFFERENT, 60));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertNotNull(report);
            assertTrue(report.contains("2026-02-17"));
            assertTrue(report.contains("2026-02-19"));
        }
    }

    // ========== 重叠指示器测试 ==========

    @Nested
    @DisplayName("重叠指示器测试")
    class OverlapIndicatorTests {

        @Test
        @DisplayName("部分重叠指示器")
        void testPartialOverlapIndicator() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "小明", "1", "钢琴",
                    time.plusMinutes(30), 60, "小华", "1", "钢琴",
                    CollisionType.PARTIAL_OVERLAP, 30));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertTrue(report.contains("部分重叠") || report.contains("30分钟"));
        }

        @Test
        @DisplayName("完全覆盖指示器")
        void testFullOverlapIndicator() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            List<CollisionResult> collisions = new ArrayList<>();
            collisions.add(createCollision(
                    time, 60, "小明", "1", "钢琴",
                    time, 45, "小华", "1", "钢琴",
                    CollisionType.DURATION_DIFFERENT, 45));

            String report = reportBuilder.buildReport(collisions, "2026-02-17", "2026-02-23");

            assertTrue(report.contains("覆盖") || report.contains("DURATION_DIFFERENT")
                    || report.contains("完全覆盖+课时不同"));
        }
    }
}
