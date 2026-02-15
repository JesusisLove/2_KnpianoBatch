package com.liu.knbatch.entity;

import com.liu.knbatch.entity.CollisionResult.CollisionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CollisionResult 碰撞结果实体类测试
 * 测试内容：
 * - Builder模式
 * - 工具方法（getCollisionDate, getCollisionReason）
 * - 枚举类型
 *
 * @author Liu
 * @version 1.0.0
 */
class CollisionResultTest {

    // ========== 测试数据工厂 ==========

    private KNDB4020Entity createLesson(LocalDateTime startTime, int duration,
                                         String studentName, String subjectId, String subjectName) {
        KNDB4020Entity entity = new KNDB4020Entity();
        entity.setLessonId("1");
        entity.setStuId("100");
        entity.setStudentName(studentName);
        entity.setSubjectId(subjectId);
        entity.setSubjectName(subjectName);
        entity.setSchedualDateTime(startTime);
        entity.setClassDuration(duration);
        return entity;
    }

    // ========== Builder模式测试 ==========

    @Nested
    @DisplayName("Builder模式测试")
    class BuilderTests {

        @Test
        @DisplayName("使用Builder创建碰撞结果 - 部分重叠")
        void testBuilder_PartialOverlap() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            KNDB4020Entity auto = createLesson(time, 60, "小明", "1", "钢琴");
            KNDB4020Entity manual = createLesson(time.plusMinutes(30), 60, "小华", "1", "钢琴");

            CollisionResult result = CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(manual)
                    .collisionType(CollisionType.PARTIAL_OVERLAP)
                    .overlapMinutes(30)
                    .build();

            assertNotNull(result);
            assertEquals(auto, result.getAutoLesson());
            assertEquals(manual, result.getManualLesson());
            assertEquals(CollisionType.PARTIAL_OVERLAP, result.getCollisionType());
            assertEquals(30, result.getOverlapMinutes());
        }

        @Test
        @DisplayName("使用Builder创建碰撞结果 - 课时不同")
        void testBuilder_DurationDifferent() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            KNDB4020Entity auto = createLesson(time, 60, "小明", "1", "钢琴");
            KNDB4020Entity manual = createLesson(time, 45, "小华", "1", "钢琴");

            CollisionResult result = CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(manual)
                    .collisionType(CollisionType.DURATION_DIFFERENT)
                    .overlapMinutes(45)
                    .build();

