package com.liu.knbatch.util;

import com.liu.knbatch.entity.CollisionResult;
import com.liu.knbatch.entity.KNDB4020Entity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 碰撞报告生成器
 * 生成碰撞检测邮件报告内容（含时间轴图解）
 *
 * @author Liu
 * @version 1.0.0
 */
@Component
public class CollisionReportBuilder {

    private static final String[] WEEK_DAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    /**
     * 构建碰撞报告
     *
     * @param collisions 碰撞列表
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 报告内容
     */
    public String buildReport(List<CollisionResult> collisions, String startDate, String endDate) {
        StringBuilder sb = new StringBuilder();

        // 报告头
        sb.append("【课程碰撞检测报告】").append(startDate).append(" ~ ").append(endDate).append("\n\n");
        sb.append("检测到 ").append(collisions.size()).append(" 处课程碰撞，请及时处理：\n\n");

        // 按日期分组
        Map<LocalDate, List<CollisionResult>> grouped = collisions.stream()
                .filter(c -> c.getCollisionDate() != null)
                .collect(Collectors.groupingBy(CollisionResult::getCollisionDate));

        int index = 1;
        for (Map.Entry<LocalDate, List<CollisionResult>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();
            List<CollisionResult> dayCollisions = entry.getValue();

            for (CollisionResult collision : dayCollisions) {
                sb.append(buildCollisionSection(collision, index++, date));
            }
        }

        // 报告尾部
        sb.append("\n---\n");
        sb.append("此邮件由 KNPiano 批处理系统自动发送\n");
        sb.append("如有疑问，请联系系统管理员\n");

        return sb.toString();
    }

    /**
     * 构建单个碰撞区段
     */
    private String buildCollisionSection(CollisionResult collision, int index, LocalDate date) {
        StringBuilder sb = new StringBuilder();

        String weekDay = getWeekDay(date);
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("碰撞 #").append(index).append(": ")
                .append(dateStr)
                .append(" (").append(weekDay).append(")\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // 时间轴图解
        sb.append(buildTimelineChart(collision));

        sb.append("\n碰撞类型: ").append(collision.getCollisionType().getDescription()).append("\n");
        sb.append("碰撞原因: ").append(collision.getCollisionReason()).append("\n\n");

        return sb.toString();
    }

    /**
     * 构建时间轴图解
     */
    private String buildTimelineChart(CollisionResult collision) {
        KNDB4020Entity auto = collision.getAutoLesson();
        KNDB4020Entity manual = collision.getManualLesson();

        StringBuilder sb = new StringBuilder();

        // 计算时间范围
        int autoStartHour = auto.getSchedualDateTime().getHour();
        int autoEndHour = auto.getEndDateTime().getHour();
        int manualStartHour = manual.getSchedualDateTime().getHour();
        int manualEndHour = manual.getEndDateTime().getHour();

        int startHour = Math.min(autoStartHour, manualStartHour);
        int endHour = Math.max(autoEndHour, manualEndHour) + 1;

        // 确保时间范围合理
        if (endHour - startHour > 6) {
            endHour = startHour + 6;
        }

        // 构建时间轴头
        sb.append("            ");
        for (int h = startHour; h <= endHour; h++) {
            sb.append(String.format("%02d:00   ", h));
        }
        sb.append("\n");

        sb.append("         ──┼");
        for (int h = startHour; h <= endHour; h++) {
            sb.append("───────┼");
        }
        sb.append("──\n");

        // 自动排课行
        sb.append("自动排课    ");
        sb.append(buildLessonBar(auto, startHour, endHour));
        sb.append(auto.getStudentName()).append(" ")
                .append(auto.getSubjectName()).append(" ")
                .append(auto.getClassDuration()).append("分钟\n");

        // 手动排课行
        sb.append("手动排课    ");
        sb.append(buildLessonBar(manual, startHour, endHour));
        sb.append(manual.getStudentName()).append(" ")
                .append(manual.getSubjectName()).append(" ")
                .append(manual.getClassDuration()).append("分钟\n");

        // 重叠标记行
        sb.append("           ");
        sb.append(buildOverlapIndicator(collision));
        sb.append("\n");

        return sb.toString();
    }

    /**
     * 构建课程时间条
     */
    private String buildLessonBar(KNDB4020Entity lesson, int startHour, int endHour) {
        StringBuilder sb = new StringBuilder();

        int lessonStartMinute = (lesson.getSchedualDateTime().getHour() - startHour) * 60
                + lesson.getSchedualDateTime().getMinute();
        int lessonEndMinute = lessonStartMinute + lesson.getClassDuration();

        // 每15分钟对应2个字符
        int totalChars = (endHour - startHour + 1) * 8;
        int barStart = lessonStartMinute / 15 * 2;
        int barEnd = lessonEndMinute / 15 * 2;

        // 确保不超出边界
        barStart = Math.max(0, Math.min(barStart, totalChars - 1));
        barEnd = Math.max(0, Math.min(barEnd, totalChars));

        for (int i = 0; i < totalChars; i++) {
            if (i == barStart) {
                sb.append("├");
            } else if (i == barEnd) {
                sb.append("┤");
            } else if (i > barStart && i < barEnd) {
                sb.append("█");
            } else {
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    /**
     * 构建重叠指示器
     */
    private String buildOverlapIndicator(CollisionResult collision) {
        String label;
        switch (collision.getCollisionType()) {
            case PARTIAL_OVERLAP:
                label = String.format("│--部分重叠%d分钟-│", collision.getOverlapMinutes());
                break;
            case DURATION_DIFFERENT:
                label = "│--------被完全覆盖-------│";
                break;
            case SUBJECT_DIFFERENT:
                label = "│--------完全重叠---------│";
                break;
            default:
                label = "";
        }
        return label;
    }

    /**
     * 获取星期几
     */
    private String getWeekDay(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        return WEEK_DAYS[dayOfWeek - 1];
    }
}
