# 自动化测试平台 - 前端原型设计

## 设计概览

本文档展示了自动化测试平台前端的完整原型设计，包括主要页面的 UI 设计和交互说明。

## 核心页面原型

### 1. 测试用例列表页

![测试用例列表页](/Users/xhb/.gemini/antigravity/brain/e77f905b-e4b3-4454-ac29-7438aa607732/test_case_list_page_1764585364522.png)

**功能说明**：
- 展示所有测试用例的列表视图
- 支持按模块筛选和关键词搜索
- 快速操作：编辑、删除、执行单个用例
- 批量操作：批量删除、批量执行

**交互要点**：
- 点击"New Case"按钮打开用例编辑器
- 点击表格行可查看用例详情
- 状态开关可快速启用/禁用用例

---

### 2. 测试用例编辑器

![测试用例编辑器](/Users/xhb/.gemini/antigravity/brain/e77f905b-e4b3-4454-ac29-7438aa607732/test_case_editor_1764585394257.png)

**功能说明**：
- 完整的用例配置表单
- 代码编辑器支持语法高亮和自动补全
- 实时表单验证

**表单字段**：
1. **基本信息**
   - 用例名称（必填）
   - 所属模块（下拉选择）
   - HTTP 方法（单选）
   - 请求 URL（必填，支持变量占位符）

2. **请求配置**
   - 请求体（JSON 格式，Monaco 编辑器）
   - 前置条件（依赖的用例 ID，逗号分隔）

3. **脚本配置**
   - 变量提取脚本（Groovy，用于从响应中提取变量）
   - 断言脚本（Groovy，必填，用于验证响应）

**交互要点**：
- "Test Run"按钮可快速测试当前配置
- 保存前进行表单验证
- 支持快捷键保存（Ctrl+S）

---

### 3. 测试执行页

![测试执行页](/Users/xhb/.gemini/antigravity/brain/e77f905b-e4b3-4454-ac29-7438aa607732/test_execution_page_1764585419028.png)

**功能说明**：
- 配置并执行测试用例
- 实时显示执行进度和日志
- 统计数据实时更新

**执行流程**：
1. 选择环境（dev/staging/prod）
2. 可选：选择特定模块
3. 点击"Start Execution"开始执行
4. 实时查看执行进度和日志
5. 执行完成后自动跳转到报告页

**交互要点**：
- 执行过程中显示进度条和当前执行的用例
- 实时日志采用 WebSocket 推送
- 可以中途停止执行

---

### 4. 测试报告页

![测试报告页](/Users/xhb/.gemini/antigravity/brain/e77f905b-e4b3-4454-ac29-7438aa607732/test_report_page_1764585444758.png)

**功能说明**：
- 可视化展示测试结果
- 详细的用例执行信息
- 支持导出报告

**报告内容**：
1. **概览卡片**
   - 通过率（百分比）
   - 总用例数
   - 执行时长
   - 最后执行时间

2. **图表分析**
   - 饼图：测试结果分布（通过/失败/跳过）
   - 柱状图：各模块通过率对比

3. **详细结果**
   - 表格展示每个用例的执行结果
   - 失败用例高亮显示
   - 可展开查看错误详情和堆栈信息

**交互要点**：
- 点击图表可筛选对应状态的用例
- 点击用例行可展开详细信息
- 导出按钮支持 HTML/PDF 格式

---

## 技术实现建议

### 推荐技术栈
- **Vue 3** + Composition API
- **Element Plus** - UI 组件库
- **Vue Router** - 路由管理
- **Pinia** - 状态管理
- **Axios** - HTTP 客户端
- **Monaco Editor** - 代码编辑器
- **ECharts** - 图表库

### 项目结构
```
frontend/
├── src/
│   ├── assets/          # 静态资源
│   ├── components/      # 公共组件
│   ├── views/           # 页面组件
│   │   ├── TestCaseList.vue
│   │   ├── TestCaseEditor.vue
│   │   ├── TestExecution.vue
│   │   └── TestReport.vue
│   ├── router/          # 路由配置
│   ├── stores/          # Pinia stores
│   ├── api/             # API 接口
│   └── utils/           # 工具函数
├── package.json
└── vite.config.js
```

### API 集成
后端 API 基础路径：`http://localhost:7777/api`

主要接口：
- `GET /api/cases` - 获取用例列表
- `POST /api/cases` - 创建用例
- `PUT /api/cases/{id}` - 更新用例
- `DELETE /api/cases/{id}` - 删除用例
- `POST /api/cases/execute` - 执行测试

---

## 下一步行动

1. **初始化 Vue 项目**
   ```bash
   npm create vue@latest
   ```

2. **安装依赖**
   ```bash
   npm install element-plus axios pinia vue-router
   npm install monaco-editor echarts
   ```

3. **实现核心页面**
   - 从测试用例列表页开始
   - 逐步实现编辑器、执行页、报告页

4. **集成后端 API**
   - 配置 Axios 实例
   - 实现 API 服务层
   - 处理跨域问题（CORS）

5. **优化和测试**
   - 响应式适配
   - 性能优化
   - 用户体验优化
