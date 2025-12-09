# API 自动化测试平台 - V1.0

## 项目概述

这是一个基于 Spring Boot + Vue 3 的 API 自动化测试平台，支持测试用例管理、测试执行和结果报告。

## 项目结构

```
GmnTestPlatform/
├── backend/              # 后端项目（Spring Boot）
│   ├── src/
│   ├── pom.xml
│   └── README.md
├── frontend-app/         # 前端项目（Vue 3）
│   ├── src/
│   ├── package.json
│   └── vite.config.js
└── doc/                  # 项目文档
    ├── api_endpoints.md
    ├── frontend_design.md
    ├── frontend_guide.md
    ├── frontend_prototype.md
    ├── backend_tasks.md
    ├── frontend_tasks.md
    └── test_tasks.md
```

## 核心功能

### 后端功能（Backend）
1. **测试用例管理**
   - 创建、查询、更新、删除测试用例
   - 支持 HTTP 方法配置（GET/POST/PUT/DELETE）
   - 支持请求体、断言脚本、变量提取脚本

2. **测试执行引擎**
   - 串行执行测试用例
   - 支持环境切换（dev/staging/prod）
   - 前置条件检查
   - Groovy 脚本执行（断言和变量提取）

3. **测试结果**
   - 实时返回执行结果
   - 包含状态、耗时、详细信息

### 前端功能（Frontend）
1. **测试用例列表页**
   - 展示所有测试用例
   - 搜索和筛选
   - CRUD 操作

2. **测试执行页**
   - 选择执行环境
   - 实时显示执行进度
   - 统计数据展示

3. **测试报告页**
   - 通过率统计
   - 详细结果表格

## 技术栈

### 后端
- Spring Boot 3.2.0
- Spring Data JPA
- MySQL
- Groovy (脚本引擎)
- Allure (测试报告)

### 前端
- Vue 3 (Composition API)
- Element Plus
- Vue Router
- Pinia
- Axios
- Vite

## 快速开始

### 后端启动
```bash
cd backend
mvn spring-boot:run
# 运行在 http://localhost:7777
```

### 前端启动
```bash
cd frontend-app
npm install
npm run dev
# 运行在 http://localhost:8888
```

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://10.48.0.13:3306/TestPlatform
    username: insp_dev
    password: insp_dev@123
```

## API 接口

### 测试用例管理
- `GET /api/cases` - 获取所有用例
- `POST /api/cases` - 创建用例
- `PUT /api/cases/{id}` - 更新用例
- `DELETE /api/cases/{id}` - 删除用例

### 测试执行
- `POST /api/cases/execute?envKey={env}` - 执行测试

## 已知问题和限制

1. 暂不支持模块管理功能
2. 代码编辑器使用简单文本框，未集成 Monaco Editor
3. 测试报告无图表可视化
4. 不支持并发执行
5. 无历史执行记录

## 后续优化方向

1. 添加模块管理功能
2. 集成 Monaco Editor 代码编辑器
3. 使用 ECharts 添加图表
4. 支持 WebSocket 实时日志推送
5. 添加用户认证和权限管理
6. 支持测试报告导出（HTML/PDF）

## 文档说明

- `api_endpoints.md` - API 接口文档
- `frontend_design.md` - 前端设计方案
- `frontend_guide.md` - 前端使用指南
- `frontend_prototype.md` - 前端原型设计
- `backend_tasks.md` - 后端任务清单
- `frontend_tasks.md` - 前端任务清单
- `test_tasks.md` - 测试任务清单

## 版本信息

- **版本**: V1.0
- **发布日期**: 2025-12-02
- **作者**: API Test Platform Team
