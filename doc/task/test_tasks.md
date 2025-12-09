# 测试任务清单

## 后端测试任务

### 1. 单元测试

#### 1.1 Repository 层测试
- ❌ TestCaseRepository 测试
  - ❌ 测试 findAll 方法
  - ❌ 测试 findByModuleId 方法
  - ❌ 测试 save 方法
  - ❌ 测试 delete 方法

- ❌ TestModuleRepository 测试
  - ❌ 测试基本 CRUD 操作

#### 1.2 Service 层测试
- ❌ TestCaseService 测试
  - ❌ 测试用例保存
  - ❌ 测试用例查询
  - ❌ 测试用例删除
  - ❌ 测试执行引擎
  - ❌ 测试 Groovy 脚本执行
  - ❌ 测试断言逻辑
  - ❌ 测试变量提取

#### 1.3 Controller 层测试
- ❌ TestCaseController 测试
  - ❌ 测试 GET /api/cases
  - ❌ 测试 POST /api/cases
  - ❌ 测试 PUT /api/cases/{id}
  - ❌ 测试 DELETE /api/cases/{id}
  - ❌ 测试 POST /api/cases/execute

### 2. 集成测试

#### 2.1 API 集成测试
- ✅ 测试完整的用例创建流程
- ✅ 测试完整的用例执行流程
- ❌ 测试错误处理机制
- ❌ 测试并发请求

#### 2.2 数据库集成测试
- ❌ 测试数据持久化
- ❌ 测试事务回滚
- ❌ 测试级联操作

### 3. 端到端测试

- ❌ 创建测试用例 -> 执行 -> 查看结果
- ❌ 编辑测试用例 -> 重新执行
- ❌ 删除测试用例验证

## 前端测试任务

### 1. 组件测试

#### 1.1 页面组件测试
- ❌ TestCaseList.vue 测试
  - ❌ 测试用例列表渲染
  - ❌ 测试搜索功能
  - ❌ 测试新建对话框
  - ❌ 测试编辑功能
  - ❌ 测试删除功能

- ❌ TestExecution.vue 测试
  - ❌ 测试环境选择
  - ❌ 测试执行按钮
  - ❌ 测试结果展示

- ❌ TestReport.vue 测试
  - ❌ 测试统计卡片
  - ❌ 测试结果表格

#### 1.2 布局组件测试
- ❌ Layout.vue 测试
  - ❌ 测试侧边栏导航
  - ❌ 测试路由切换

### 2. API 集成测试

- ❌ 测试 API 请求拦截器
- ❌ 测试 API 响应拦截器
- ❌ 测试错误处理
- ❌ 测试代理配置

### 3. E2E 测试

- ❌ 用户完整操作流程测试
- ❌ 跨页面交互测试
- ❌ 浏览器兼容性测试

## 测试执行计划

### 阶段一：基础测试（当前阶段）
**目标**: 验证核心功能可用性

#### 后端测试
1. **API 接口测试**
   ```bash
   # 测试获取所有用例
   curl http://localhost:7777/api/cases
   
   # 测试创建用例
   curl -X POST http://localhost:7777/api/cases \
     -H "Content-Type: application/json" \
     -d '{
       "caseName": "Test Case 1",
       "method": "GET",
       "url": "http://httpbin.org/get",
       "assertionScript": "status_code == 200"
     }'
   
   # 测试执行
   curl -X POST "http://localhost:7777/api/cases/execute?envKey=dev"
   ```

2. **数据库验证**
   ```sql
   -- 验证数据插入
   SELECT * FROM test_case;
   SELECT * FROM test_module;
   ```

#### 前端测试
1. **页面访问测试**
   - 访问 http://localhost:8888
   - 验证页面正常加载
   - 验证路由切换

2. **功能测试**
   - 创建测试用例
   - 编辑测试用例
   - 删除测试用例
   - 执行测试
   - 查看结果

### 阶段二：自动化测试
**目标**: 建立自动化测试体系

- ❌ 编写单元测试用例
- ❌ 配置 CI/CD 流水线
- ❌ 集成测试覆盖率工具
- ❌ 建立测试报告机制

### 阶段三：性能测试
**目标**: 验证系统性能

- ❌ 并发测试（100 用户）
- ❌ 压力测试（1000 用例执行）
- ❌ 响应时间测试
- ❌ 资源占用测试

## 测试环境

### 开发环境
- 后端: http://localhost:7777
- 前端: http://localhost:8888
- 数据库: MySQL 10.48.0.13:3306

### 测试数据
```sql
-- 测试模块
INSERT INTO test_module (module_name) VALUES ('Auth Module');
INSERT INTO test_module (module_name) VALUES ('User Module');

-- 测试用例
INSERT INTO test_case (module_id, case_name, method, url, assertion_script) 
VALUES (1, 'Login Test', 'POST', 'http://httpbin.org/post', 'status_code == 200');
```

## 测试报告

### 测试结果记录
- 测试日期: ___________
- 测试人员: ___________
- 测试环境: ___________

### 测试通过标准
- ❌ 所有 API 接口正常响应
- ❌ 前端页面正常加载
- ❌ CRUD 操作功能正常
- ❌ 测试执行功能正常
- ❌ 无严重 Bug

### 已知问题
1. 
2. 
3. 

### 待修复问题
1. 
2. 
3.
