# 新Batch模块开发手顺 - 标准流程

**参考案例**: KNDB2020 年度月收入报告数据监视
**开发日期**: 2025-12-16
**本文档用途**: 作为开发新batch模块的标准参考手顺

---

## 📋 目录

1. [成果物清单](#成果物清单)
2. [开发手顺（按顺序执行）](#开发手顺)
3. [数据库初期化](#数据库初期化)
4. [编译和测试](#编译和测试)
5. [验证清单](#验证清单)

---

## 📦 成果物清单

### 1. Entity类（实体类）

根据SQL查询结果，创建对应的Entity类。

**KNDB2020案例 - 创建了4个Entity类**：

| 文件路径 | 用途 | 对应SQL |
|---------|------|---------|
| `src/main/java/com/liu/knbatch/entity/KNDB2020MonthSummaryEntity.java` | 月度汇总验证实体 | SQL-1 查询结果 |
| `src/main/java/com/liu/knbatch/entity/KNDB2020ValidationSummaryEntity.java` | 汇总验证结果实体 | SQL-2 查询结果 |
| `src/main/java/com/liu/knbatch/entity/KNDB2020FeeErrorEntity.java` | 费用表错误记录实体 | SQL-3-1 查询结果 |
| `src/main/java/com/liu/knbatch/entity/KNDB2020PayErrorEntity.java` | 支付表错误记录实体 | SQL-3-2 查询结果 |

**命名规则**：`KNDB{编号}{业务名称}Entity.java`

### 2. 数据访问层

| 文件路径 | 用途 |
|---------|------|
| `src/main/java/com/liu/knbatch/dao/KNDB2020Dao.java` | DAO接口（定义查询方法） |
| `src/main/resources/mybatis/mapper/KNDB2020Mapper.xml` | MyBatis映射文件（实现SQL） |

### 3. 业务逻辑层

| 文件路径 | 用途 |
|---------|------|
| `src/main/java/com/liu/knbatch/tasklet/KNDB2020Tasklet.java` | Tasklet业务处理任务 |

### 4. 配置层

| 文件路径 | 用途 |
|---------|------|
| `src/main/java/com/liu/knbatch/config/KNDB2020Config.java` | Spring Batch配置类 |

### 5. 数据库配置脚本

| 文件路径 | 用途 |
|---------|------|
| `database/KNDB2020_insert_NOW.sql` | 数据库配置脚本（实际邮箱版） |
| `database/KNDB2020_config.sql` | 数据库配置脚本（模板版） |

### 6. 文档资料

| 文件路径 | 用途 |
|---------|------|
| `KNDB2020_部署说明.md` | 完整部署指南 |
| `KNDB2020_测试清单.md` | 测试检查清单 |
| `开发手顺_新Batch模块标准流程.md` | 本文档（标准流程） |

---

## 🔧 开发手顺

### 前置准备

1. **明确业务需求**
   - 业务逻辑是什么？
   - 需要查询哪些数据？
   - 需要更新哪些数据？
   - 执行频率是什么？（cron表达式）
   - 是否需要发送邮件？

2. **准备SQL语句**
   - 编写并测试所有SQL查询
   - 确认SQL结果结构
   - 确认所需的视图和表都存在

---

### 步骤1: 创建Entity类

**目的**: 为每个SQL查询结果创建对应的Java实体类

**操作**:

1. 分析SQL查询的返回字段
2. 创建Entity类，遵循命名规范：`KNDB{编号}{业务名称}Entity.java`
3. 使用标准POJO模式：
   - 私有成员变量
   - 无参构造函数
   - 全参构造函数
   - Getter/Setter方法

**参考模板**:
```java
package com.liu.knbatch.entity;

import java.math.BigDecimal; // 金额类型

/**
 * {业务描述}实体类
 *
 * @author Liu
 * @version 1.0.0
 */
public class KNDBXXXX{Name}Entity {

    private String fieldName;       // 字段描述
    private BigDecimal amount;      // 金额字段用BigDecimal
    private Integer count;          // 计数字段用Integer

    public KNDBXXXX{Name}Entity() {}

    public KNDBXXXX{Name}Entity(参数列表) {
        // 初始化
    }

    // Getter/Setter 方法
}
```

**KNDB2020案例**:
- 4个SQL查询 → 4个Entity类
- 金额字段使用 `BigDecimal`
- 计数字段使用 `Integer`

---

### 步骤2: 创建DAO接口

**位置**: `src/main/java/com/liu/knbatch/dao/KNDBXXXX Dao.java`

**操作**:

1. 创建DAO接口
2. 添加 `@Mapper` 注解
3. 定义查询方法，使用 `@Param` 注解传参

**参考模板**:
```java
package com.liu.knbatch.dao;

import com.liu.knbatch.entity.KNDBXXXXEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * KNDBXXXX {业务描述} 数据访问接口
 *
 * @author Liu
 * @version 1.0.0
 */
@Mapper
public interface KNDBXXXXDao {

    /**
     * 查询方法描述
     *
     * @param paramName 参数描述
     * @return 返回值描述
     */
    List<KNDBXXXXEntity> getXxxList(@Param("paramName") String paramName);

    // 返回单个对象
    KNDBXXXXEntity getXxxInfo(@Param("paramName") String paramName);

    // 更新操作
    int updateXxx(@Param("paramName") String paramName);
}
```

**KNDB2020案例**:
- 4个查询方法
- 参数使用 `@Param("year")` 传递年份

---

### 步骤3: 创建Mapper XML

**位置**: `src/main/resources/mybatis/mapper/KNDBXXXXMapper.xml`

**操作**:

1. 创建XML文件
2. 定义 `namespace`（对应DAO接口）
3. 创建 `resultMap`（字段映射）
4. 实现SQL查询

**参考模板**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.liu.knbatch.dao.KNDBXXXXDao">

    <!-- 结果映射 -->
    <resultMap id="EntityMap" type="com.liu.knbatch.entity.KNDBXXXXEntity">
        <result property="javaFieldName" column="db_column_name" />
    </resultMap>

    <!-- 查询SQL -->
    <select id="getXxxList" parameterType="string" resultMap="EntityMap">
        SELECT
            db_column_name,
            CASE
                WHEN condition THEN value1
                ELSE value2
            END as calculated_field
        FROM table_name
        WHERE column_name = #{paramName}
        ORDER BY column_name
    </select>

</mapper>
```

**注意事项**:
- 数据库列名使用下划线（snake_case）
- Java属性名使用驼峰命名（camelCase）
- XML中的特殊字符需要转义：
  - `<` → `<![CDATA[<]]>` 或 `&lt;`
  - `>` → `<![CDATA[>]]>` 或 `&gt;`
  - `<=` → `<![CDATA[<=]]>`
  - `>=` → `<![CDATA[>=]]>`

**KNDB2020案例**:
- 4个resultMap对应4个Entity
- 使用 `CONCAT(#{year}, '-%')` 动态拼接年份
- 使用 `<![CDATA[<]]>` 处理小于号

---

### 步骤4: 创建Tasklet业务逻辑

**位置**: `src/main/java/com/liu/knbatch/tasklet/KNDBXXXXTasklet.java`

**操作**:

1. 实现 `Tasklet` 接口
2. 添加 `@Component` 注解
3. 注入DAO和邮件服务
4. 实现 `execute` 方法

**参考模板**:
```java
package com.liu.knbatch.tasklet;

import com.liu.knbatch.config.BatchMailInfo;
import com.liu.knbatch.dao.BatchMailConfigDao;
import com.liu.knbatch.dao.KNDBXXXXDao;
import com.liu.knbatch.entity.KNDBXXXXEntity;
import com.liu.knbatch.service.SimpleEmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * KNDBXXXX {业务描述} 业务处理任务
 *
 * 业务逻辑：
 * 1. {步骤1描述}
 * 2. {步骤2描述}
 * 3. {步骤3描述}
 *
 * @author Liu
 * @version 1.0.0
 */
@Component
public class KNDBXXXXTasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(KNDBXXXXTasklet.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private String jobId = "KNDBXXXX";

    @Autowired
    private KNDBXXXXDao dao;

    @Autowired
    private BatchMailConfigDao mailDao;

    @Autowired(required = false)
    private SimpleEmailService emailService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long startTime = System.currentTimeMillis();
        String batchName = "KNDBXXXX";
        String description = "{业务描述}";
        boolean success = false;
        StringBuilder logContent = new StringBuilder();

        addLog(logContent, "========== " + batchName + " 批处理开始执行 ==========");
        logger.info("========== {} 批处理开始执行 ==========", batchName);

        try {
            // 获取作业参数
            String baseDate = (String) chunkContext.getStepContext()
                    .getJobParameters().get("baseDate");
            String jobMode = (String) chunkContext.getStepContext()
                    .getJobParameters().get("jobMode");

            addLog(logContent, "批处理参数 - 基准日期: " + baseDate + ", 执行模式: " + jobMode);
            logger.info("批处理参数 - 基准日期: {}, 执行模式: {}", baseDate, jobMode);

            // 业务逻辑实现
            // ...

            success = true;

            // 发送邮件通知
            sendEmailNotification(batchName, description, success, logContent.toString());

            return RepeatStatus.FINISHED;

        } catch (Exception e) {
            addLog(logContent, "========== " + batchName + " 批处理执行异常 ==========");
            addLog(logContent, "错误信息: " + e.getMessage());
            logger.error("========== {} 批处理执行异常 ==========", batchName, e);

            success = false;
            sendEmailNotification(batchName, description, success, logContent.toString());
            throw e;
        }
    }

    /**
     * 添加日志条目（带时间戳）
     */
    private void addLog(StringBuilder logContent, String message) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logContent.append(String.format("[%s] %s\n", timestamp, message));
    }

    /**
     * 发送邮件通知
     */
    private void sendEmailNotification(String jobName, String description,
            boolean success, String logContent) {
        BatchMailInfo mailInfo = mailDao.selectMailInfo(jobId);

        try {
            if (emailService != null) {
                emailService.setFromEmail(mailInfo.getEmailFrom());
                emailService.setToEmails(mailInfo.getMailToDevloper());
                emailService.sendBatchNotification(jobName, description, success, logContent);

                if (!mailInfo.getEmailToUser().isEmpty()){
                    emailService.setToEmails(mailInfo.getEmailToUser());
                    // 构建用户邮件内容
                    emailService.sendBatchNotification(jobName, description, success, logContent);
                }

                logger.info("邮件通知发送完成 - jobName: {}, success: {}", jobName, success);
            } else {
                logger.info("邮件服务未启用，跳过邮件发送 - jobName: {}", jobName);
            }
        } catch (Exception e) {
            logger.error("发送邮件通知时出错 - jobName: {}, error: {}", jobName, e.getMessage(), e);
        }
    }
}
```

**关键点**:
- 使用 `@Autowired(required = false)` 注入邮件服务（避免未配置时报错）
- 使用 `StringBuilder` 累积日志
- 异常情况下也要发送邮件
- 邮件发送失败不影响批处理任务状态

---

### 步骤5: 创建Config配置类

**位置**: `src/main/java/com/liu/knbatch/config/KNDBXXXXConfig.java`

**操作**:

1. 创建配置类
2. 添加 `@Configuration` 注解
3. 定义Job和Step Bean
4. 创建JobExecutionListener

**参考模板**:
```java
package com.liu.knbatch.config;

import com.liu.knbatch.tasklet.KNDBXXXXTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KNDBXXXX {业务描述} 批处理配置类
 *
 * @author Liu
 * @version 1.0.0
 */
@Configuration
public class KNDBXXXXConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private KNDBXXXXTasklet tasklet;

    /**
     * KNDBXXXX 批处理作业配置
     */
    @Bean("kndbXXXXJob")  // 注意Bean名称要与数据库配置一致
    public Job kndbXXXXJob() {
        return jobBuilderFactory.get("KNDBXXXX")
                .incrementer(new RunIdIncrementer())
                .listener(new KNDBXXXXJobExecutionListener())
                .start(kndbXXXXStep())
                .build();
    }

    /**
     * KNDBXXXX 步骤配置
     */
    @Bean("kndbXXXXStep")
    public Step kndbXXXXStep() {
        return stepBuilderFactory.get("KNDBXXXX_STEP")
                .tasklet(tasklet)
                .build();
    }

    /**
     * KNDBXXXX 作业执行监听器
     */
    public static class KNDBXXXXJobExecutionListener extends JobExecutionListenerSupport {

        private static final Logger logger = LoggerFactory.getLogger(KNDBXXXXJobExecutionListener.class);

        @Override
        public void beforeJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            String baseDate = jobExecution.getJobParameters().getString("baseDate");
            String jobMode = jobExecution.getJobParameters().getString("jobMode");

            logger.info("*************************************************");
            logger.info("KNDBXXXX {业务描述}作业开始执行");
            logger.info("作业名称: {}", jobName);
            logger.info("执行模式: {}", jobMode);
            logger.info("基准日期: {}", baseDate);
            logger.info("业务描述: {详细描述}");
            logger.info("*************************************************");
        }

        @Override
        public void afterJob(JobExecution jobExecution) {
            String status = jobExecution.getStatus().toString();
            long duration = 0;

            if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
                duration = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
            }

            logger.info("*************************************************");
            logger.info("KNDBXXXX {业务描述}作业执行完成");
            logger.info("执行状态: {}", status);
            logger.info("执行耗时: {} ms ({} 秒)", duration, duration / 1000.0);

            if ("COMPLETED".equals(status)) {
                logger.info("✅ {业务描述}处理成功");
            } else if ("FAILED".equals(status)) {
                logger.error("❌ {业务描述}处理失败");
            }

            logger.info("*************************************************");
        }
    }
}
```

**关键点**:
- Bean名称格式：`kndbXXXXJob`（小写开头）
- Job名称格式：`KNDBXXXX`（大写）
- 必须与数据库配置中的 `bean_name` 一致

---

### 步骤6: 准备数据库配置SQL脚本

**位置**: `database/KNDBXXXX_insert_NOW.sql`

**操作**:

1. 创建SQL脚本
2. 配置作业信息（t_batch_job_config）
3. 配置邮件信息（t_batch_mail_config）

**参考模板**:
```sql
-- ============================================================
-- KNDBXXXX {业务描述} 批处理配置脚本
-- ============================================================
-- 执行频率: {cron描述}
-- ============================================================

-- 1. 插入批处理作业配置
INSERT INTO t_batch_job_config (
    job_id,
    bean_name,
    description,
    cron_expression,
    cron_description,
    target_description,
    batch_enabled
) VALUES (
    'KNDBXXXX',
    'kndbXXXXJob',
    '{业务简述}',
    '{cron表达式}',
    '{cron描述}',
    '{详细业务描述}',
    1
);

-- 2. 插入批处理邮件配置
INSERT INTO t_batch_mail_config (
    job_id,
    email_from,
    mail_to_devloper,
    email_to_user,
    mail_content_for_user
) VALUES (
    'KNDBXXXX',
    '{发送方邮箱}',
    '{接收方邮箱，逗号分隔}',
    '',
    ''
);

-- 3. 验证插入结果
SELECT job_id, bean_name, description, batch_enabled
FROM t_batch_job_config
WHERE job_id = 'KNDBXXXX';

SELECT job_id, email_from, mail_to_devloper
FROM t_batch_mail_config
WHERE job_id = 'KNDBXXXX';
```

**常用cron表达式**:
- `0 30 0 * * ?` - 每天凌晨0:30
- `0 0 1 1 * ?` - 每月1号凌晨1点
- `0 0 20 ? * SUN` - 每周日晚上8点
- `0 0 1 1 1 ?` - 每年1月1号凌晨1点

**KNDB2020案例**:
- cron表达式: `0 30 0 * * ?`（每天凌晨0:30）
- 字段名使用 `batch_enabled`（不是 `enabled`）

---

## 🗄️ 数据库初期化

### 步骤1: 修改SQL脚本中的邮箱地址

打开 `database/KNDBXXXX_insert_NOW.sql`，修改：

```sql
INSERT INTO t_batch_mail_config (
    ...
) VALUES (
    'KNDBXXXX',
    '发送方邮箱',           -- ⚠️ 改成实际邮箱
    '接收方邮箱',           -- ⚠️ 改成实际邮箱（可多个，逗号分隔）
    '',
    ''
);
```

### 步骤2: 执行SQL脚本

**方法1: 使用mysql命令行**
```bash
mysql -h {host} -P {port} -u {username} -p{password} {database} < database/KNDBXXXX_insert_NOW.sql
```

**KNDB2020实际命令**:
```bash
mysql -h 192.168.50.101 -P 49168 -u root -p7654321 KNStudent < database/KNDB2020_insert_NOW.sql
```

**方法2: 使用数据库客户端**
1. 打开数据库客户端（DBeaver, MySQL Workbench等）
2. 连接到数据库
3. 打开SQL脚本文件
4. 执行全部SQL

### 步骤3: 验证配置已插入

```sql
-- 查看作业配置
SELECT * FROM t_batch_job_config WHERE job_id = 'KNDBXXXX';

-- 查看邮件配置
SELECT * FROM t_batch_mail_config WHERE job_id = 'KNDBXXXX';

-- 查看所有作业
SELECT job_id, description, batch_enabled
FROM t_batch_job_config
ORDER BY job_id;
```

**预期结果**:
- t_batch_job_config 表中有新记录
- t_batch_mail_config 表中有新记录
- 邮箱地址已是实际邮箱（不是模板文本）

---

## 🔨 编译和测试

### 步骤1: 编译项目

```bash
cd $HOME/Documents/KnPianoBatchRepository/KnpianoBatch
mvn clean package -DskipTests
```

**预期结果**:
```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 步骤2: 设置环境变量

```bash
source setup-env-test_or_dev.sh
```

**验证环境变量**:
```bash
echo $EMAIL_USERNAME
echo $DB_URL
echo $SERVER_PORT
```

### 步骤3: 手动测试（MANUAL模式）

```bash
# 方法1: 使用Maven
mvn spring-boot:run -Dspring-boot.run.arguments="--job.name=KNDBXXXX_MANUAL --base.date=20251216"

# 方法2: 使用JAR包
java -jar target/knbatch-1.0.0.jar \
  --job.name=KNDBXXXX_MANUAL \
  --base.date=20251216
```

**预期输出**:
```
*************************************************
KNDBXXXX {业务描述}作业开始执行
步骤1: ...
步骤2: ...
✅ {业务描述}处理成功
*************************************************
[INFO] BUILD SUCCESS
```

### 步骤4: 自动测试（AUTO模式）

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job.name=KNDBXXXX_AUTO"
```

### 步骤5: 检查日志

```bash
# 查看主日志
tail -100 logs/knpiano-batch.log | grep KNDBXXXX

# 查看错误日志（如果有）
tail -100 logs/knpiano-batch-error.log | grep KNDBXXXX
```

### 步骤6: 检查邮件

验证邮件是否发送到配置的邮箱：
- 邮件主题包含作业名称
- 邮件内容包含执行结果
- 邮件格式友好易读

---

## ✅ 验证清单

### 代码文件检查

- [ ] Entity类已创建（每个SQL查询一个Entity）
- [ ] DAO接口已创建
- [ ] Mapper XML已创建
- [ ] Tasklet已创建
- [ ] Config已创建
- [ ] SQL脚本已创建

### 代码质量检查

- [ ] Entity类有完整的getter/setter
- [ ] DAO方法有Javadoc注释
- [ ] SQL语句已在数据库中测试通过
- [ ] Tasklet有完整的日志记录
- [ ] Config的Bean名称与数据库配置一致

### 数据库配置检查

- [ ] SQL脚本中的邮箱已修改为实际邮箱
- [ ] t_batch_job_config 表已插入记录
- [ ] t_batch_mail_config 表已插入记录
- [ ] batch_enabled = 1（已启用）
- [ ] cron_expression 正确

### 编译测试检查

- [ ] `mvn clean package` 编译成功
- [ ] 环境变量已设置
- [ ] MANUAL模式测试成功
- [ ] AUTO模式测试成功
- [ ] 日志文件正常生成
- [ ] 邮件正常发送
- [ ] 邮件内容格式正确

### 业务逻辑检查

- [ ] 数据查询结果正确
- [ ] 业务逻辑执行正确
- [ ] 异常处理完善
- [ ] 邮件内容符合需求

---

## 📝 开发时间估算

基于KNDB2020的开发经验：

| 步骤 | 预计时间 |
|------|---------|
| 需求分析和SQL准备 | 30-60分钟 |
| Entity类创建 | 15-30分钟 |
| DAO和Mapper创建 | 30-45分钟 |
| Tasklet业务逻辑 | 60-120分钟 |
| Config配置 | 15-30分钟 |
| SQL脚本和文档 | 30-45分钟 |
| 编译测试调试 | 30-60分钟 |
| **总计** | **3.5-6.5小时** |

---

## 🎯 常见注意事项

### 1. 命名规范

- **Entity**: `KNDBXXXX{Name}Entity.java`
- **DAO**: `KNDBXXXXDao.java`
- **Mapper**: `KNDBXXXXMapper.xml`
- **Tasklet**: `KNDBXXXXTasklet.java`
- **Config**: `KNDBXXXXConfig.java`
- **Bean名称**: `kndbXXXXJob`（小写开头）
- **Job名称**: `KNDBXXXX`（大写）

### 2. 数据库字段名

- 作业配置表使用 `batch_enabled`（不是 `enabled`）
- 邮件配置表使用 `mail_to_devloper`（注意拼写）

### 3. 日期参数处理

- baseDate 格式: `yyyyMMdd`（如：20251216）
- 需要转换为其他格式时使用 `DateTimeFormatter`

### 4. 邮件发送

- 使用 `@Autowired(required = false)` 避免邮件服务未配置时报错
- 邮件发送失败不应影响批处理任务状态
- 异常情况下也要发送邮件

### 5. 事务管理

- Tasklet 默认在事务中执行
- 如需手动控制事务，使用 `@Transactional` 注解

---

## 📚 参考资料

### 现有模块参考

可参考以下现有模块的实现：

- **KNDB1010**: 数据更新和错误检测
- **KNDB2030**: 数据调整和邮件通知
- **KNDB4000**: 年度数据生成
- **KNDB4010**: 自动排课
- **KNDB5000**: 数据库备份
- **KNDB2020**: 数据验证和错误报告（本次开发）

### 技术文档

- Spring Batch官方文档
- MyBatis官方文档
- 项目 CLAUDE.md 文档

---

## 🔄 持续改进

每次开发新模块后，建议：

1. 更新本文档（如有新的最佳实践）
2. 记录遇到的问题和解决方案
3. 优化代码模板
4. 改进测试流程

---

**文档版本**: 1.0.0
**最后更新**: 2025-12-16
**维护者**: Liu
**参考案例**: KNDB2020 年度月收入报告数据监视
