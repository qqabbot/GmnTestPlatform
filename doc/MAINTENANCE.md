# 项目文档维护规范

## 📋 文档结构

```
doc/
├── task/                    # 任务清单目录
│   ├── backend_tasks.md    # 后端开发任务
│   ├── frontend_tasks.md   # 前端开发任务
│   └── test_tasks.md       # 测试任务
├── api_endpoints.md         # API 接口文档
├── frontend_design.md       # 前端设计方案
├── frontend_guide.md        # 前端使用指南
├── frontend_prototype.md    # 前端原型设计
└── README.md               # 项目总览
```

## ✅ 任务状态标记规范

### 已完成任务
使用绿色打勾标记：`✅`

示例：
```markdown
- ✅ 创建 Spring Boot 项目
- ✅ 配置 Maven 依赖
```

### 未完成任务
使用红色叉号标记：`❌`

示例：
```markdown
- ❌ 实现 TestModuleController
- ❌ 添加模块 CRUD 接口
```

## 🔄 文档更新规则

### 1. 代码变更时
**必须同步更新以下文档**：

#### 后端代码变更
- ✅ 更新 `doc/task/backend_tasks.md` - 标记完成的任务
- ✅ 更新 `doc/api_endpoints.md` - 如果有新增或修改 API

#### 前端代码变更
- ✅ 更新 `doc/task/frontend_tasks.md` - 标记完成的任务

#### 测试相关
- ✅ 更新 `doc/task/test_tasks.md` - 标记完成的测试

### 2. API 接口变更
当后端 Controller 有以下变更时，必须更新 `api_endpoints.md`：
- 新增接口
- 修改接口路径
- 修改请求/响应格式
- 修改参数

### 3. 任务完成流程
1. 完成代码开发
2. 在对应的 task 文件中将 `❌` 改为 `✅`
3. 如果涉及 API 变更，更新 `api_endpoints.md`
4. 提交代码时在 commit message 中注明更新的文档

## 📝 文档更新示例

### 示例 1：完成后端功能
```markdown
# 代码变更
实现了 TestModuleController

# 需要更新的文档
1. doc/task/backend_tasks.md
   - ❌ 实现 TestModuleController  →  ✅ 实现 TestModuleController
   
2. doc/api_endpoints.md
   添加新接口：
   - GET /api/modules
   - POST /api/modules
   - PUT /api/modules/{id}
   - DELETE /api/modules/{id}
```

### 示例 2：完成前端功能
```markdown
# 代码变更
添加了 Monaco Editor 代码编辑器

# 需要更新的文档
1. doc/task/frontend_tasks.md
   - ❌ 集成 Monaco Editor  →  ✅ 集成 Monaco Editor
```

## 🎯 维护要点

1. **及时更新**：代码变更后立即更新文档
2. **准确标记**：使用正确的状态标记（✅/❌）
3. **详细记录**：API 文档要包含完整的请求/响应示例
4. **版本同步**：文档版本与代码版本保持一致

## 📌 快速检查清单

在提交代码前，检查：
- [ ] 是否更新了对应的 task 文件？
- [ ] 如果有 API 变更，是否更新了 api_endpoints.md？
- [ ] 状态标记是否正确（✅/❌）？
- [ ] 文档描述是否清晰准确？
