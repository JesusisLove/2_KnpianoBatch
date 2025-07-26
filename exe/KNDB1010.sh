#!/bin/bash

# KNPiano Batch 执行脚本

echo "KNPiano Batch Application"
echo "========================="

# 检查参数
if [ $# -eq 0 ]; then
    echo "使用方法:"
    echo "  $0 auto                    - 自动执行模式"
    echo "  $0 manual YYYYMMDD         - 手动执行模式"
    echo "  $0 test                    - 测试模式（使用月末日期）"
    echo ""
    echo "示例:"
    echo "  $0 auto"
    echo "  $0 manual 20250831"
    echo "  $0 test"
    exit 1
fi

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 进入项目根目录（脚本在exe子目录中，所以需要回到上级目录）
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

echo "当前工作目录: $(pwd)"
echo "检查pom.xml文件..."

if [ ! -f "pom.xml" ]; then
    echo "错误: 找不到pom.xml文件，请确认脚本在正确的项目目录中运行"
    echo "当前目录: $(pwd)"
    exit 1
fi

echo "编译项目..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "编译失败!"
    exit 1
fi

echo "启动批处理..."

if [ "$1" = "auto" ]; then
    echo "执行模式: 自动"
    mvn spring-boot:run -Dspring-boot.run.arguments="--job.name=KNDB1010_AUTO"
elif [ "$1" = "manual" ]; then
    if [ -z "$2" ]; then
        echo "错误: 手动模式需要指定日期参数 (YYYYMMDD)"
        exit 1
    fi
    echo "执行模式: 手动, 基准日期: $2"
    mvn spring-boot:run -Dspring-boot.run.arguments="--job.name=KNDB1010_MANUAL --base.date=$2"
elif [ "$1" = "test" ]; then
    # 获取当前月的最后一天进行测试
    if command -v gdate >/dev/null 2>&1; then
        # macOS 使用 gdate (需要安装 brew install coreutils)
        LAST_DAY=$(gdate -d "$(gdate +'%Y-%m-01') +1 month -1 day" +'%Y%m%d')
    else
        # Linux 使用 date
        LAST_DAY=$(date -d "$(date +'%Y-%m-01') +1 month -1 day" +'%Y%m%d')
    fi
    echo "执行模式: 测试, 使用月末日期: $LAST_DAY"
    mvn spring-boot:run -Dspring-boot.run.arguments="--job.name=KNDB1010_MANUAL --base.date=$LAST_DAY"
else
    echo "错误: 不支持的执行模式 '$1'"
    echo "支持的模式: auto, manual, test"
    exit 1
fi