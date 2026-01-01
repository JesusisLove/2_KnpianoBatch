package com.liu.knbatch.dao;

import com.liu.knbatch.entity.KNDB4000Entity;

import org.apache.ibatis.annotations.Mapper;

/**
 * KNDB4000 钢琴课程级别矫正 数据访问接口
 * 
 * @author Liu
 * @version 1.0.0
 */
@Mapper
public interface KNDB4000Dao {
    
    /**
     * 年度周次表生成（1年52周）
     */
    void insertFixedLessonStatus(KNDB4000Entity status);

    /**
     * 删除年度的周次记录
     */
    int deleteAll();

}