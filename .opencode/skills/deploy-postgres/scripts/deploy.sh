#!/bin/bash
# Thunder Note PostgreSQL 部署脚本
# 用法: ./deploy.sh [端口号]

set -e

CONTAINER_NAME="thunder-db"
IMAGE="postgres:17"
DB_NAME="thunder_note"
DB_USER="postgres"
DB_PASS="postgres"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MIGRATION_DIR="$PROJECT_ROOT/thunder-note-server/src/main/resources/db/migration"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查 Docker
if ! command -v docker &> /dev/null; then
    log_error "Docker 未安装，请先安装 Docker"
    exit 1
fi

# 检查端口
check_port() {
    local port=$1
    if ss -tlnp 2>/dev/null | grep -q ":$port " || netstat -tlnp 2>/dev/null | grep -q ":$port "; then
        return 1  # 端口被占用
    fi
    return 0  # 端口空闲
}

# 检查现有容器
if docker ps -a --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
    if docker ps --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
        log_info "容器 ${CONTAINER_NAME} 已在运行中"
        PORT=$(docker port ${CONTAINER_NAME} 5432 | cut -d: -f2)
        log_info "数据库连接: jdbc:postgresql://localhost:${PORT}/${DB_NAME}"
        exit 0
    else
        log_warn "容器 ${CONTAINER_NAME} 已存在但未运行"
        read -p "是否启动现有容器? (y/n): " choice
        if [[ "$choice" == "y" || "$choice" == "Y" ]]; then
            docker start ${CONTAINER_NAME}
            PORT=$(docker port ${CONTAINER_NAME} 5432 | cut -d: -f2)
            log_info "容器已启动，数据库连接: jdbc:postgresql://localhost:${PORT}/${DB_NAME}"
            exit 0
        else
            log_warn "将删除现有容器并重新创建..."
            docker rm -f ${CONTAINER_NAME}
        fi
    fi
fi

# 确定端口
if [ -n "$1" ]; then
    PORT=$1
else
    if check_port 5432; then
        PORT=5432
        log_info "使用默认端口 5432"
    else
        PORT=15432
        log_warn "端口 5432 已被占用，使用备用端口 ${PORT}"
        log_warn "请修改 application.yml 中的数据库端口为 ${PORT}"
    fi
fi

# 启动容器
log_info "启动 PostgreSQL 17 容器..."
docker run -d \
    --name ${CONTAINER_NAME} \
    -e POSTGRES_USER=${DB_USER} \
    -e POSTGRES_PASSWORD=${DB_PASS} \
    -e POSTGRES_DB=${DB_NAME} \
    -p ${PORT}:5432 \
    ${IMAGE}

# 等待就绪
log_info "等待 PostgreSQL 就绪..."
for i in {1..15}; do
    if docker exec ${CONTAINER_NAME} pg_isready -U ${DB_USER} 2>/dev/null; then
        log_info "PostgreSQL 已就绪"
        break
    fi
    if [ $i -eq 15 ]; then
        log_error "PostgreSQL 启动超时"
        exit 1
    fi
    sleep 1
done

# 执行迁移脚本
log_info "执行数据库初始化脚本..."
if [ -d "$MIGRATION_DIR" ]; then
    for sql_file in $(ls -v ${MIGRATION_DIR}/V*.sql 2>/dev/null); do
        filename=$(basename "$sql_file")
        log_info "执行: $filename"
        docker exec -i ${CONTAINER_NAME} psql -U ${DB_USER} -d ${DB_NAME} < "$sql_file"
    done
else
    log_error "迁移脚本目录不存在: $MIGRATION_DIR"
    exit 1
fi

# 验证
log_info "验证数据库表..."
docker exec ${CONTAINER_NAME} psql -U ${DB_USER} -d ${DB_NAME} -c "\dt"

echo ""
log_info "======================================"
log_info "部署完成!"
log_info "======================================"
log_info "容器名称: ${CONTAINER_NAME}"
log_info "数据库连接: jdbc:postgresql://localhost:${PORT}/${DB_NAME}"
log_info "用户名/密码: ${DB_USER} / ${DB_PASS}"
log_info ""
log_info "连接命令: docker exec -it ${CONTAINER_NAME} psql -U ${DB_USER} -d ${DB_NAME}"
