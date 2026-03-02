# Docker 部署指南（CentOS）

本文说明在 CentOS 服务器上使用 Docker 部署 GMN 测试平台。

**端口与 MySQL 说明**：

- **前端**：宿主机端口 **5000**（容器内 nginx 仍为 80）
- **后端**：宿主机端口 **4000**
- **MySQL**：使用 `docker-compose.yml` 中当前配置（库 `TestPlatform`，用户 `testplatform`），**不会**在启动时自动执行建表 SQL；请使用已有数据库或自行按 `doc/sql/` 下脚本初始化表结构。

## 前置条件

- CentOS 7+（建议 CentOS 8 或 Rocky/AlmaLinux 8+）
- 已安装 Docker 与 Docker Compose

### 安装 Docker（若未安装）

```bash
# CentOS 7
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io
sudo systemctl start docker
sudo systemctl enable docker

# CentOS 8 / Rocky Linux 8
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo systemctl start docker
sudo systemctl enable docker
```

### 安装 Docker Compose（若使用独立命令）

若未使用 `docker compose` 插件，可安装独立版本：

```bash
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

## 部署步骤

### 1. 上传代码到服务器

将项目根目录（含 `docker-compose.yml`、`backend/`、`frontend-app/`）上传到服务器，例如 `/home/carsome/QA/GmnTestPlatform`。MySQL 使用当前配置（`docker-compose.yml` 中的环境变量），**不会**自动执行建表 SQL 脚本，请使用已有数据库或自行执行 `doc/sql/` 下的脚本初始化表结构。

```bash
# 在服务器上进入项目目录
cd /home/carsome/QA/GmnTestPlatform
```

### 2. 构建并启动

```bash
# 构建镜像并启动所有服务（首次或代码更新后）
sudo docker compose build --no-cache
sudo docker compose up -d

# 或使用独立 docker-compose 命令
sudo docker-compose build --no-cache
sudo docker-compose up -d
```

### 3. 查看状态与日志

```bash
# 查看容器状态
sudo docker compose ps

# 查看日志（全部）
sudo docker compose logs -f

# 仅后端
sudo docker compose logs -f backend
# 仅前端
sudo docker compose logs -f frontend
# 仅 MySQL
sudo docker compose logs -f mysql
```

### 4. 访问应用

- **前端（浏览器访问）**：`http://<服务器IP>:5000`
- **后端 API**：`http://<服务器IP>:4000/api`（如需直接调 API）
- **MySQL**：`<服务器IP>:3306`，数据库 `TestPlatform`，用户 `testplatform`，密码 `testplatform`（仅内网或按需开放）

## 常用命令

```bash
# 停止所有服务
sudo docker compose down

# 停止并删除数据卷（会清空数据库与上传文件，慎用）
sudo docker compose down -v

# 仅重启后端
sudo docker compose restart backend

# 仅重启前端
sudo docker compose restart frontend

# 重新构建并启动（代码或 Dockerfile 变更后）
sudo docker compose build --no-cache && sudo docker compose up -d
```

## 代码更新与部署脚本

项目提供了一组 Shell 脚本，用于在服务器上拉取代码并完成部署，无需每次手动执行多条命令。

### 脚本位置与权限

脚本位于项目根目录下的 `scripts/` 目录：

```bash
cd /home/carsome/QA/GmnTestPlatform
chmod +x scripts/*.sh
```

若使用 `sudo` 运行 Docker，建议用 root 或同一用户执行脚本，或为对应用户配置 Docker 免 sudo（将用户加入 `docker` 组）。

### 配置文件 `scripts/deploy.conf`

首次使用前可按需编辑 `scripts/deploy.conf`：

