# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 通用开发规范请参考 `/CLAUDE.md`（项目根目录）。

---

## Project Overview

KNPiano Batch is a Spring Boot + Spring Batch + MyBatis application for managing piano course data processing and automated tasks. The system supports both manual execution and scheduled automation modes, with a Web-based management interface.

**Key Technologies:**
- Java 21 LTS
- Spring Boot 2.7.18
- Spring Batch 4.3.x
- MyBatis 2.3.2
- MySQL 8.0.33 (business data)
- H2 (Spring Batch metadata)
- Thymeleaf (Web UI templates)

## Build and Run Commands

### Build the project
```bash
mvn clean package
```

### Manual execution mode (for development and testing)
```bash
# Manual mode with specific date
java -jar target/knbatch-1.0.0.jar --job.name=KNDB1010_MANUAL --base.date=20250831

# Auto mode using current date
java -jar target/knbatch-1.0.0.jar --job.name=KNDB1010_AUTO
```

### Web service mode (production deployment)
```bash
# Start without job.name parameter to run Web service mode
java -jar target/knbatch-1.0.0.jar
```

This starts the Web management interface on http://localhost:8081 (port configured via SERVER_PORT environment variable).

### Running tests
```bash
mvn test
```

## Execution Modes

The application has two distinct runtime modes, controlled by the presence of `--job.name` parameter:

1. **Manual Execution Mode**: When `--job.name` is provided, runs a single batch job and exits. Uses `dev` profile and WebApplicationType.NONE.

2. **Web Service Mode**: When no `--job.name` is provided, starts as a Web application with Servlet container. Uses `prod` profile and runs continuously with both Web UI and scheduled tasks.

See KnpianoBatchApplication.java:29-40 for mode detection logic.

## Architecture

### Dynamic Configuration System

The application uses a **configuration-driven architecture** that eliminates hardcoded job definitions. Batch jobs are configured via database (table: `batch_job_config`) instead of the deprecated batch-jobs.xml file.

**Key Components:**

- **BatchJobConfigDao** (src/main/java/com/liu/knbatch/dao/BatchJobConfigDao.java): Loads job configurations from database
- **BatchJobRegistry** (src/main/java/com/liu/knbatch/config/BatchJobRegistry.java): Maintains runtime registry of all batch jobs. Initializes at startup via @PostConstruct, reading from database through BatchJobConfigDao.
- **DynamicSchedulerManager** (src/main/java/com/liu/knbatch/scheduler/DynamicSchedulerManager.java): Implements SchedulingConfigurer to automatically register scheduled tasks based on cron expressions from database configuration

### Adding New Batch Jobs

To add a new batch job:

1. Create the Tasklet implementation class (e.g., `KNDB9999Tasklet.java`)
2. Create the Config class defining the Spring Batch Job bean (e.g., `KNDB9999Config.java`)
3. Create MyBatis Mapper XML (e.g., `KNDB9999Mapper.xml`)
4. Insert configuration into database table `batch_job_config` with columns:
   - `job_id`: Job identifier (e.g., "KNDB9999")
   - `bean_name`: Spring bean name (e.g., "kndb9999Job")
   - `description`: Human-readable description
   - `cron_expression`: Optional cron expression for scheduling
   - `cron_description`: Human-readable cron description
   - `target_description`: Description of what the job processes
   - `enabled`: Boolean flag to enable/disable the job

**No changes required** to KnpianoBatchApplication.java, DynamicSchedulerManager.java, or BatchJobRegistry.java.

### Job Naming Convention

Job names follow the pattern: `{JOB_ID}_{MODE}`
- JOB_ID: Business module identifier (e.g., KNDB1010)
- MODE: Either `MANUAL` (requires --base.date) or `AUTO` (uses current date)

