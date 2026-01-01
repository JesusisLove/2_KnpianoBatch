package com.liu.knbatch.dao;

import com.liu.knbatch.entity.KNDB2020MonthSummaryEntity;
import com.liu.knbatch.entity.KNDB2020ValidationSummaryEntity;
import com.liu.knbatch.entity.KNDB2020FeeErrorEntity;
import com.liu.knbatch.entity.KNDB2020PayErrorEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * KNDB2020 年度月收入报告数据监视 数据访问接口
 *
 * @author Liu
 * @version 1.0.0
 */
@Mapper
public interface KNDB2020Dao {

    /**
     * 获取指定年度的所有月份汇总验证数据
     *
     * @param year 年度 (格式: yyyy，如: "2025")
     * @return 月度汇总验证列表，包含应收、已支付、未支付、差额等信息
     */
    List<KNDB2020MonthSummaryEntity> getMonthSummaryList(@Param("year") String year);

    /**
     * 获取指定年度的整体验证结果汇总
     *
     * @param year 年度 (格式: yyyy，如: "2025")
     * @return 验证结果汇总，包含总月份数、正确月份数、错误月份数、最终结果
     */
    KNDB2020ValidationSummaryEntity getValidationSummary(@Param("year") String year);

    /**
     * 检查费用表：一个lesson_id是否对应了多个lsn_fee_id（绝对不允许）
     *
     * @return 费用表错误记录列表，包含lesson_id和对应的多个lsn_fee_id
     */
    List<KNDB2020FeeErrorEntity> getFeeErrorList();

    /**
     * 检查支付表：一个lsn_fee_id是否对应了多个lsn_pay_id（绝对不允许）
     *
     * @return 支付表错误记录列表，包含lsn_fee_id和对应的多个lsn_pay_id
     */
    List<KNDB2020PayErrorEntity> getPayErrorList();
}
