package com.liu.knbatch.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * KNDB1020 学生信息同步 数据访问接口
 * 
 * @author Liu
 * @version 1.0.0
 */
@Mapper
public interface KNDB1020Dao {
    
    /**
     * 获取需要同步的学生信息记录数
     * 
     * @return 待同步的学生信息记录数
     */
    int selectPendingSyncStudentCount();
    
    /**
     * 执行学生信息同步操作
     * 
     * @return 成功同步的记录数
     */
    int syncStudentInformation();
    
    /**
     * 根据条件获取需要同步的学生信息记录数
     * 
     * @param syncDate 同步日期 (格式: yyyy-MM-dd)
     * @return 待同步的学生信息记录数
     */
    int selectPendingSyncStudentCountByDate(@Param("syncDate") String syncDate);
    
    /**
     * 根据日期范围执行学生信息同步
     * 
     * @param startDate 开始日期 (格式: yyyy-MM-dd)
     * @param endDate 结束日期 (格式: yyyy-MM-dd)
     * @return 成功同步的记录数
     */
    int syncStudentInformationByDateRange(@Param("startDate") String startDate, 
                                        @Param("endDate") String endDate);
}