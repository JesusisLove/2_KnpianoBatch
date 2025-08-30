package com.liu.knbatch.dao;

import java.util.List;

import com.liu.knbatch.config.BatchJobInfo;

public interface BatchJobConfigDao {

    // 加载batch启动配置信息
    List<BatchJobInfo> loadBatchJobs();

    BatchJobInfo findByJobId(String jobId);

    // 追加Job配置
    int insertJobInfo(BatchJobInfo jobInfo);

    // 修改Job配置
    int updateJobInfo(BatchJobInfo jobInfo);

}
