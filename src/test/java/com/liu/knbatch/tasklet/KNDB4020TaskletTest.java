package com.liu.knbatch.tasklet;

import com.liu.knbatch.entity.CollisionResult;
import com.liu.knbatch.entity.CollisionResult.CollisionType;
import com.liu.knbatch.entity.KNDB4020Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KNDB4020Tasklet 碰撞检测逻辑测试
 * 覆盖所有业务场景：
 * - 3种碰撞警告场景
 * - 1种忽略场景（集体上课）
 * - 无碰撞场景
 * - 边界情况
 *
 * @author Liu
 * @version 1.0.0
 */
class KNDB4020TaskletTest {

    private KNDB4020Tasklet tasklet;
    private Method detectCollisionsMethod;
    private Method checkCollisionMethod;
    private Method isPartialOverlapMethod;
    private Method calculateOverlapMinutesMethod;

    @BeforeEach
    void setUp() throws Exception {
        tasklet = new KNDB4020Tasklet();

        // 通过反射获取私有方法
        detectCollisionsMethod = KNDB4020Tasklet.class.getDeclaredMethod(
                "detectCollisions", List.class, List.class);
        detectCollisionsMethod.setAccessible(true);

        checkCollisionMethod = KNDB4020Tasklet.class.getDeclaredMethod(
                "checkCollision", KNDB4020Entity.class, KNDB4020Entity.class);
        checkCollisionMethod.setAccessible(true);

        isPartialOverlapMethod = KNDB4020Tasklet.class.getDeclaredMethod(
                "isPartialOverlap", LocalDateTime.class, LocalDateTime.class,
                LocalDateTime.class, LocalDateTime.class);
        isPartialOverlapMethod.setAccessible(true);

        calculateOverlapMinutesMethod = KNDB4020Tasklet.class.getDeclaredMethod(
                "calculateOverlapMinutes", LocalDateTime.class, LocalDateTime.class,
                LocalDateTime.class, LocalDateTime.class);
        calculateOverlapMinutesMethod.setAccessible(true);
    }

    // ========== 测试数据工厂方法 ==========

    /**
     * 创建自动排课实体
     */
    private KNDB4020Entity createAutoLesson(LocalDateTime startTime, int duration,
                                             String studentName, String subjectId, String subjectName) {
        KNDB4020Entity entity = new KNDB4020Entity();
        entity.setLessonId("1");
        entity.setStuId("100");
        entity.setStudentName(studentName);
        entity.setSubjectId(subjectId);
        entity.setSubjectName(subjectName);
        entity.setSchedualDateTime(startTime);
        entity.setClassDuration(duration);
        entity.setSchedualType(1); // 自动排课
        return entity;
    }

    /**
     * 创建手动排课实体
     */
    private KNDB4020Entity createManualLesson(LocalDateTime startTime, int duration,
                                               String studentName, String subjectId, String subjectName) {
        KNDB4020Entity entity = new KNDB4020Entity();
        entity.setLessonId("2");
        entity.setStuId("200");
        entity.setStudentName(studentName);
        entity.setSubjectId(subjectId);
        entity.setSubjectName(subjectName);
        entity.setSchedualDateTime(startTime);
        entity.setClassDuration(duration);
        entity.setSchedualType(0); // 手动排课
        return entity;
    }

    // ========== 碰撞场景1：部分重叠（PARTIAL_OVERLAP） ==========

    @Nested
    @DisplayName("碰撞场景1: 部分重叠（PARTIAL_OVERLAP）")
    class PartialOverlapTests {

        @Test
        @DisplayName("自动排课在前，手动排课在后，重叠30分钟")
        void testAutoBeforeManual_Overlap30Minutes() throws Exception {
            // 自动排课: 08:00-09:00
            // 手动排课: 08:30-09:30
            // 预期: 部分重叠30分钟
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(30), 60,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.PARTIAL_OVERLAP, result.getCollisionType());
            assertEquals(30, result.getOverlapMinutes());
        }

