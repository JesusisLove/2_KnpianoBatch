package com.liu.knbatch.util;

import com.liu.knbatch.entity.KNDB4030Entity;
import com.liu.knbatch.tasklet.KNDB4030Tasklet.MergeableGroup;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * KNDB4030 碎片課合併提醒報告生成器
 * 生成郵件報告内容，展示哪些学生的哪些碎片課可以湊成整課
 *
 * @author Liu
 * @version 1.0.0
 */
@Component
public class FragmentMergeReportBuilder {

    /**
     * 構建碎片課合併提醒報告
     *
     * @param mergeableGroups 可合併碎片組列表
     * @param baseDate        基準日期（yyyyMMdd）
     * @return 報告内容
     */
    public String buildReport(List<MergeableGroup> mergeableGroups, String baseDate) {

        StringBuilder sb = new StringBuilder();

        // 格式化日期
        String formattedDate = baseDate.replaceAll(
                "(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3");

        // 報告頭
        sb.append("【零碎課合併提醒】").append(formattedDate).append("\n\n");
        sb.append("検出到以下学生的零碎課可以湊成整課，請及時処理：\n\n");

        // 按学生+科目聚合（同一学生同一科目可能有多組）
        Map<String, List<MergeableGroup>> byStudentSubject =
                mergeableGroups.stream()
                        .collect(Collectors.groupingBy(
                                g -> g.getStuId() + "|" + g.getSubjectId(),
                                LinkedHashMap::new,
                                Collectors.toList()));

        int totalLessons = 0;
        Set<String> studentSet = new HashSet<>();

        for (Map.Entry<String, List<MergeableGroup>> entry
                : byStudentSubject.entrySet()) {

            List<MergeableGroup> groups = entry.getValue();
            MergeableGroup first = groups.get(0);

            studentSet.add(first.getStuId());

            // 学生+科目標題
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            sb.append("学生: ").append(first.getStuName());
            sb.append("  科目: ").append(first.getSubjectName());
            sb.append("  標準課時: ").append(first.getMinutesPerLsn())
                    .append("分鐘\n");
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

            // 毎組碎片明細
            for (int i = 0; i < groups.size(); i++) {
                MergeableGroup group = groups.get(i);
                totalLessons++;

                sb.append("  ▶ 可湊成第").append(i + 1)
                        .append("節整課（共").append(group.getFragments().size())
                        .append("個碎片）:\n");

                int subtotal = 0;
                for (KNDB4030Entity fragment : group.getFragments()) {
                    sb.append("    · ").append(fragment.getScanqrDateShort())
                            .append(" ").append(fragment.getClassDuration())
                            .append("分鐘\n");
                    subtotal += fragment.getClassDuration();
                }

                sb.append("    合計: ").append(subtotal)
                        .append("分鐘 = 1節標準課時\n\n");
            }
        }

        int totalStudents = studentSet.size();

        // 報告匯總
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("共計: ").append(totalStudents).append("名学生, ")
                .append(totalLessons).append("節整課可合併\n");

        return sb.toString();
    }
}
