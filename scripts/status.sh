#!/usr/bin/env sh
#
# 查看服务状态与最近日志
# 用法：./status.sh [-f]  （-f 表示持续输出日志）
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

echo "========== 容器状态 =========="
${DOCKER_COMPOSE_CMD} ps

if [ "${1:-}" = "-f" ]; then
  echo ""
  echo "========== 日志 (Ctrl+C 退出) =========="
  ${DOCKER_COMPOSE_CMD} logs -f
fi
