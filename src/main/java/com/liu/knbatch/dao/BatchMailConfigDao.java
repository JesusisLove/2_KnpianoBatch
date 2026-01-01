package com.liu.knbatch.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.liu.knbatch.config.BatchMailInfo;

import java.util.List;
import java.util.Map;

/**
 * 批处理邮件配置Mapper接口
 * 对应XML文件：BatchMailConfigMapper.xml
 */
@Mapper
public interface BatchMailConfigDao {

    /**
     * 查询所有邮件配置信息
     * @return 邮件配置信息列表
     */
    List<BatchMailInfo> selectAllMailInfo();

    /**
     * 根据作业ID查询邮件配置信息
     * @param jobId 作业ID
     * @return 邮件配置信息，如果不存在返回null
     */
    BatchMailInfo selectMailInfo(@Param("jobId") String jobId);

    /**
     * 插入邮件配置信息
     * @param mailInfo 邮件配置信息对象
     * @return 插入的记录数
     */
    int insertMailInfo(BatchMailInfo mailInfo);

    /**
     * 更新邮件配置信息
     * @param mailInfo 邮件配置信息对象
     * @return 更新的记录数
     */
    int updateMailInfo(BatchMailInfo mailInfo);

    /**
     * 根据作业ID删除邮件配置信息
     * @param jobId 作业ID
     * @return 删除的记录数
     */
    int deleteMailInfo(@Param("jobId") String jobId);

    /**
     * 检查作业ID是否存在
     * @param jobId 作业ID
     * @return 存在的记录数（0表示不存在，1表示存在）
     */
    int existsByJobId(@Param("jobId") String jobId);

    /**
     * 根据条件查询邮件配置信息
     * @param conditions 查询条件Map
     * @return 邮件配置信息列表
     */
    List<BatchMailInfo> selectMailInfoByConditions(Map<String, Object> conditions);
}