#!/usr/bin/env sh
#
# GMN 测试平台 - 服务器部署脚本
# 功能：拉取代码 → 构建镜像 → 启动/更新服务
# 用法：./deploy.sh [--no-pull] [--no-cache] [--pull-only]
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
PROJECT_ROOT="${PROJECT_ROOT:-/home/carsome/QA/GmnTestPlatform}"
GIT_BRANCH="${GIT_BRANCH:-main}"
GIT_PULL_ENABLED="${GIT_PULL_ENABLED:-true}"
DOCKER_COMPOSE_CMD="${DOCKER_COMPOSE_CMD:-docker compose}"
BUILD_NO_CACHE="${BUILD_NO_CACHE:-false}"

# 解析参数
DO_PULL="${GIT_PULL_ENABLED}"
DO_BUILD="true"
DO_UP="true"
NO_CACHE="${BUILD_NO_CACHE}"

for arg in "$@"; do
  case "${arg}" in
    --no-pull)   DO_PULL="false" ;;
    --no-cache)   NO_CACHE="true" ;;
    --pull-only) DO_BUILD="false"; DO_UP="false" ;;
    -h|--help)
      echo "用法: $0 [选项]"
      echo "选项:"
      echo "  --no-pull    不执行 git pull，仅构建并启动"
      echo "  --no-cache   构建镜像时使用 docker compose build --no-cache"
      echo "  --pull-only  仅拉取代码，不构建、不启动"
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

# ---------- 1. 拉取代码 ----------
if [ "${DO_PULL}" = "true" ]; then
  _log ">>> 拉取代码 (分支: ${GIT_BRANCH})"
  if ! [ -d .git ]; then
    _log "错误: 当前目录不是 Git 仓库: ${PROJECT_ROOT}"
    exit 1
  fi
  git fetch origin
  git checkout "${GIT_BRANCH}"
  git pull origin "${GIT_BRANCH}"
  _log ">>> 拉取完成"
else
  _log ">>> 跳过拉取 (--no-pull)"
fi

if [ "${DO_UP}" = "false" ] && [ "${DO_BUILD}" = "false" ]; then
  _log ">>> 仅拉取模式，结束"
  exit 0
fi

# ---------- 2. 构建镜像 ----------
if [ "${DO_BUILD}" = "true" ]; then
  _log ">>> 构建 Docker 镜像"
  if [ "${NO_CACHE}" = "true" ]; then
    ${DOCKER_COMPOSE_CMD} build --no-cache
  else
    ${DOCKER_COMPOSE_CMD} build
  fi
  _log ">>> 构建完成"
fi

# ---------- 3. 启动/更新服务 ----------
if [ "${DO_UP}" = "true" ]; then
  _log ">>> 启动服务 (docker compose up -d)"
  ${DOCKER_COMPOSE_CMD} up -d
  _log ">>> 部署完成"
  _log ">>> 前端: http://<服务器IP>:5000  后端 API: http://<服务器IP>:4000/api"
fi

exit 0
