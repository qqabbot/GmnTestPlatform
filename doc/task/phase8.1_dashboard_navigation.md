# Phase 8.1 Task List: Dashboard 导航页与测试环境/常用地址管理

**Goal**: 将 Dashboard 改为导航页，支持测试环境与常用地址的层级管理（增删改查），便于快速跳转与选择环境。

**状态**: 🚧 已实现（需执行迁移 SQL）  
**优先级**: 中  
**依赖**: Phase 8.0（无强依赖，可独立进行）  
**预计完成日期**: TBD

---

## 📋 功能概述

### 核心功能

1. **Dashboard 改为导航页**：首页从统计/报告改为导航入口，展示环境与常用地址的层级结构。
2. **层级结构**：环境（一组）→ 地址项（多个）。
   - 示例：**A 环境** 下包含 **A1: my**、**A2: th**（每个地址项有名称与 URL/说明）。
   - 支持多环境，每个环境下多条地址。
3. **CRUD**：支持对环境、地址项的**添加、删除、编辑**。

### 层级与数据示例

```
环境 A
├── A1  →  my   (如 https://my.example.com)
├── A2  →  th   (如 https://th.example.com)
└── A3  →  prod (如 https://prod.example.com)

环境 B
├── B1  →  dev
└── B2  →  staging
```

- **环境**：名称（如「A」「B」）、可选描述、排序。
- **地址项**：所属环境、短名称（如 A1、A2）、显示名/标签（如 my、th）、URL、可选备注。

---

## Backend

### 1. 数据模型 (Data Model)

#### 1.1 导航环境表 (dashboard_nav_environment) ✅

**目标**：存储「环境」这一层级。

- [x] 表 `dashboard_nav_environment`：id, name, description, sort_order, created_at, updated_at ✅  
**迁移脚本**: `doc/sql/migration_phase8.1_dashboard_nav.sql`

#### 1.2 导航地址项表 (dashboard_nav_address) ✅

**目标**：存储每个环境下的地址项（A1/my、A2/th）。

- [x] 表 `dashboard_nav_address`：id, environment_id (FK), short_name, label, url, remark, sort_order, created_at, updated_at ✅  
**迁移脚本**: 同上

---

### 2. API 设计 (API Endpoints)

#### 2.1 环境 CRUD ✅

- [x] `GET /api/dashboard-nav/environments` ✅
- [x] `POST /api/dashboard-nav/environments` ✅
- [x] `PUT /api/dashboard-nav/environments/:id` ✅
- [x] `DELETE /api/dashboard-nav/environments/:id`（级联删除其下地址）✅

#### 2.2 地址项 CRUD ✅

- [x] `GET /api/dashboard-nav/environments/:envId/addresses` ✅
- [x] `POST /api/dashboard-nav/environments/:envId/addresses` ✅
- [x] `PUT /api/dashboard-nav/addresses/:id` ✅
- [x] `DELETE /api/dashboard-nav/addresses/:id` ✅

#### 2.3 树形接口 ✅

- [x] `GET /api/dashboard-nav/tree` — 返回「环境 + 其下地址」树形结构 ✅

**请求/响应示例**（树形）：

```json
// GET /api/dashboard-nav/tree
[
  {
    "id": 1,
    "name": "A",
    "description": "A 环境",
    "sortOrder": 0,
    "addresses": [
      { "id": 1, "shortName": "A1", "label": "my", "url": "https://my.example.com", "remark": "" },
      { "id": 2, "shortName": "A2", "label": "th", "url": "https://th.example.com", "remark": "" }
    ]
  },
  {
    "id": 2,
    "name": "B",
    "addresses": [
      { "id": 3, "shortName": "B1", "label": "dev", "url": "https://dev.example.com", "remark": "" }
    ]
  }
]
```

---

### 3. 后端实现任务清单 ✅

- [x] 建表 SQL：`doc/sql/migration_phase8.1_dashboard_nav.sql`（**需在数据库中执行**）
- [x] Entity：`NavEnvironment`、`NavAddress`
- [x] Mapper：`NavEnvironmentMapper`、`NavAddressMapper`
- [x] Service：`DashboardNavService`（级联删除）
- [x] Controller：`DashboardNavController`

---

## Frontend

### 1. 路由与入口 ✅

- [x] `/dashboard` 已指向 `DashboardNav.vue` ✅
- 原报告视图可通过侧边栏「API Testing → Reports」「UI Testing → Reports」访问。

**路由示例**：

```js
{
  path: '/dashboard',
  name: 'Dashboard',
  component: () => import('../views/DashboardNav.vue')
}
```

---

### 2. 导航页布局 (DashboardNav.vue)

- [ ] 页面标题：如「导航」或「快捷入口」。
- [ ] 主体：按**层级**展示「环境 → 地址项」。
  - 环境：卡片、折叠面板或列表分组。
  - 地址项：每个项展示「短名称 + 标签」（如 A1 my、A2 th），可带图标、URL 预览。
- [ ] 操作：
  - 点击地址项：跳转至该 URL（新标签/当前页）或复制 URL。
  - 环境级：编辑、删除、新增地址。
  - 地址级：编辑、删除。
- [ ] 顶部或右侧：**新增环境**按钮；进入某环境后提供**新增地址**按钮。

---

### 3. 层级展示与交互

- [ ] **环境列表**：支持排序（按 `sort_order` 或名称）。
- [ ] **地址列表**：在每个环境下展示其地址项，支持排序。
- [ ] **添加环境**：弹窗或内联表单，字段：名称、描述、排序。
- [ ] **编辑环境**：同添加，预填数据。
- [ ] **删除环境**：二次确认；若有地址项，提示「将同时删除其下所有地址」或禁止删除直至清空。
- [ ] **添加地址项**：表单字段：短名称（A1）、标签（my）、URL、备注、排序。
- [ ] **编辑/删除地址项**：与添加一致，删除需确认。

---

### 4. 前端实现任务清单 ✅

- [x] `views/DashboardNav.vue`：折叠面板展示「环境 → 地址」层级 ✅
- [x] 调用 `GET /api/dashboard-nav/tree` 渲染 ✅
- [x] 环境 CRUD：新增/编辑/删除环境（删除前二次确认，级联删除地址）✅
- [x] 地址项 CRUD：新增/编辑/删除地址 ✅
- [x] 点击地址：有 URL 时新标签打开，无 URL 仅显示标签 ✅
- [x] API 模块：`api/dashboardNav.js` ✅

---

## 完成标准与验收

- [x] 用户可在 Dashboard 看到以「环境 → 地址项」为层级的导航结构。
- [x] 可添加、编辑、删除环境；可添加、编辑、删除地址项（A1/my、A2/th 等）。
- [x] 点击地址项（有 URL）在新标签打开；无 URL 仅显示标签。
- [x] 数据持久化在后端，刷新后保持。

**首次使用**：在数据库中执行 `doc/sql/migration_phase8.1_dashboard_nav.sql` 创建表。

---

## 附录：名词与字段对照

| 概念       | 示例   | 字段/说明                    |
| ---------- | ------ | ---------------------------- |
| 环境       | A、B   | name, description, sort_order |
| 地址短名称 | A1、A2 | short_name                   |
| 地址标签   | my、th | label                        |
| 地址 URL   | https://… | url                       |
| 备注       | 可选   | remark                       |

文档版本：1.0  
最后更新：2025-01-27