Examples:
- `KNDB1010_MANUAL` with `--base.date=20250831`
- `KNDB1010_AUTO` (automatically uses today's date)

See KnpianoBatchApplication.java:201-214 for date extraction logic.

### Database Configuration

The application uses **two separate databases**:

1. **Business Database (MySQL)**: Configured via environment-specific properties, stores application business data
2. **Batch Metadata Database (H2)**: In-memory database for Spring Batch job execution metadata

Connection settings are in application.properties with environment-specific overrides in application-dev.properties and application-prod.properties.

### Environment Configuration

The application uses Spring profiles:
- **dev**: Development profile (detailed logging, local paths)
- **prod**: Production profile (optimized logging, production paths)
- **db**: Database initialization profile (always included via spring.profiles.include)

Configuration files:
- `application.properties`: Common settings for all environments
- `application-dev.properties`: Development-specific settings
- `application-prod.properties`: Production-specific settings
- `application-db.properties`: Database connection handling

**Environment Variables Required:**
- `SPRING_MAIL_HOST`: SMTP server host
- `SPRING_MAIL_PORT`: SMTP server port
- `EMAIL_USERNAME`: Email sender username
- `EMAIL_PASSWORD`: Email sender password (never commit to git)
- `EMAIL_RECIPIENTS`: Email recipient addresses
- `DEPLOY_ENVIROMENT`: Deployment environment label
- `SERVER_PORT`: Web application port (default: 8081)

### Email Notification System

Batch jobs can send email notifications on success/failure. Configuration:
- Email service: SimpleEmailService.java
- Template engine: Thymeleaf
- Mail configuration: Managed via BatchMailConfigDao and Web interface

Email settings configurable via Web UI at http://localhost:8081/batch/mail

### Web Management Interface

The Web UI provides:
- **Login page**: http://localhost:8081
- **Job configuration management**: http://localhost:8081/batch/job
- **Email configuration management**: http://localhost:8081/batch/mail

Controllers:
- LoginController.java: Authentication
- BatchJobConfigController.java: Job management
- BatchMailConfigController.java: Email settings

### Package Structure

```
com.liu.knbatch/
├── KnpianoBatchApplication.java    # Main entry point with dual-mode logic
├── config/                          # Configuration classes
│   ├── BatchJobInfo.java           # Job metadata entity
│   ├── BatchJobRegistry.java       # Job registry (loads from DB)
│   ├── BatchJobConfigLoader.java   # Deprecated XML loader
│   ├── KNDB*Config.java           # Individual job configurations
│   └── BatchSchedulerConfig.java   # Scheduler setup
├── scheduler/
│   └── DynamicSchedulerManager.java # Dynamic task scheduler
├── tasklet/                        # Business logic implementations
│   └── KNDB*Tasklet.java          # Individual job tasklets
├── dao/                            # MyBatis data access interfaces
├── entity/                         # Data entities
├── controller/                     # Web controllers
└── service/                        # Business services (e.g., email)
```

### Logging

Logs are written to:
- `logs/knpiano-batch.log`: Main application log
- `logs/knpiano-batch-error.log`: Error log only

Log configuration: src/main/resources/logback-spring.xml

MyBatis SQL logging is enabled at DEBUG level in dev profile (logging.level.org.apache.ibatis=DEBUG).

## Current Batch Jobs

Jobs configured in the database (batch_job_config table):

- **KNDB1010**: Piano course level correction (monthly, 1st day 00:00)
- **KNDB1020**: Student information sync
- **KNDB2020**: Annual monthly income report data monitoring
- **KNDB2030**: Prepaid lesson fee adjustment (weekly Sunday 23:00)
- **KNDB4000**: Annual week number table generation (yearly Jan 1 00:00)
- **KNDB4010**: Auto-schedule next week courses (weekly Sunday 20:00)
- **KNDB4020**: Auto-schedule vs manual-schedule collision detection
- **KNDB4030**: Fragmented lesson completion email reminder
- **KNDB5000**: Database backup (daily 01:00)

Each job follows the pattern: Config class defines the Job bean, Tasklet implements business logic, Mapper XML defines SQL queries.

## Important Notes

- The batch-jobs.xml file is **deprecated** and kept only as reference. All job configuration now comes from the database.
- Manual mode uses the `dev` profile automatically (see KnpianoBatchApplication.java:59)
- Web service mode uses the `prod` profile automatically (see KnpianoBatchApplication.java:87)
- Never commit email credentials; use environment variables
- The DatabaseConnectionWaiter.java ensures database is ready before application starts
- Job execution parameters always include: baseDate, jobMode, businessModule, timestamp

---

# LLM 编程行为准则

以下准则为最高标准要求，适用于本项目的所有开发工作。

> 权衡说明：这些准则偏向谨慎而非速度。对于简单任务，请自行判断取舍。

---

## 1. 先思考，再动手

不要假设，不要隐藏困惑，主动暴露权衡点。

动手实现之前：

- 明确说出你的假设。如果不确定，先问清楚。
- 如果存在多种解读，逐一列出——不要默默选一个。
- 如果有更简单的方案，说出来。必要时提出异议。
- 如果有不清楚的地方，停下来。说明哪里困惑，然后提问。

---

## 2. 简洁优先

用最少的代码解决问题，不写投机性代码。

- 不实现用户没有要求的功能。
- 单次使用的代码不做抽象封装。
- 没有被要求的"灵活性"或"可配置性"一律不加。
- 不为不可能发生的场景写错误处理。
- 如果你写了 200 行但 50 行就够，重写它。

自问：「一个资深工程师会觉得这过度设计吗？」如果是，简化它。

---

## 3. 外科手术式修改

只碰必须碰的地方，只清理自己制造的混乱。

**修改现有代码时：**

- 不"顺手优化"相邻代码、注释或格式。
- 不重构没有问题的东西。
- 匹配现有代码风格，即使你会用不同的方式写。
- 如果发现无关的死代码，提出来——不要擅自删除。

**当你的改动产生孤儿代码时：**

- 删除因**你的改动**而变得无用的 import / 变量 / 函数。
- 不删除原本就存在的死代码（除非被要求）。

检验标准：每一行改动都应能直接追溯到用户的需求。

---

## 4. 目标驱动执行

定义验收标准，循环直到验证通过。

将任务转化为可验证的目标：

- "添加校验" → "为非法输入写测试，然后让测试通过"
- "修复 bug" → "写一个能复现 bug 的测试，然后让测试通过"
- "重构 X" → "确保重构前后测试均通过"

对于多步骤任务，先陈述简要计划：

```
1. [步骤] → 验证：[检查点]
2. [步骤] → 验证：[检查点]
3. [步骤] → 验证：[检查点]
```

强验收标准让你能独立循环推进；弱标准（"让它能跑"）则需要不断确认。

