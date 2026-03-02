#!/usr/bin/env sh
#
# GMN 测试平台 - 服务器部署脚本
# 功能：构建镜像 → 启动/更新服务（不拉取代码）
# 用法：./deploy.sh [--no-cache]
#

set -e

# 脚本所在目录（用于解析 deploy.conf）
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
readonly SCRIPT_DIR

# 加载配置（若存在）
if [ -f "${SCRIPT_DIR}/deploy.conf" ]; then
  # shellcheck source=scripts/deploy.conf
  . "${SCRIPT_DIR}/deploy.conf"
fi

# 可覆盖的变量
PROJECT_ROOT="${PROJECT_ROOT:-/opt/GmnTestPlatform}"
DOCKER_COMPOSE_CMD="${DOCKER_COMPOSE_CMD:-docker compose}"
BUILD_NO_CACHE="${BUILD_NO_CACHE:-false}"

# 解析参数
NO_CACHE="${BUILD_NO_CACHE}"

for arg in "$@"; do
  case "${arg}" in
    --no-cache) NO_CACHE="true" ;;
    -h|--help)
      echo "用法: $0 [选项]"
      echo "选项:"
      echo "  --no-cache   构建镜像时使用 docker compose build --no-cache"
      echo "  -h, --help   显示此帮助"
      exit 0
      ;;
  esac
done

_log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

# 进入项目目录
if ! [ -d "${PROJECT_ROOT}" ]; then
  _log "错误: 项目目录不存在: ${PROJECT_ROOT}"
  exit 1
fi
cd "${PROJECT_ROOT}"
readonly PROJECT_ROOT

# ---------- 1. 构建镜像 ----------
_log ">>> 构建 Docker 镜像"
if [ "${NO_CACHE}" = "true" ]; then
  ${DOCKER_COMPOSE_CMD} build --no-cache
else
  ${DOCKER_COMPOSE_CMD} build
fi
_log ">>> 构建完成"

# ---------- 2. 启动/更新服务 ----------
_log ">>> 启动服务 (docker compose up -d)"
${DOCKER_COMPOSE_CMD} up -d
_log ">>> 部署完成"
_log ">>> 前端: http://<服务器IP>:5000  后端 API: http://<服务器IP>:4000/api"

exit 0
