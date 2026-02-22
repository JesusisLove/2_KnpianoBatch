package com.liu.knbatch.util;

import com.liu.knbatch.entity.KNDB4030Entity;
import com.liu.knbatch.tasklet.KNDB4030Tasklet.MergeableGroup;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * KNDB4030 碎片课合并提醒报告生成器
 * 生成邮件报告内容，展示哪些学生的哪些碎片课可以凑成整课
 *
 * @author Liu
 * @version 1.0.0
 */
@Component
public class FragmentMergeReportBuilder {

    /**
     * 构建碎片课合并提醒报告
     *
     * @param mergeableGroups 可合并碎片组列表
     * @param baseDate        基准日期（yyyyMMdd）
     * @return 报告内容
     */
    public String buildReport(List<MergeableGroup> mergeableGroups, String baseDate) {

        StringBuilder sb = new StringBuilder();

        // 格式化日期
        String formattedDate = baseDate.replaceAll(
                "(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3");

        // 按学生+科目聚合（同一学生同一科目可能有多组）
        Map<String, List<MergeableGroup>> byStudentSubject =
                mergeableGroups.stream()
                        .collect(Collectors.groupingBy(
                                g -> g.getStuId() + "|" + g.getSubjectId(),
                                LinkedHashMap::new,
                                Collectors.toList()));

        // 先统计学生总数，用于报告头
        long studentCount = mergeableGroups.stream()
                .map(MergeableGroup::getStuId).distinct().count();

        // 报告头（含学生总数）
        sb.append("【零碎课合并提醒】").append(formattedDate).append("\n");
        sb.append("检测到以下").append(studentCount)
                .append("个学生的零碎课可以凑成整课，请及时处理：\n\n");

        int totalLessons = 0;
        Set<String> studentSet = new HashSet<>();
        int studentIndex = 0;

        for (Map.Entry<String, List<MergeableGroup>> entry
                : byStudentSubject.entrySet()) {

            List<MergeableGroup> groups = entry.getValue();
            MergeableGroup first = groups.get(0);

            studentSet.add(first.getStuId());
            studentIndex++;

            // 学生+科目标题
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━\n");
            sb.append(studentIndex).append(". 学生: ").append(first.getStuName());
            sb.append("  科目: ").append(first.getSubjectName());
            sb.append("  标准课时: ").append(first.getMinutesPerLsn())
                    .append("分钟\n");
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━\n\n");

            // 每组碎片明细
            for (int i = 0; i < groups.size(); i++) {
                MergeableGroup group = groups.get(i);
                totalLessons++;

                sb.append("  ▶ 可凑成第").append(i + 1)
                        .append("节整课（共").append(group.getFragments().size())
                        .append("个碎片）:\n");

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                int subtotal = 0;
                for (KNDB4030Entity fragment : group.getFragments()) {
                    String dateStr = fragment.getScanqrDate() != null
                            ? fragment.getScanqrDate().format(dtf) : "";
                    sb.append("    · ").append(dateStr)
                            .append("  ").append(fragment.getClassDuration())
                            .append("分钟\n");
                    subtotal += fragment.getClassDuration();
                }

                sb.append("    合计: ").append(subtotal)
                        .append("分钟 = 1节标准课时 ✓\n\n");
            }
        }

        int totalStudents = studentSet.size();

        // 报告汇总
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("共计: ").append(totalStudents).append("名学生, ")
                .append(totalLessons).append("节整课可合并\n");

        return sb.toString();
    }
}
