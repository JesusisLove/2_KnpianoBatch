# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
- **KNDB2030**: Prepaid lesson fee adjustment (weekly Sunday 23:00)
- **KNDB4000**: Annual week number table generation (yearly Jan 1 00:00)
- **KNDB4010**: Auto-schedule next week courses (weekly Sunday 20:00)
- **KNDB5000**: Database backup (daily 01:00)

Each job follows the pattern: Config class defines the Job bean, Tasklet implements business logic, Mapper XML defines SQL queries.

## Important Notes

- The batch-jobs.xml file is **deprecated** and kept only as reference. All job configuration now comes from the database.
- Manual mode uses the `dev` profile automatically (see KnpianoBatchApplication.java:59)
- Web service mode uses the `prod` profile automatically (see KnpianoBatchApplication.java:87)
- Never commit email credentials; use environment variables
- The DatabaseConnectionWaiter.java ensures database is ready before application starts
- Job execution parameters always include: baseDate, jobMode, businessModule, timestamp
