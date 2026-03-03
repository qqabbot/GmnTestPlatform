# GMN 测试平台

一个企业级的自动化测试平台，支持 API、UI、App 和性能测试。

## 🚀 项目结构

```
GmnTestPlatform/
├── backend/              # 后端服务（Spring Boot 3.2.0）
├── frontend-app/         # 前端应用（Vue 3 + Vite）
└── doc/                  # 项目文档
```

---

## ⚡ 快速开始

### 启动后端
```bash
cd backend
mvn spring-boot:run
```
👉 后端运行在 **http://localhost:4000**

### 启动前端
```bash
cd frontend-app
npm install  # 首次运行需要安装依赖
npm run dev
```
👉 前端运行在 **http://localhost:5000**

### 插入测试数据（可选）
```bash
./insert_test_data.sh
```

---

## 📋 核心功能

### ✅ 已实现功能（Phase 3.1）

#### API 测试
- ✅ **项目与模块管理** - 层级化组织测试用例
- ✅ **测试用例编辑器** - 基于 Monaco Editor 的专业编辑器
- ✅ **步骤化测试** - 支持多步骤 API 流程测试
- ✅ **变量管理** - 环境变量、项目变量、模块变量（支持继承与覆盖）
- ✅ **Dry Run** - 预览变量解析，不发送真实请求
- ✅ **执行引擎** - WebClient 响应式执行，支持超时、重试、熔断
- ✅ **报告中心** - ECharts 可视化 + PDF 导出
- ✅ **Allure 报告** - 详细的执行日志与指标
- ✅ **Prometheus 监控** - 实时性能指标

#### 高级特性
- ✅ **Circuit Breaker** - 防止系统过载（Resilience4j）
- ✅ **变量加密** - 敏感信息加密存储（Jasypt）
- ✅ **JSON Schema 验证** - 自动生成 Diff 报告
- ✅ **SpEL 表达式** - 动态变量（如 `${T(System).currentTimeMillis()}`）
- ✅ **JSONPath 提取** - 从响应中提取数据

### ⏭️ 规划中（Phase 3.2+）
- ⏭️ UI 自动化测试（Selenium/Playwright）
- ⏭️ App 自动化测试（Appium）
- ⏭️ 性能测试（JMeter/Gatling）
- ⏭️ 变量遮罩 UI
- ⏭️ 批量导入/导出
- ⏭️ CI/CD 集成

---

## 💡 核心概念

### 什么是 Step（测试步骤）？

**Step** 让一个测试用例可以包含**多个连续的 API 请求**，实现复杂业务流程测试。

#### 使用场景示例：

```
Test Case: "完整下单流程"
├─ Step 1: 用户登录 (POST /login)
│  └─ 提取 token 到变量
├─ Step 2: 获取商品信息 (GET /products/123)
│  └─ 提取 product_id 到变量
├─ Step 3: 添加到购物车 (POST /cart)
│  └─ 使用 Step 1 的 token
├─ Step 4: 创建订单 (POST /orders)
│  └─ 使用 token 和 product_id
└─ Step 5: 支付订单 (POST /orders/{id}/pay)
   └─ 验证订单状态
```

#### Step 的优势：
✅ **变量传递** - 前一步的响应数据可以提取并用于后续步骤  
✅ **业务流程** - 模拟真实用户操作流程  
✅ **可复用** - 步骤可以复制、重排序（拖拽支持）  
✅ **可维护** - 每个步骤独立编辑，逻辑清晰  

---

### Dry Run vs Run 的区别

| 特性 | **Dry Run** 🔍 | **Run** ▶️ |
|------|---------------|-----------|
| **是否发送请求** | ❌ 不发送 | ✅ 发送真实 HTTP 请求 |
| **作用** | 预览变量解析结果 | 执行测试并返回真实响应 |
| **返回结果** | 解析后的 URL、Body、Headers | 实际的 Response、状态码、耗时 |
| **安全性** | 安全，不会修改数据 | 会执行真实操作（可能修改数据） |
| **用途** | 调试变量、检查语法 | 真正的测试执行 |

