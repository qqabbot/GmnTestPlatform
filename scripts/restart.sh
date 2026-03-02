#!/usr/bin/env sh
#
# 仅重启 Docker 服务，不拉代码、不重新构建
# 用法：./restart.sh [服务名...]，不传则重启所有服务
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
readonly SCRIPT_DIR

if [ -f "${SCRIPT_DIR}/deploy.conf" ]; then
  # shellcheck source=scripts/deploy.conf
  . "${SCRIPT_DIR}/deploy.conf"
fi

PROJECT_ROOT="${PROJECT_ROOT:-/home/carsome/QA/GmnTestPlatform}"
DOCKER_COMPOSE_CMD="${DOCKER_COMPOSE_CMD:-docker compose}"

if ! [ -d "${PROJECT_ROOT}" ]; then
  echo "错误: 项目目录不存在: ${PROJECT_ROOT}"
  exit 1
fi
cd "${PROJECT_ROOT}"

if [ $# -eq 0 ]; then
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] 重启所有服务"
  ${DOCKER_COMPOSE_CMD} restart
else
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] 重启: $*"
  ${DOCKER_COMPOSE_CMD} restart "$@"
fi
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 重启完成"