            assertEquals(CollisionType.DURATION_DIFFERENT, result.getCollisionType());
        }

        @Test
        @DisplayName("使用Builder创建碰撞结果 - 科目不同")
        void testBuilder_SubjectDifferent() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            KNDB4020Entity auto = createLesson(time, 60, "小明", "1", "钢琴");
            KNDB4020Entity manual = createLesson(time, 60, "小华", "2", "声乐");

            CollisionResult result = CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(manual)
                    .collisionType(CollisionType.SUBJECT_DIFFERENT)
                    .overlapMinutes(60)
                    .build();

            assertEquals(CollisionType.SUBJECT_DIFFERENT, result.getCollisionType());
        }

        @Test
        @DisplayName("使用构造方法创建碰撞结果")
        void testConstructor() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            KNDB4020Entity auto = createLesson(time, 60, "小明", "1", "钢琴");
            KNDB4020Entity manual = createLesson(time.plusMinutes(30), 60, "小华", "1", "钢琴");

            CollisionResult result = new CollisionResult(auto, manual, CollisionType.PARTIAL_OVERLAP, 30);

            assertEquals(auto, result.getAutoLesson());
            assertEquals(manual, result.getManualLesson());
            assertEquals(CollisionType.PARTIAL_OVERLAP, result.getCollisionType());
            assertEquals(30, result.getOverlapMinutes());
        }

        @Test
        @DisplayName("使用默认构造方法和Setter")
        void testDefaultConstructorAndSetters() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            KNDB4020Entity auto = createLesson(time, 60, "小明", "1", "钢琴");
            KNDB4020Entity manual = createLesson(time.plusMinutes(30), 60, "小华", "1", "钢琴");

            CollisionResult result = new CollisionResult();
            result.setAutoLesson(auto);
            result.setManualLesson(manual);
            result.setCollisionType(CollisionType.PARTIAL_OVERLAP);
            result.setOverlapMinutes(30);

            assertEquals(auto, result.getAutoLesson());
            assertEquals(manual, result.getManualLesson());
            assertEquals(CollisionType.PARTIAL_OVERLAP, result.getCollisionType());
            assertEquals(30, result.getOverlapMinutes());
        }
    }

    // ========== getCollisionDate 测试 ==========

    @Nested
    @DisplayName("getCollisionDate 测试")
    class GetCollisionDateTests {

        @Test
        @DisplayName("正常获取碰撞日期")
        void testGetCollisionDate_Normal() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            KNDB4020Entity auto = createLesson(time, 60, "小明", "1", "钢琴");
            KNDB4020Entity manual = createLesson(time.plusMinutes(30), 60, "小华", "1", "钢琴");

            CollisionResult result = CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(manual)
                    .collisionType(CollisionType.PARTIAL_OVERLAP)
                    .overlapMinutes(30)
                    .build();

            LocalDate date = result.getCollisionDate();

            assertNotNull(date);
            assertEquals(LocalDate.of(2026, 2, 17), date);
        }

        @Test
        @DisplayName("autoLesson为null时返回null")
        void testGetCollisionDate_AutoLessonNull() {
            CollisionResult result = CollisionResult.builder()
                    .autoLesson(null)
                    .manualLesson(createLesson(LocalDateTime.now(), 60, "小华", "1", "钢琴"))
                    .collisionType(CollisionType.PARTIAL_OVERLAP)
                    .build();

            assertNull(result.getCollisionDate());
        }

        @Test
        @DisplayName("autoLesson时间为null时返回null")
        void testGetCollisionDate_AutoLessonTimeNull() {
            KNDB4020Entity auto = createLesson(null, 60, "小明", "1", "钢琴");

            CollisionResult result = CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(createLesson(LocalDateTime.now(), 60, "小华", "1", "钢琴"))
                    .collisionType(CollisionType.PARTIAL_OVERLAP)
                    .build();

            assertNull(result.getCollisionDate());
        }
    }

    // ========== getCollisionReason 测试 ==========

    @Nested
    @DisplayName("getCollisionReason 测试")
    class GetCollisionReasonTests {

        @Test
        @DisplayName("部分重叠原因描述")
        void testGetCollisionReason_PartialOverlap() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            KNDB4020Entity auto = createLesson(time, 60, "小明", "1", "钢琴");
            KNDB4020Entity manual = createLesson(time.plusMinutes(30), 60, "小华", "1", "钢琴");

            CollisionResult result = CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(manual)
                    .collisionType(CollisionType.PARTIAL_OVERLAP)
                    .overlapMinutes(30)
                    .build();

            String reason = result.getCollisionReason();

            assertNotNull(reason);
            assertTrue(reason.contains("部分重叠"));
            assertTrue(reason.contains("30"));
        }

        @Test
        @DisplayName("课时不同原因描述")
        void testGetCollisionReason_DurationDifferent() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            KNDB4020Entity auto = createLesson(time, 60, "小明", "1", "钢琴");
            KNDB4020Entity manual = createLesson(time, 45, "小华", "1", "钢琴");

            CollisionResult result = CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(manual)
                    .collisionType(CollisionType.DURATION_DIFFERENT)
                    .overlapMinutes(45)
                    .build();

            String reason = result.getCollisionReason();

            assertNotNull(reason);
            assertTrue(reason.contains("课时不同"));
            assertTrue(reason.contains("60"));
            assertTrue(reason.contains("45"));
        }

        @Test
        @DisplayName("科目不同原因描述")
        void testGetCollisionReason_SubjectDifferent() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            KNDB4020Entity auto = createLesson(time, 60, "小明", "1", "钢琴");
            KNDB4020Entity manual = createLesson(time, 60, "小华", "2", "声乐");

            CollisionResult result = CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(manual)
                    .collisionType(CollisionType.SUBJECT_DIFFERENT)
                    .overlapMinutes(60)
                    .build();

            String reason = result.getCollisionReason();

            assertNotNull(reason);
            assertTrue(reason.contains("科目不同"));
            assertTrue(reason.contains("钢琴"));
            assertTrue(reason.contains("声乐"));
        }

        @Test
        @DisplayName("碰撞类型为null时返回未知")
        void testGetCollisionReason_TypeNull() {
            CollisionResult result = CollisionResult.builder()
                    .autoLesson(createLesson(LocalDateTime.now(), 60, "小明", "1", "钢琴"))
                    .manualLesson(createLesson(LocalDateTime.now(), 60, "小华", "1", "钢琴"))
                    .collisionType(null)
                    .build();

            String reason = result.getCollisionReason();

            assertEquals("未知碰撞类型", reason);
        }
    }

    // ========== CollisionType 枚举测试 ==========

    @Nested
    @DisplayName("CollisionType 枚举测试")
    class CollisionTypeTests {

        @Test
        @DisplayName("PARTIAL_OVERLAP 描述")
        void testPartialOverlapDescription() {
            assertEquals("部分重叠", CollisionType.PARTIAL_OVERLAP.getDescription());
        }

        @Test
        @DisplayName("DURATION_DIFFERENT 描述")
        void testDurationDifferentDescription() {
            assertEquals("完全覆盖+课时不同", CollisionType.DURATION_DIFFERENT.getDescription());
        }

        @Test
        @DisplayName("SUBJECT_DIFFERENT 描述")
        void testSubjectDifferentDescription() {
            assertEquals("完全覆盖+课时相同+科目不同", CollisionType.SUBJECT_DIFFERENT.getDescription());
        }

        @Test
        @DisplayName("枚举值数量检查")
        void testEnumValuesCount() {
            assertEquals(3, CollisionType.values().length);
        }

        @Test
        @DisplayName("枚举valueOf测试")
        void testEnumValueOf() {
            assertEquals(CollisionType.PARTIAL_OVERLAP, CollisionType.valueOf("PARTIAL_OVERLAP"));
            assertEquals(CollisionType.DURATION_DIFFERENT, CollisionType.valueOf("DURATION_DIFFERENT"));
            assertEquals(CollisionType.SUBJECT_DIFFERENT, CollisionType.valueOf("SUBJECT_DIFFERENT"));
        }
    }

    // ========== toString 测试 ==========

    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        @Test
        @DisplayName("toString包含关键信息")
        void testToString_ContainsKeyInfo() {
            LocalDateTime time = LocalDateTime.of(2026, 2, 17, 8, 0);
            KNDB4020Entity auto = createLesson(time, 60, "小明", "1", "钢琴");
            KNDB4020Entity manual = createLesson(time.plusMinutes(30), 60, "小华", "1", "钢琴");

            CollisionResult result = CollisionResult.builder()
                    .autoLesson(auto)
                    .manualLesson(manual)
                    .collisionType(CollisionType.PARTIAL_OVERLAP)
                    .overlapMinutes(30)
                    .build();

            String str = result.toString();

            assertNotNull(str);
            assertTrue(str.contains("小明"));
            assertTrue(str.contains("小华"));
            assertTrue(str.contains("钢琴"));
            assertTrue(str.contains("PARTIAL_OVERLAP"));
            assertTrue(str.contains("30"));
        }

        @Test
        @DisplayName("autoLesson为null时toString不报错")
        void testToString_AutoLessonNull() {
            CollisionResult result = CollisionResult.builder()
                    .autoLesson(null)
                    .manualLesson(createLesson(LocalDateTime.now(), 60, "小华", "1", "钢琴"))
                    .collisionType(CollisionType.PARTIAL_OVERLAP)
                    .build();

            String str = result.toString();

            assertNotNull(str);
            assertTrue(str.contains("null"));
        }

        @Test
        @DisplayName("manualLesson为null时toString不报错")
        void testToString_ManualLessonNull() {
            CollisionResult result = CollisionResult.builder()
                    .autoLesson(createLesson(LocalDateTime.now(), 60, "小明", "1", "钢琴"))
                    .manualLesson(null)
                    .collisionType(CollisionType.PARTIAL_OVERLAP)
                    .build();

            String str = result.toString();

            assertNotNull(str);
            assertTrue(str.contains("null"));
        }
    }
}
