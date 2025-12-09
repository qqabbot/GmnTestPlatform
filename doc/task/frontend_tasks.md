# 前端开发任务清单 - V3.1

## Phase 1 & 2 已完成 ✅

### 1. 项目初始化
- ✅ 创建 Vue 3 + Vite 项目
- ✅ 配置 Element Plus UI 框架
- ✅ 配置 Vue Router
- ✅ 配置 Pinia 状态管理
- ✅ 配置 Axios HTTP 客户端

### 2. 核心页面
- ✅ **Layout** - 主框架布局（侧边栏导航）
- ✅ **ProjectList** - 项目列表页
- ✅ **ModuleList** - 模块列表页（Phase 3.1 新增）
- ✅ **TestCaseList** - 测试用例列表页
- ✅ **EnvironmentList** - 环境列表页
- ✅ **TestExecution** - 测试执行页
- ✅ **TestReport** - 测试报告页

### 3. API 集成
- ✅ 封装 Request 工具（统一错误处理）
- ✅ 实现所有 API 服务模块：
  - ✅ projectApi
  - ✅ testModuleApi
  - ✅ testCaseApi
  - ✅ environmentApi
  - ✅ globalVariableApi (部分功能)
  - ✅ reportApi

---

## Phase 3 & 3.1 已完成 ✅

### 1. Test Case Editor 重构
- ✅ **Step-based UI**
  - ✅ 实现步骤树（左侧）+ 详情面板（右侧）
  - ✅ 支持拖拽排序（vuedraggable）
  - ✅ 支持复制、禁用步骤
  - ✅ 步骤列表组件 (StepList.vue)
  - ✅ 步骤详情组件 (StepDetail.vue)
- ✅ **Rich Editing**
  - ✅ 集成 Monaco Editor（JSON 编辑 + 自动格式化）
  - ✅ 步骤表单：URL、Method、Headers、Body
  - ✅ 实时变量替换预览（Dry Run）
  - ✅ Case Settings 标签页（基本信息、断言脚本）

### 2. 状态管理
- ✅ **Pinia Store**
  - ✅ testCaseStore - 管理当前编辑的测试用例状态
  - ✅ 操作：loadCase, saveCase, addStep, removeStep, updateStep
  - ✅ 操作：runDryRun, executeCase

### 3. Execution Page 升级
- ✅ **Interactive Execution**
  - ✅ 从编辑器运行单个用例（Run Case）
  - ✅ 项目/模块级批量执行
- ✅ **Real-time Feedback**
  - ✅ 使用 el-collapse 可折叠面板显示结果
  - ✅ 集成 Monaco Editor 展示 Request/Response 详情

### 4. Reporting UI
- ✅ **Dashboard**
  - ✅ 添加 "Dashboard" 和 "Execution History" 标签页
  - ✅ 集成 ECharts：
    - ✅ Pass/Fail 饼图
    - ✅ 执行趋势折线图
  - ✅ 统计卡片（Total, Passed, Failed, Pass Rate）
- ✅ **Details**
  - ✅ ReportDetail 组件（详情对话框）
  - ✅ Monaco Editor 显示日志（语法高亮）
  - ✅ 导出单个报告为 PDF（jsPDF + html2canvas）

### 5. Module Management (Phase 3.1 新增)
- ✅ **ModuleList.vue**
  - ✅ 完整 CRUD 操作
  - ✅ 按项目筛选模块
  - ✅ 搜索功能
  - ✅ 添加到导航菜单

### 6. Bug 修复与改进
- ✅ 修复编辑测试用例时的错误处理
- ✅ 为 Case Settings 添加 URL 和 Body 字段
- ✅ 改进必填字段验证和错误提示
- ✅ 优化加载状态和错误消息
- ✅ 修复路由和导航问题

---

## 依赖包

### UI & 编辑器
- Element Plus - UI 组件库
- Monaco Editor - 代码编辑器
- @guolao/vue-monaco-editor - Vue 3 Monaco wrapper
- vuedraggable - 拖拽排序

### 数据可视化
- ECharts - 图表库
- vue-echarts - Vue wrapper for ECharts

### PDF 导出
- jsPDF - PDF 生成
- html2canvas - 页面截图

### 其他
- Pinia - 状态管理
- Vue Router - 路由
- Axios - HTTP 客户端
- Sass - CSS 预处理器

---

## Phase 3.2 待实现功能 ⏭️

### 1. Environment & Variables UI Enhancement
- ⏭️ **Security UI**
  - ⏭️ 遮罩 secret 变量显示 (****)
  - ⏭️ 切换按钮显示/隐藏 secret 值
  - ⏭️ 一键复制 `${variable}` 语法
  - ⏭️ Secret vs Normal 变量的视觉区分
- ⏭️ **UX Improvements**
  - ⏭️ 变量分组（按环境/项目/模块）
  - ⏭️ 可折叠区域
  - ⏭️ 批量导入/导出变量（CSV/JSON）
  - ⏭️ 跨所有变量搜索和过滤

### 2. Execution Control
- ⏭️ 实现 Stop/Abort 执行按钮
- ⏭️ 实时执行状态更新
- ⏭️ 执行队列/历史视图

### 3. Additional UI Improvements
- ⏭️ **Test Case Editor**
  - ⏭️ 步骤模板库（常见 API 模式）
  - ⏭️ 保存前步骤验证
  - ⏭️ 复制测试用例功能
- ⏭️ **Execution Page**
  - ⏭️ 按日期/状态/项目筛选执行历史
  - ⏭️ 自定义用例选择的批量执行
- ⏭️ **General**
  - ⏭️ 键盘快捷键
  - ⏭️ 改进加载状态和错误消息
  - ⏭️ 用户偏好设置（主题、默认环境等）

### 4. Frontend Testing
- ⏭️ **Cypress E2E Tests**
  - ⏭️ Test Case Editor 工作流（创建/编辑/删除）
  - ⏭️ 步骤管理（添加/删除/重排序）
  - ⏭️ Dry Run 和执行功能
  - ⏭️ 变量管理
  - ⏭️ 报告查看和 PDF 导出
- ⏭️ **Visual Regression**
  - ⏭️ 设置视觉回归测试
  - ⏭️ 关键页面的基线截图
- ⏭️ **Accessibility**
  - ⏭️ 运行可访问性审计（axe-core）
  - ⏭️ 确保键盘导航有效
  - ⏭️ 添加适当的 ARIA 标签

### 5. Code Quality
- ⏭️ ESLint 配置和修复
- ⏭️ Prettier 代码格式化
- ⏭️ TypeScript 迁移（可选，更好的类型安全）

### 6. Performance Optimization
- ⏭️ 实现虚拟滚动（大列表）
- ⏭️ 添加请求缓存
- ⏭️ 优化打包体积
- ⏭️ 实现路由懒加载

---

## 技术栈总结

### 核心框架
- Vue 3 (Composition API)
- Vite (构建工具)
- Pinia (状态管理)
- Vue Router (路由)

### UI 框架
- Element Plus (组件库)
- Monaco Editor (代码编辑器)
- ECharts (数据可视化)

### 工具库
- Axios (HTTP 客户端)
- vuedraggable (拖拽排序)
- jsPDF + html2canvas (PDF 导出)
- Sass (CSS 预处理)

---

## 优先级

### P0（高优先级 - Phase 3.2）
- Environment & Variables UI 增强
- Cypress E2E 测试（关键流程）

### P1（中优先级 - 未来）
- Execution Control
- Code Quality 改进
- Performance 优化

### P2（低优先级 - 可选）
- Visual Regression Testing
- TypeScript 迁移
- 高级 UI 功能