| 变量 | 含义 | 默认值 |
|------|------|--------|
| `PROJECT_ROOT` | 项目根目录（含 docker-compose.yml） | `/home/carsome/QA/GmnTestPlatform` |
| `GIT_BRANCH` | 拉取的分支 | `main` |
| `GIT_PULL_ENABLED` | 是否在部署前执行 git pull | `true` |
| `DOCKER_COMPOSE_CMD` | Docker Compose 命令 | `docker compose` |
| `BUILD_NO_CACHE` | 是否使用 `build --no-cache` | `false` |

也可通过环境变量覆盖，例如：`export PROJECT_ROOT=/home/carsome/QA/GmnTestPlatform; ./scripts/deploy.sh`。

### 一键拉代码并部署（推荐）

每次代码更新后，在服务器上执行：

```bash
cd /home/carsome/QA/GmnTestPlatform
./scripts/deploy.sh
```

脚本会依次：**拉取当前分支最新代码 → 构建镜像 → 启动/更新容器**。

可选参数：

- `--no-pull`：不拉代码，仅构建并启动（代码已手动拉过时使用）
- `--no-cache`：构建时使用 `docker compose build --no-cache`（更干净，耗时更长）
- `--pull-only`：只拉取代码，不构建、不重启服务
- `-h` / `--help`：显示帮助

示例：

```bash
# 拉代码 + 构建 + 启动（使用缓存构建，较快）
./scripts/deploy.sh

# 拉代码 + 无缓存构建 + 启动（推荐在依赖或 Dockerfile 变更后使用）
./scripts/deploy.sh --no-cache

# 仅拉取代码，不构建不重启
./scripts/deploy.sh --pull-only

# 不拉代码，仅重新构建并启动
./scripts/deploy.sh --no-pull --no-cache
```

### 其他脚本

| 脚本 | 作用 |
|------|------|
| `scripts/pull.sh` | 仅拉取最新代码（可传分支名，如 `./scripts/pull.sh main`） |
| `scripts/restart.sh` | 仅重启容器，不拉代码、不重新构建（可传服务名，如 `./scripts/restart.sh backend frontend`） |
| `scripts/status.sh` | 查看容器状态；加 `-f` 可持续输出日志（如 `./scripts/status.sh -f`） |

### 典型更新流程

1. 在开发机提交并推送到 Git 仓库。
2. 登录服务器，进入项目目录并执行部署脚本：

   ```bash
   cd /home/carsome/QA/GmnTestPlatform
   ./scripts/deploy.sh
   ```

3. 需要无缓存重建时：

   ```bash
   ./scripts/deploy.sh --no-cache
   ```

4. 查看服务是否正常：

   ```bash
   ./scripts/status.sh
   # 或持续看日志
   ./scripts/status.sh -f
   ```

## Jenkins 部署

项目根目录提供 `Jenkinsfile`，使用 Declarative Pipeline，通过 Docker Compose 构建并启动服务。

### 前置条件

- Jenkins 所在节点（或执行机）已安装 **Docker** 与 **Docker Compose**（或 `docker compose` 插件）。
- Jenkins 用户有权限执行 Docker（或将用户加入 `docker` 组；若需 sudo，在流水线参数中选择 `sudo docker compose`）。

### 新建流水线任务

1. **新建任务** → 输入任务名称 → 选择 **“流水线”** → 确定。
2. **流水线** 配置：
   - **定义**：选择 **“Pipeline script from SCM”**。
   - **SCM**：选择 **Git**，填写仓库 URL、凭据，分支填 `*/main`（或你要监听的分支）。
   - **脚本路径**：`Jenkinsfile`（项目根目录下的 Jenkinsfile）。
3. **保存**。

### 参数说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `BRANCH` | `main` | 要构建并部署的 Git 分支 |
| `BUILD_NO_CACHE` | `false` | 为 true 时执行 `docker compose build --no-cache` |
| `DOCKER_COMPOSE_CMD` | `docker compose` | 若节点上需 sudo，选 `sudo docker compose` |

### 执行流程

流水线分为三阶段：

