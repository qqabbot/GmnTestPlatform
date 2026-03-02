#!/usr/bin/env sh
#
# 仅拉取最新代码，不构建、不重启服务
# 用法：./pull.sh [分支名，默认 main]
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
readonly SCRIPT_DIR

if [ -f "${SCRIPT_DIR}/deploy.conf" ]; then
  # shellcheck source=scripts/deploy.conf
  . "${SCRIPT_DIR}/deploy.conf"
fi

PROJECT_ROOT="${PROJECT_ROOT:-/opt/GmnTestPlatform}"
GIT_BRANCH="${1:-${GIT_BRANCH:-main}}"

if ! [ -d "${PROJECT_ROOT}/.git" ]; then
  echo "错误: 不是 Git 仓库: ${PROJECT_ROOT}"
  exit 1
fi

cd "${PROJECT_ROOT}"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 拉取分支: ${GIT_BRANCH}"
git fetch origin
git checkout "${GIT_BRANCH}"
git pull origin "${GIT_BRANCH}"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 拉取完成"