#### Dry Run 示例

**配置**：
```
URL: ${base_url}/users/${user_id}
Body: {"name": "张三", "timestamp": "${T(System).currentTimeMillis()}"}
Environment: dev
```

**Dry Run 返回**：
```json
{
  "resolvedUrl": "https://api-dev.example.com/users/1001",
  "resolvedBody": "{\"name\": \"张三\", \"timestamp\": \"1733294395000\"}",
  "variables": {
    "base_url": "https://api-dev.example.com",
    "user_id": "1001"
  }
}
```
👉 **只是预览**，没有真正发送请求

#### Run 示例

**同样的配置 → Run 执行后返回**：
```json
{
  "status": "PASS",
  "statusCode": 200,
  "duration": 245,
  "request": {
    "method": "PUT",
    "url": "https://api-dev.example.com/users/1001",
    "body": "{\"name\": \"张三\", \"timestamp\": \"1733294395000\"}"
  },
  "response": {
    "statusCode": 200,
    "body": "{\"id\": 1001, \"name\": \"张三\", \"updated\": true}"
  }
}
```
👉 **真正执行了 HTTP 请求**，返回服务器真实响应

---

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 3.2.0
- **数据库**: PostgreSQL
- **HTTP 客户端**: WebClient (Reactive)
- **韧性**: Resilience4j (Circuit Breaker, Retry, Rate Limiter)
- **脚本引擎**: Groovy
- **报告**: Allure
- **监控**: Micrometer + Prometheus
- **安全**: Jasypt (加密)
- **验证**: JSON Schema (Everit)
- **数据处理**: JSONPath, SpEL

### 前端
- **框架**: Vue 3 (Composition API)
- **构建工具**: Vite
- **UI 库**: Element Plus
- **状态管理**: Pinia
- **编辑器**: Monaco Editor
- **图表**: ECharts
- **PDF**: jsPDF + html2canvas
- **拖拽**: vuedraggable

---

## 🐳 Docker 部署（CentOS 服务器）

在项目根目录执行：

```bash
docker compose build --no-cache
docker compose up -d
```

- 前端访问：**http://&lt;服务器IP&gt;:5000**
- 后端 API：**http://&lt;服务器IP&gt;:4000**

代码更新后可在服务器执行 `./scripts/deploy.sh` 拉取并重新部署。详细步骤、部署脚本说明、使用已有 MySQL、常用命令与故障排查见 [Docker 部署指南](doc/deploy-docker.md)。

### 查看部署日志

在服务器项目根目录执行：

```bash
# 使用脚本：先看容器状态，加 -f 可持续输出日志
./scripts/status.sh
./scripts/status.sh -f

# 或直接用 Docker Compose
sudo docker compose logs          # 最近日志
sudo docker compose logs -f       # 持续跟踪所有服务
sudo docker compose logs -f backend   # 仅后端
sudo docker compose logs -f frontend  # 仅前端
sudo docker compose logs --tail=200 -f  # 最近 200 行并持续跟踪
```

### 后端启动报错：Failed to determine suitable jdbc url

该错误表示**未配置数据源**，后端拿不到 `SPRING_DATASOURCE_URL`。当前使用外部 MySQL，需在**项目根目录**（与 `docker-compose.yml` 同目录）配置：

1. **方式一**：创建 `.env` 文件（可复制 `.env.example` 后修改）：
   ```bash
   cp .env.example .env
   # 编辑 .env，填写实际 MySQL 地址、用户名、密码
   ```
2. **方式二**：在执行 `docker compose up` 前导出环境变量，或在 Jenkins 中配置注入 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`。

确保 `SPRING_DATASOURCE_URL` 为完整 JDBC 地址，例如：`jdbc:mysql://<MySQL主机>:3306/TestPlatform?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true`。

### UI 测试执行模式

- **服务器执行（Server Execution）**：在服务器上运行，**固定使用无头模式**（无图形界面），适合定时任务、CI 或无人值守场景。
- **本地执行（Local Execution）**：在**使用者本机**运行，通过 [Playwright Local Agent](playwright-local-agent/README.md) 使用真实浏览器进行**执行、录制与回放**。

