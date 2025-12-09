# 后端开发任务清单 - V3.1

## Phase 1 & 2 已完成 ✅

### 1. 项目初始化
- ✅ 创建 Spring Boot 项目
- ✅ 配置 Maven 依赖
- ✅ 配置数据库连接（PostgreSQL）

### 2. 核心数据模型
- ✅ 创建 Project 实体
- ✅ 创建 TestModule 实体
- ✅ 创建 TestCase 实体
- ✅ 创建 TestStep 实体（步骤架构）
- ✅ 创建 Extractor 实体（变量提取器）
- ✅ 创建 Assertion 实体（断言）
- ✅ 创建 Environment 实体
- ✅ 创建 GlobalVariable 实体
- ✅ 配置所有 JPA 关系映射

### 3. 数据访问层
- ✅ 实现所有 Repository 接口
- ✅ 自定义查询方法
- ✅ 解决 JPA 查询方法命名问题

### 4. 核心业务逻辑
- ✅ 实现 ProjectService
- ✅ 实现 TestModuleService
- ✅ 实现 TestCaseService
- ✅ 实现 EnvironmentService
- ✅ 实现 GlobalVariableService
- ✅ 实现测试执行引擎 (TestExecutionEngine)

### 5. 控制器层
- ✅ ProjectController (完整 CRUD)
- ✅ TestModuleController (完整 CRUD)
- ✅ TestCaseController (完整 CRUD + 执行 + Dry Run)
- ✅ EnvironmentController (完整 CRUD)
- ✅ GlobalVariableController (完整 CRUD)
- ✅ ReportController (查询执行历史)

---

## Phase 3 & 3.1 已完成 ✅

### 1. 执行引擎升级
- ✅ **WebClient 迁移**
  - ✅ 替换 RestTemplate 为 WebClient（响应式/非阻塞）
  - ✅ 支持所有 HTTP 方法（GET/POST/PUT/DELETE/PATCH/HEAD/OPTIONS）
  - ✅ 添加超时配置（connectTimeout/readTimeout）
- ✅ **高级请求处理**
  - ✅ 支持 multipart/form-data 文件上传/下载
  - ✅ 内置认证：Basic Auth, Bearer Token, API Key, Digest, OAuth2
- ✅ **韧性与可靠性**
  - ✅ Retry 机制（指数退避，Resilience4j）
  - ✅ Circuit Breaker / Rate Limiting（防止系统过载）
- ✅ **动态处理**
  - ✅ 集成 JSONPath（Jayway）进行变量提取
  - ✅ 实现 SpEL（Spring Expression Language）动态变量替换

### 2. 高级变量与环境
- ✅ **安全性**
  - ✅ GlobalVariable 添加 type 字段（normal/secret）
  - ✅ 使用 Jasypt 加密 secret 变量
- ✅ **层级与逻辑**
  - ✅ 支持 Project/Module 级别变量继承/覆盖
  - ✅ 支持 SpEL 动态变量（如 `${T(System).currentTimeMillis()}`）

### 3. 报告与日志
- ✅ **Allure 增强**
  - ✅ 自动附加完整 Request/Response JSON（带高亮）
  - ✅ 自动附加指标：响应时间、状态码、Body 大小
  - ✅ JSON Schema 验证失败时附加 Diff
- ✅ **日志记录**
  - ✅ 持久化执行日志到数据库（带脱敏）
  - ✅ "Dry Run" 模式（解析变量但不发送请求）

### 4. 监控与指标
- ✅ 集成 Spring Boot Actuator
- ✅ 集成 Micrometer + Prometheus
- ✅ 暴露 `/actuator/health` 健康检查
- ✅ 暴露 `/actuator/metrics` 指标列表
- ✅ 暴露 `/actuator/prometheus` Prometheus 格式指标

### 5. JSON Schema 验证
- ✅ 集成 Everit JSON Schema
- ✅ 支持断言中的 Schema 验证
- ✅ 验证失败时生成详细 Diff 报告

---

## 问题修复 ✅

### 历史问题
- ✅ 修复 Groovy 依赖问题（groupId 更正）
- ✅ 修复 Allure.step 编译错误
- ✅ 修复 JPA 懒加载异常
- ✅ 修复 application.yml 配置问题

### Phase 3.1 修复
- ✅ 添加缺失的 `GET /api/cases/{id}` 端点
- ✅ 修复 TestCase 创建时缺少 URL 和 Body 字段的问题
- ✅ 改进编辑模式的错误处理
- ✅ 增强必填字段验证

---

## Phase 3.2 待实现功能 ⏭️

### 1. 执行控制
- ⏭️ 添加执行状态管理（RUNNING/STOPPED/COMPLETED）
- ⏭️ 实现执行取消机制
- ⏭️ 添加执行超时自动清理

### 2. WireMock 集成（可选）
- ⏭️ 集成 WireMock Standalone 用于本地 Mocking
- ⏭️ 创建 Mock 配置管理 API
- ⏭️ 支持从录制请求生成 Stub

### 3. 高级功能
- ⏭️ API 版本控制
- ⏭️ 基于 Cron 的定时调度执行
- ⏭️ 数据导出到外部系统（JIRA、Slack 等）
- ⏭️ Webhook 支持

### 4. 测试与质量
- ⏭️ 添加单元测试（目标覆盖率 80%+）
- ⏭️ 集成测试（Testcontainers + PostgreSQL）
- ⏭️ 性能测试（100 并发用例）

### 5. 安全性
- ⏭️ 用户认证与授权
- ⏭️ API 接口鉴权
- ⏭️ HTTPS 强制
- ⏭️ Rate Limiting on API endpoints
- ⏭️ SQL 注入防护验证

### 6. 优化
- ⏭️ 添加缓存机制
- ⏭️ 优化数据库查询
- ⏭️ 连接池优化
- ⏭️ 异步处理

---

## 技术栈总结

### 核心框架
- Spring Boot 3.2.0
- Spring WebFlux (WebClient)
- Spring Data JPA
- PostgreSQL

### 测试与质量
- Allure Reporting
- Groovy Scripting
- JSON Schema Validation (Everit)

### 韧性与监控
- Resilience4j (Circuit Breaker, Retry, Rate Limiter)
- Micrometer + Prometheus
- Spring Boot Actuator

### 安全
- Jasypt (变量加密)

### 工具库
- JSONPath (Jayway)
- SpEL (Spring Expression Language)
- Apache Commons FileUpload

---

## 优先级

### P0（高优先级 - Phase 3.2）
- 单元测试与集成测试
- 用户认证与授权
- 执行状态管理

### P1（中优先级 - 未来）
- 定时调度执行
- 性能优化
- 高级报告功能

### P2（低优先级 - 可选）
- WireMock 集成
- API 版本控制
- 高级数据导出
