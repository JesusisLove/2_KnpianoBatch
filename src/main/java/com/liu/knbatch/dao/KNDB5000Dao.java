package com.liu.knbatch.dao;

// import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// import java.util.List;

/**
 * KNDB5000 数据库备份 DAO
 * 
 * @author Liu
 * @version 1.0.0
 */
@Mapper
public interface KNDB5000Dao {
    
    /**
     * 检查数据库连接状态
     * @return 连接正常返回1
     */
    @Select("SELECT 1")
    Integer checkDatabaseConnection();
    
    /**
     * 获取数据库大小信息
     * @param databaseName 数据库名称
     * @return 数据库大小（字节）
     */
    @Select("SELECT ROUND(SUM(data_length + index_length), 0) AS database_size " +
            "FROM information_schema.tables " +
            "WHERE table_schema = #{databaseName}")
    Long getDatabaseSize(@Param("databaseName") String databaseName);
    
    /**
     * 获取数据库表数量
     * @param databaseName 数据库名称
     * @return 表数量
     */
    @Select("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = #{databaseName}")
    Integer getTableCount(@Param("databaseName") String databaseName);
    
    /**
     * 获取数据库版本信息
     * @return 数据库版本
     */
    @Select("SELECT VERSION()")
    String getDatabaseVersion();
    
//     /**
//      * 获取数据库所有表名
//      * @param databaseName 数据库名称
//      * @return 表名列表
//      */
//     @Select("SELECT table_name FROM information_schema.tables WHERE table_schema = #{databaseName} ORDER BY table_name")
//     List<String> getAllTableNames(@Param("databaseName") String databaseName);
    
//     /**
//      * 记录备份日志（如果有备份日志表的话）
//      * 注意：这个方法可选，如果没有备份日志表可以删除
//      */
//     @Insert("INSERT INTO t_backup_log (backup_file_name, backup_file_path, backup_size, " +
//             "backup_start_time, backup_end_time, backup_status, error_message, create_time) " +
//             "VALUES (#{backupFileName}, #{backupFilePath}, #{backupFileSize}, " +
//             "#{backupStartTime}, #{backupEndTime}, #{backupStatus}, #{errorMessage}, NOW())")
//     void insertBackupLog(
//             @Param("backupFileName") String backupFileName,
//             @Param("backupFilePath") String backupFilePath,
//             @Param("backupFileSize") Long backupFileSize,
//             @Param("backupStartTime") String backupStartTime,
//             @Param("backupEndTime") String backupEndTime,
//             @Param("backupStatus") String backupStatus,
//             @Param("errorMessage") String errorMessage);
}