1. **Checkout**：检出仓库；若指定了 `BRANCH`，会 `git fetch` 并切换到该分支。
2. **Build**：在 Jenkins 工作空间执行 `docker compose build`（可选 `--no-cache`）。
3. **Deploy**：执行 `docker compose up -d` 启动/更新容器。

构建完成后，前端访问 `http://<服务器IP>:5000`，后端 API 为 `http://<服务器IP>:4000/api`。

### 注意事项

- 若 Jenkins 与 Docker 不在同一台机器（如 Jenkins 在 Master、构建在 Agent），需在 **执行构建的节点** 上安装 Docker，且该节点的工作空间路径一致（或保证 `docker-compose.yml` 所在目录即代码检出目录）。
- 若使用 **独立 Docker Compose 可执行文件**（如 `docker-compose` 命令），需在 Jenkinsfile 或节点环境中将 `DOCKER_COMPOSE_CMD` 改为实际命令（如 `docker-compose`），或在 Jenkins 中增加对应参数选项。

## 使用已有 MySQL（不启动容器内 MySQL）

若使用服务器上已有 MySQL，可不启动 compose 中的 `mysql` 服务：

1. 在 MySQL 中创建数据库和用户：

```sql
CREATE DATABASE TestPlatform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'testplatform'@'%' IDENTIFIED BY '你的密码';
GRANT ALL ON TestPlatform.* TO 'testplatform'@'%';
FLUSH PRIVILEGES;
```

2. 执行项目中的建表脚本（按顺序）：  
   `doc/sql/mybatis_schema.sql`、`migration_phase7_scenarios.sql`、`ui_test_schema.sql`、`migration_phase8.1_dashboard_nav.sql`、`migration_phase8_execution_history.sql`（均在 `doc/sql/` 目录下）。

3. 修改 `docker-compose.yml`：注释或删除 `mysql` 服务；在 `backend` 的 `environment` 中设置：

```yaml
environment:
  SPRING_PROFILES_ACTIVE: docker
  SPRING_DATASOURCE_URL: jdbc:mysql://<MySQL主机>:3306/TestPlatform?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
  SPRING_DATASOURCE_USERNAME: testplatform
  SPRING_DATASOURCE_PASSWORD: 你的密码
```

4. 启动时只起 backend 和 frontend：

```bash
sudo docker compose up -d backend frontend
```

## 可选：插入测试数据

后端与 MySQL 就绪后，可在本机或服务器上执行：

```bash
# 将 BASE_URL 改为实际后端地址，例如 http://<服务器IP>:4000/api
export BASE_URL="http://<服务器IP>:4000/api"
# 然后执行项目根目录下的
./insert_test_data.sh
```

或在服务器上：

```bash
curl -s -X POST "http://localhost:4000/api/environments" \
  -H "Content-Type: application/json" \
  -d '{"envName":"dev","description":"开发环境","domain":"https://api-dev.example.com"}'
# 其余见 insert_test_data.sh
```

## 故障排查

- **后端启动报错 “Communications link failure”**：多为 MySQL 未就绪，等几秒后 `docker compose restart backend`，或确认 `SPRING_DATASOURCE_*` 与 MySQL 地址、库名、用户、密码一致。
- **前端访问 502 / 无法打开**：确认 `frontend` 容器在运行，且 nginx 将 `/api` 代理到 `backend:4000`（见 `frontend-app/nginx.conf`）。
- **端口占用**：修改 `docker-compose.yml` 中 `ports`（如 `"5001:80"`、`"4001:4000"`）避免与宿主机冲突。

## 安全建议

- 生产环境务必修改 MySQL 的 `MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD` 以及后端的 `SPRING_DATASOURCE_PASSWORD`。
- 避免将 3306、4000、5000 等端口对公网开放，必要时用 Nginx 做反向代理并配置 HTTPS。
- 敏感配置（如 Jasypt、Gemini API Key）通过环境变量或外部配置注入，不要写死在镜像中。