**本机没有 Local Agent 时如何操作：**

1. 本机安装 **Node.js 18+**（若未安装，请到 [nodejs.org](https://nodejs.org/) 下载）。
2. 在**项目根目录**（与 `playwright-local-agent` 同级）打开终端，执行：
   ```bash
   cd playwright-local-agent && npm install && npx playwright install && npm start
   ```
3. 看到 Agent 监听 `127.0.0.1:9933` 后，在平台中选择「本地执行」并点击执行或录制即可。  
   首次运行会下载 Chromium 等浏览器，可能需要几分钟。详细说明见 [playwright-local-agent/README.md](playwright-local-agent/README.md)。

### UI 测试报错：Failed to create driver / 无法创建 driver

该错误说明**当前运行的后端镜像里没有 Playwright 的浏览器环境**（或 Playwright 版本与镜像不一致）。本仓库的 backend 镜像已改为基于 [Playwright Java 官方镜像](https://playwright.dev/java/docs/docker) 构建，需**用最新代码重新构建并部署**：

1. 在服务器上拉取最新 main：`git pull origin main`
2. 重新构建后端镜像（不要用旧镜像）：  
   `docker compose build backend --no-cache`
3. 重新启动：`docker compose up -d`

若仍报错，确认日志中 Playwright 版本与 `backend/pom.xml` 中一致（当前为 1.50.0），且 `docker-compose.yml` 中 backend 已配置 `ipc: host` 与 `init: true`。

---

## 📚 文档

- [API 端点文档](doc/api_endpoints.md)
- [Docker 部署指南](doc/deploy-docker.md)（含 Jenkins 流水线说明）
- [后端任务清单](doc/task/backend_tasks.md)
- [前端任务清单](doc/task/frontend_tasks.md)
- [Phase 3.1 功能清单](doc/task/phase3.1_tasks.md)
- [Phase 3.2 规划](doc/task/phase3.2_tasks.md)

---

## 🎯 最佳实践

1. **开发阶段**：先用 **Dry Run** 检查变量是否正确解析
2. **测试阶段**：确认无误后再用 **Run** 执行真实测试
3. **Step 使用**：复杂业务流程用多个 Step，简单请求用单个 Test Case
4. **变量提取**：在 Step 中使用 JSONPath 提取响应数据供后续步骤使用
5. **环境隔离**：使用不同环境变量隔离 dev/test/prod 配置

---

## 🌟 特色功能

### 变量继承机制
```
Environment Variables (全局)
  ↓ (覆盖)
Project Variables (项目级)
  ↓ (覆盖)
Module Variables (模块级)
```

### 支持的变量语法
- `${variable_name}` - 普通变量
- `${T(System).currentTimeMillis()}` - SpEL 表达式
- JSONPath 提取: `$.data.user.id`

### 支持的断言
- 状态码检查: `status_code == 200`
- 响应体验证: `response.data.id == 1001`
- JSON Schema 验证
- 自定义 Groovy 脚本

---

## 🔐 安全特性

- ✅ Secret 变量加密存储（AES-256）
- ✅ 日志脱敏
- ⏭️ 用户认证与授权（Phase 3.2）
- ⏭️ API 接口鉴权（Phase 3.2）

---

## 📊 监控与指标

访问 **http://localhost:4000/actuator/prometheus** 查看实时指标：
- JVM 内存、GC、线程
- HTTP 请求耗时
- Circuit Breaker 状态
- 数据库连接池

集成 Grafana 可实现可视化监控。

---

## 版本历史

- **V3.1** (2025-12-04) - Step 架构、Monaco Editor、ECharts Dashboard
- **V3.0** (2025-12-03) - 环境与变量管理
- **V2.0** (2025-12-02) - 执行引擎与报告
- **V1.0** (2025-12-02) - 基础 CRUD 功能

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可

MIT License

---

**Powered by GMN Team** 🚀