        @Test
        @DisplayName("手动排课在前，自动排课在后，重叠30分钟")
        void testManualBeforeAuto_Overlap30Minutes() throws Exception {
            // 手动排课: 08:00-09:00
            // 自动排课: 08:30-09:30
            // 预期: 部分重叠30分钟
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(30), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.PARTIAL_OVERLAP, result.getCollisionType());
            assertEquals(30, result.getOverlapMinutes());
        }

        @Test
        @DisplayName("自动排课在前，手动排课在后，重叠15分钟")
        void testAutoBeforeManual_Overlap15Minutes() throws Exception {
            // 自动排课: 08:00-08:45 (45分钟)
            // 手动排课: 08:30-09:30 (60分钟)
            // 预期: 部分重叠15分钟
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 45,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(30), 60,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.PARTIAL_OVERLAP, result.getCollisionType());
            assertEquals(15, result.getOverlapMinutes());
        }

        @Test
        @DisplayName("自动排课完全包含手动排课（开始时刻不同）")
        void testAutoContainsManual() throws Exception {
            // 自动排课: 08:00-10:00 (120分钟)
            // 手动排课: 08:30-09:30 (60分钟)
            // 预期: 部分重叠60分钟
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 120,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(30), 60,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.PARTIAL_OVERLAP, result.getCollisionType());
            assertEquals(60, result.getOverlapMinutes());
        }

        @Test
        @DisplayName("手动排课完全包含自动排课（开始时刻不同）")
        void testManualContainsAuto() throws Exception {
            // 手动排课: 08:00-10:00 (120分钟)
            // 自动排课: 08:30-09:30 (60分钟)
            // 预期: 部分重叠60分钟
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(30), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(0), 120,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.PARTIAL_OVERLAP, result.getCollisionType());
            assertEquals(60, result.getOverlapMinutes());
        }
    }

    // ========== 碰撞场景2：完全覆盖+课时不同（DURATION_DIFFERENT） ==========

    @Nested
    @DisplayName("碰撞场景2: 完全覆盖+课时不同（DURATION_DIFFERENT）")
    class DurationDifferentTests {

        @Test
        @DisplayName("相同开始时刻，自动60分钟，手动45分钟")
        void testSameStartTime_Auto60_Manual45() throws Exception {
            // 自动排课: 08:00-09:00 (60分钟)
            // 手动排课: 08:00-08:45 (45分钟)
            // 预期: 完全覆盖+课时不同
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(0), 45,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.DURATION_DIFFERENT, result.getCollisionType());
            assertEquals(45, result.getOverlapMinutes()); // 取较短的
        }

        @Test
        @DisplayName("相同开始时刻，自动45分钟，手动60分钟")
        void testSameStartTime_Auto45_Manual60() throws Exception {
            // 自动排课: 08:00-08:45 (45分钟)
            // 手动排课: 08:00-09:00 (60分钟)
            // 预期: 完全覆盖+课时不同
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 45,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.DURATION_DIFFERENT, result.getCollisionType());
            assertEquals(45, result.getOverlapMinutes()); // 取较短的
        }

        @Test
        @DisplayName("相同开始时刻，自动30分钟，手动60分钟")
        void testSameStartTime_Auto30_Manual60() throws Exception {
            // 自动排课: 08:00-08:30 (30分钟)
            // 手动排课: 08:00-09:00 (60分钟)
            // 预期: 完全覆盖+课时不同
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 30,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.DURATION_DIFFERENT, result.getCollisionType());
            assertEquals(30, result.getOverlapMinutes()); // 取较短的
        }

        @Test
        @DisplayName("相同开始时刻，自动60分钟，手动90分钟，科目不同")
        void testSameStartTime_DifferentDuration_DifferentSubject() throws Exception {
            // 自动排课: 08:00-09:00 (60分钟) 钢琴
            // 手动排课: 08:00-09:30 (90分钟) 声乐
            // 预期: 完全覆盖+课时不同（优先判断课时）
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(0), 90,
                    "小华", "2", "声乐");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.DURATION_DIFFERENT, result.getCollisionType());
        }
    }

    // ========== 碰撞场景3：完全覆盖+课时相同+科目不同（SUBJECT_DIFFERENT） ==========

    @Nested
    @DisplayName("碰撞场景3: 完全覆盖+课时相同+科目不同（SUBJECT_DIFFERENT）")
    class SubjectDifferentTests {

        @Test
        @DisplayName("相同时间+相同课时+科目不同（钢琴 vs 声乐）")
        void testSameTimeAndDuration_DifferentSubject_PianoVsVocal() throws Exception {
            // 自动排课: 08:00-09:00 (60分钟) 钢琴
            // 手动排课: 08:00-09:00 (60分钟) 声乐
            // 预期: 科目不同
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小华", "2", "声乐");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.SUBJECT_DIFFERENT, result.getCollisionType());
            assertEquals(60, result.getOverlapMinutes());
        }

        @Test
        @DisplayName("相同时间+相同课时+科目不同（钢琴 vs 小提琴）")
        void testSameTimeAndDuration_DifferentSubject_PianoVsViolin() throws Exception {
            // 自动排课: 10:00-10:45 (45分钟) 钢琴
            // 手动排课: 10:00-10:45 (45分钟) 小提琴
            // 预期: 科目不同
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(10).withMinute(0), 45,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(10).withMinute(0), 45,
                    "小华", "3", "小提琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.SUBJECT_DIFFERENT, result.getCollisionType());
            assertEquals(45, result.getOverlapMinutes());
        }
    }

    // ========== 忽略场景：集体上课（同时间+同课时+同科目） ==========

    @Nested
    @DisplayName("忽略场景: 集体上课（同时间+同课时+同科目）")
    class GroupLessonIgnoreTests {

        @Test
        @DisplayName("完全相同时间+相同课时+相同科目，不同学生（集体上课）")
        void testGroupLesson_SameTimeAndSubject_DifferentStudent() throws Exception {
            // 自动排课: 08:00-09:00 (60分钟) 钢琴 小明
            // 手动排课: 08:00-09:00 (60分钟) 钢琴 小华
            // 预期: 无碰撞（集体上课）
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNull(result, "集体上课场景不应检测为碰撞");
        }

        @Test
        @DisplayName("完全相同时间+相同课时+相同科目，同一学生（调课场景）")
        void testSameStudent_SameTimeAndSubject() throws Exception {
            // 自动排课: 08:00-09:00 (60分钟) 钢琴 小明
            // 手动排课: 08:00-09:00 (60分钟) 钢琴 小明
            // 预期: 无碰撞（可能是正常的调课确认）
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小明", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNull(result, "同学生同时间同科目不应检测为碰撞");
        }
    }

    // ========== 无碰撞场景 ==========

    @Nested
    @DisplayName("无碰撞场景")
    class NoCollisionTests {

        @Test
        @DisplayName("不同日期，无碰撞")
        void testDifferentDate_NoCollision() throws Exception {
            // 自动排课: 2026-02-17 08:00-09:00
            // 手动排课: 2026-02-18 08:00-09:00
            // 预期: 无碰撞
            KNDB4020Entity auto = createAutoLesson(
                    LocalDateTime.of(2026, 2, 17, 8, 0), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    LocalDateTime.of(2026, 2, 18, 8, 0), 60,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNull(result, "不同日期不应检测为碰撞");
        }

        @Test
        @DisplayName("同一日期，时间不重叠（相邻）")
        void testSameDate_AdjacentTime_NoCollision() throws Exception {
            // 自动排课: 08:00-09:00 (60分钟)
            // 手动排课: 09:00-10:00 (60分钟)
            // 预期: 无碰撞（刚好相邻）
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(9).withMinute(0), 60,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNull(result, "时间相邻不应检测为碰撞");
        }

        @Test
        @DisplayName("同一日期，时间完全分离")
        void testSameDate_SeparateTime_NoCollision() throws Exception {
            // 自动排课: 08:00-09:00 (60分钟)
            // 手动排课: 14:00-15:00 (60分钟)
            // 预期: 无碰撞
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(14).withMinute(0), 60,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNull(result, "时间完全分离不应检测为碰撞");
        }

        @Test
        @DisplayName("同一日期，手动排课在自动排课之前，刚好相邻")
        void testSameDate_ManualBeforeAuto_Adjacent_NoCollision() throws Exception {
            // 手动排课: 07:00-08:00 (60分钟)
            // 自动排课: 08:00-09:00 (60分钟)
            // 预期: 无碰撞（刚好相邻）
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(7).withMinute(0), 60,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNull(result, "时间相邻不应检测为碰撞");
        }
    }

    // ========== 边界情况测试 ==========

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("空列表检测")
        void testEmptyLists() throws Exception {
            List<KNDB4020Entity> autoLessons = new ArrayList<>();
            List<KNDB4020Entity> manualLessons = new ArrayList<>();

            @SuppressWarnings("unchecked")
            List<CollisionResult> results = (List<CollisionResult>)
                    detectCollisionsMethod.invoke(tasklet, autoLessons, manualLessons);

            assertNotNull(results);
            assertTrue(results.isEmpty(), "空列表应返回空碰撞列表");
        }

        @Test
        @DisplayName("自动排课列表为空")
        void testEmptyAutoLessons() throws Exception {
            List<KNDB4020Entity> autoLessons = new ArrayList<>();
            List<KNDB4020Entity> manualLessons = new ArrayList<>();
            manualLessons.add(createManualLesson(
                    LocalDateTime.of(2026, 2, 17, 8, 0), 60,
                    "小华", "1", "钢琴"));

            @SuppressWarnings("unchecked")
            List<CollisionResult> results = (List<CollisionResult>)
                    detectCollisionsMethod.invoke(tasklet, autoLessons, manualLessons);

            assertNotNull(results);
            assertTrue(results.isEmpty(), "自动排课为空应返回空碰撞列表");
        }

        @Test
        @DisplayName("手动排课列表为空")
        void testEmptyManualLessons() throws Exception {
            List<KNDB4020Entity> autoLessons = new ArrayList<>();
            autoLessons.add(createAutoLesson(
                    LocalDateTime.of(2026, 2, 17, 8, 0), 60,
                    "小明", "1", "钢琴"));
            List<KNDB4020Entity> manualLessons = new ArrayList<>();

            @SuppressWarnings("unchecked")
            List<CollisionResult> results = (List<CollisionResult>)
                    detectCollisionsMethod.invoke(tasklet, autoLessons, manualLessons);

            assertNotNull(results);
            assertTrue(results.isEmpty(), "手动排课为空应返回空碰撞列表");
        }

        @Test
        @DisplayName("自动排课时间为null")
        void testAutoLessonTimeNull() throws Exception {
            KNDB4020Entity auto = createAutoLesson(null, 60, "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    LocalDateTime.of(2026, 2, 17, 8, 0), 60,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNull(result, "时间为null时应返回null");
        }

        @Test
        @DisplayName("手动排课时间为null")
        void testManualLessonTimeNull() throws Exception {
            KNDB4020Entity auto = createAutoLesson(
                    LocalDateTime.of(2026, 2, 17, 8, 0), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(null, 60, "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNull(result, "时间为null时应返回null");
        }

        @Test
        @DisplayName("重叠时间刚好为1分钟")
        void testOverlap1Minute() throws Exception {
            // 自动排课: 08:00-09:00 (60分钟)
            // 手动排课: 08:59-10:00 (61分钟)
            // 预期: 部分重叠1分钟
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(59), 61,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.PARTIAL_OVERLAP, result.getCollisionType());
            assertEquals(1, result.getOverlapMinutes());
        }

        @Test
        @DisplayName("30分钟短课时测试")
        void testShortDuration30Minutes() throws Exception {
            // 自动排课: 08:00-08:30 (30分钟)
            // 手动排课: 08:15-08:45 (30分钟)
            // 预期: 部分重叠15分钟
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            KNDB4020Entity auto = createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 30,
                    "小明", "1", "钢琴");
            KNDB4020Entity manual = createManualLesson(
                    baseDate.withHour(8).withMinute(15), 30,
                    "小华", "1", "钢琴");

            CollisionResult result = (CollisionResult) checkCollisionMethod.invoke(tasklet, auto, manual);

            assertNotNull(result, "应检测到碰撞");
            assertEquals(CollisionType.PARTIAL_OVERLAP, result.getCollisionType());
            assertEquals(15, result.getOverlapMinutes());
        }
    }

    // ========== 批量检测测试 ==========

    @Nested
    @DisplayName("批量检测测试")
    class BatchDetectionTests {

        @Test
        @DisplayName("多个自动排课与多个手动排课检测")
        void testMultipleCollisions() throws Exception {
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            List<KNDB4020Entity> autoLessons = new ArrayList<>();
            // 自动排课1: 08:00-09:00
            autoLessons.add(createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 60,
                    "小明", "1", "钢琴"));
            // 自动排课2: 10:00-11:00
            autoLessons.add(createAutoLesson(
                    baseDate.withHour(10).withMinute(0), 60,
                    "小红", "1", "钢琴"));
            // 自动排课3: 14:00-15:00
            autoLessons.add(createAutoLesson(
                    baseDate.withHour(14).withMinute(0), 60,
                    "小刚", "1", "钢琴"));

            List<KNDB4020Entity> manualLessons = new ArrayList<>();
            // 手动排课1: 08:30-09:30 (与自动1重叠)
            manualLessons.add(createManualLesson(
                    baseDate.withHour(8).withMinute(30), 60,
                    "小华", "1", "钢琴"));
            // 手动排课2: 10:00-11:00 (与自动2完全相同，集体上课)
            manualLessons.add(createManualLesson(
                    baseDate.withHour(10).withMinute(0), 60,
                    "小李", "1", "钢琴"));
            // 手动排课3: 16:00-17:00 (无碰撞)
            manualLessons.add(createManualLesson(
                    baseDate.withHour(16).withMinute(0), 60,
                    "小王", "1", "钢琴"));

            @SuppressWarnings("unchecked")
            List<CollisionResult> results = (List<CollisionResult>)
                    detectCollisionsMethod.invoke(tasklet, autoLessons, manualLessons);

            // 应该只有1个碰撞（08:30-09:30与08:00-09:00部分重叠）
            // 10:00-11:00完全相同是集体上课，不算碰撞
            assertEquals(1, results.size(), "应检测到1个碰撞");
            assertEquals(CollisionType.PARTIAL_OVERLAP, results.get(0).getCollisionType());
        }

        @Test
        @DisplayName("一个自动排课与多个手动排课碰撞")
        void testOneAutoMultipleManualCollisions() throws Exception {
            LocalDateTime baseDate = LocalDateTime.of(2026, 2, 17, 0, 0);

            List<KNDB4020Entity> autoLessons = new ArrayList<>();
            // 自动排课: 08:00-10:00 (120分钟)
            autoLessons.add(createAutoLesson(
                    baseDate.withHour(8).withMinute(0), 120,
                    "小明", "1", "钢琴"));

            List<KNDB4020Entity> manualLessons = new ArrayList<>();
            // 手动排课1: 08:30-09:00 (部分重叠)
            manualLessons.add(createManualLesson(
                    baseDate.withHour(8).withMinute(30), 30,
                    "小华", "1", "钢琴"));
            // 手动排课2: 09:00-09:30 (部分重叠)
            manualLessons.add(createManualLesson(
                    baseDate.withHour(9).withMinute(0), 30,
                    "小李", "2", "声乐"));

            @SuppressWarnings("unchecked")
            List<CollisionResult> results = (List<CollisionResult>)
                    detectCollisionsMethod.invoke(tasklet, autoLessons, manualLessons);

            assertEquals(2, results.size(), "应检测到2个碰撞");
        }
    }

    // ========== 工具方法测试 ==========

    @Nested
    @DisplayName("工具方法测试")
    class UtilityMethodTests {

        @Test
        @DisplayName("计算重叠时长 - 标准情况")
        void testCalculateOverlapMinutes_Standard() throws Exception {
            LocalDateTime start1 = LocalDateTime.of(2026, 2, 17, 8, 0);
            LocalDateTime end1 = LocalDateTime.of(2026, 2, 17, 9, 0);
            LocalDateTime start2 = LocalDateTime.of(2026, 2, 17, 8, 30);
            LocalDateTime end2 = LocalDateTime.of(2026, 2, 17, 9, 30);

            int overlap = (int) calculateOverlapMinutesMethod.invoke(tasklet, start1, end1, start2, end2);

            assertEquals(30, overlap);
        }

        @Test
        @DisplayName("计算重叠时长 - 完全包含")
        void testCalculateOverlapMinutes_FullyContained() throws Exception {
            LocalDateTime start1 = LocalDateTime.of(2026, 2, 17, 8, 0);
            LocalDateTime end1 = LocalDateTime.of(2026, 2, 17, 10, 0);
            LocalDateTime start2 = LocalDateTime.of(2026, 2, 17, 8, 30);
            LocalDateTime end2 = LocalDateTime.of(2026, 2, 17, 9, 30);

            int overlap = (int) calculateOverlapMinutesMethod.invoke(tasklet, start1, end1, start2, end2);

            assertEquals(60, overlap);
        }

        @Test
        @DisplayName("判断部分重叠 - 有重叠且开始时刻不同")
        void testIsPartialOverlap_True() throws Exception {
            LocalDateTime start1 = LocalDateTime.of(2026, 2, 17, 8, 0);
            LocalDateTime end1 = LocalDateTime.of(2026, 2, 17, 9, 0);
            LocalDateTime start2 = LocalDateTime.of(2026, 2, 17, 8, 30);
            LocalDateTime end2 = LocalDateTime.of(2026, 2, 17, 9, 30);

            boolean isPartial = (boolean) isPartialOverlapMethod.invoke(tasklet, start1, end1, start2, end2);

            assertTrue(isPartial);
        }

        @Test
        @DisplayName("判断部分重叠 - 开始时刻相同（非部分重叠）")
        void testIsPartialOverlap_SameStart_False() throws Exception {
            LocalDateTime start1 = LocalDateTime.of(2026, 2, 17, 8, 0);
            LocalDateTime end1 = LocalDateTime.of(2026, 2, 17, 9, 0);
            LocalDateTime start2 = LocalDateTime.of(2026, 2, 17, 8, 0);
            LocalDateTime end2 = LocalDateTime.of(2026, 2, 17, 9, 30);

            boolean isPartial = (boolean) isPartialOverlapMethod.invoke(tasklet, start1, end1, start2, end2);

            assertFalse(isPartial, "开始时刻相同不算部分重叠");
        }

        @Test
        @DisplayName("判断部分重叠 - 无重叠")
        void testIsPartialOverlap_NoOverlap_False() throws Exception {
            LocalDateTime start1 = LocalDateTime.of(2026, 2, 17, 8, 0);
            LocalDateTime end1 = LocalDateTime.of(2026, 2, 17, 9, 0);
            LocalDateTime start2 = LocalDateTime.of(2026, 2, 17, 10, 0);
            LocalDateTime end2 = LocalDateTime.of(2026, 2, 17, 11, 0);

            boolean isPartial = (boolean) isPartialOverlapMethod.invoke(tasklet, start1, end1, start2, end2);

            assertFalse(isPartial);
        }
    }
}
