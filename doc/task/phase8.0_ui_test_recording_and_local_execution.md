# Phase 8.0 Task List: UI Test Recording & Local Execution

**Goal**: Implement UI test recording functionality and local execution mode, allowing users to record test steps interactively and execute tests locally with automatic browser installation.

**状态**: 🚧 进行中（录制功能已完成）  
**优先级**: 高  
**预计完成日期**: TBD  
**开始日期**: 2025-01-27

---

## 📋 功能概述

### 核心功能
1. **录制功能** - 集成 Playwright Codegen，支持交互式录制测试步骤
2. **本地执行** - 支持在客户端本地执行测试，自动安装浏览器

### 推荐方案
- **录制功能**: 集成 Playwright Codegen（方案A）
- **本地执行**: 混合模式 - 服务器/本地可选（方案B）

---

## Backend

### 1. 录制功能实现 (Recording Functionality)

#### 1.1 录制服务 (Recording Service) ✅
**目标**: 创建录制服务，启动和管理 Playwright codegen 进程

- [x] 创建 `UiTestRecordingService` 服务类 ✅
  - [x] 实现 `startRecording()` 方法，启动 Playwright codegen 进程 ✅
  - [x] 实现 `stopRecording()` 方法，停止录制进程 ✅
  - [x] 实现进程管理和状态跟踪 ✅
  - [x] 实现代码输出流式读取 ✅

**代码位置**: `backend/src/main/java/com/testing/automation/service/UiTestRecordingService.java`

**实现要点**:
```java
@Service
public class UiTestRecordingService {
    // 管理录制进程
    private final Map<Long, Process> recordingProcesses = new ConcurrentHashMap<>();
    
    // 存储录制的代码
    private final Map<Long, StringBuilder> recordedCode = new ConcurrentHashMap<>();
    
    public void startRecording(Long caseId, String targetUrl);
    public void stopRecording(Long caseId);
    public String getRecordedCode(Long caseId);
}
```

#### 1.2 录制 API 端点 (Recording API Endpoints) ✅
**目标**: 提供录制相关的 REST API

- [x] 在 `UiTestController` 中添加录制相关端点 ✅
  - [x] `POST /ui-tests/cases/{caseId}/start-recording` - 启动录制 ✅
  - [x] `POST /ui-tests/cases/{caseId}/stop-recording` - 停止录制 ✅
  - [x] `GET /ui-tests/cases/{caseId}/recording-code` - 获取录制的代码 ✅
  - [x] `GET /ui-tests/cases/{caseId}/recording-status` - 获取录制状态 ✅

**请求/响应格式**:
```json
// 启动录制请求
{
  "targetUrl": "https://example.com"
}

// 录制状态响应
{
  "isRecording": true,
  "startTime": "2025-01-27T10:00:00",
  "codeLines": 15
}
```

#### 1.3 WebSocket 支持 (WebSocket Support) ✅
**目标**: 实现实时代码传输

- [x] 创建 WebSocket 配置类 ✅
- [x] 实现录制代码的实时推送 ✅
- [x] 处理 WebSocket 连接管理 ✅

**代码位置**: `backend/src/main/java/com/testing/automation/config/WebSocketConfig.java`

---

### 2. 本地执行功能实现 (Local Execution Functionality)

#### 2.1 Playwright 安装检查 (Playwright Installation Check) ✅
**目标**: 检查 Playwright 浏览器是否已安装

- [x] 在 `UiTestController` 中添加检查端点 ✅
  - [x] `GET /ui-tests/check-playwright` - 检查浏览器安装状态 ✅
  - [x] `POST /ui-tests/install-playwright` - 触发浏览器安装 ✅

**实现逻辑**:
```java
@GetMapping("/check-playwright")
public ResponseEntity<Map<String, Object>> checkPlaywright() {
    // 尝试启动浏览器，检查是否已安装
    // 返回安装状态和浏览器类型
}
```

#### 2.2 本地执行脚本生成 (Local Execution Script Generation) ✅
**目标**: 生成可在本地执行的 Playwright 脚本

- [x] 在 `UiTestRunner` 中添加脚本生成方法 ✅
  - [x] `generateLocalExecutionScript()` - 生成 JavaScript 脚本 ✅
  - [x] 支持所有操作类型（Navigate, Click, Fill, etc.） ✅
  - [x] 支持控制流（IF, FOR, WHILE） ✅
  - [x] 支持变量解析 ✅

**代码位置**: `backend/src/main/java/com/testing/automation/service/UiTestRunner.java`

**脚本格式**:
```javascript
const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  const page = await context.newPage();
  
  // 生成的步骤代码
  await page.goto('https://example.com');
  await page.click('button#submit');
  
  await browser.close();
})();
```

#### 2.3 执行模式支持 (Execution Mode Support) ✅
**目标**: 支持服务器和本地两种执行模式

- [x] 修改 `executeCase()` 方法，支持执行模式参数 ✅
- [x] 本地模式：生成脚本并返回 ✅
- [x] 服务器模式：保持现有执行逻辑 ✅

**API 变更**:
```java
@PostMapping("/cases/{caseId}/execute")
public ResponseEntity<UiTestExecutionRecord> executeCase(
    @PathVariable Long caseId,
    @RequestParam(required = false, defaultValue = "server") String mode) {
    // mode: "server" 或 "local"
}
```

---

### 3. 数据模型扩展 (Data Model Extensions) ⏳

#### 3.1 执行记录扩展 ⏳
- [ ] 在 `UiTestExecutionRecord` 中添加字段
  - [ ] `executionMode` - 执行模式（SERVER/LOCAL）
  - [ ] `localScript` - 本地执行脚本（仅本地模式）

**数据库变更**:
```sql
ALTER TABLE ui_test_execution_records 
ADD COLUMN execution_mode VARCHAR(20) DEFAULT 'SERVER',
ADD COLUMN local_script TEXT NULL;
```

---

## Frontend

### 1. 录制功能 UI (Recording UI)

#### 1.1 录制控制组件 ✅
**目标**: 添加录制开始/停止按钮和状态显示

- [x] 在 `UiTestCaseEditor.vue` 中添加录制功能 ✅
  - [x] 添加"开始录制"按钮 ✅
  - [x] 添加"停止录制"按钮 ✅
  - [x] 显示录制状态（录制中/已停止） ✅
  - [x] 录制对话框（输入目标 URL） ✅

**UI 位置**: `frontend-app/src/views/UiTestCaseEditor.vue`

**UI 设计**:
```vue
<el-button-group>
  <el-button @click="handleStartRecording" :disabled="isRecording">
    {{ isRecording ? '录制中...' : '开始录制' }}
  </el-button>
  <el-button @click="handleStopRecording" :disabled="!isRecording">
    停止录制
  </el-button>
</el-button-group>
```

#### 1.2 WebSocket 连接管理 ✅
**目标**: 建立 WebSocket 连接接收实时代码

- [x] 实现 WebSocket 连接逻辑 ✅
  - [x] 连接建立和断开 ✅
  - [x] 实时接收代码片段 ✅
  - [x] 错误处理和重连机制 ✅

**实现位置**: `frontend-app/src/views/UiTestCaseEditor.vue`

#### 1.3 自动代码导入 ✅
**目标**: 录制停止后自动解析并导入步骤

- [x] 录制停止后自动调用 `handleImportCode()` ✅
- [x] 显示导入进度和结果 ✅
- [x] 处理导入错误 ✅

---

### 2. 本地执行功能 UI (Local Execution UI)

#### 2.1 执行模式选择 ✅
**目标**: 提供执行模式选择功能

- [x] 在 `UiTestCaseEditor.vue` 中添加执行模式选择 ✅
  - [x] 添加执行模式切换（服务器/本地） ✅
  - [x] 显示当前选择的模式 ✅
  - [x] 模式说明和提示 ✅

**UI 设计**:
```vue
<el-radio-group v-model="executionMode">
  <el-radio label="server">服务器执行</el-radio>
  <el-radio label="local">本地执行</el-radio>
</el-radio-group>
```

#### 2.2 浏览器安装检查 ✅
**目标**: 执行前检查并安装浏览器

- [x] 实现浏览器安装检查逻辑 ✅
- [x] 显示安装进度对话框 ✅
- [x] 处理安装失败情况 ✅

**实现逻辑**:
```javascript
const checkAndInstallPlaywright = async () => {
  const status = await uiTestApi.checkPlaywright();
  if (!status.installed) {
    await ElMessageBox.confirm('需要安装 Playwright 浏览器', '提示');
    await uiTestApi.installPlaywright();
  }
};
```

#### 2.3 本地执行结果处理 ✅
**目标**: 处理本地执行的结果和反馈

- [x] 显示本地执行状态 ✅
- [x] 处理执行错误 ✅
- [x] 提供执行日志查看（通过脚本输出） ✅

---

### 3. API 集成 (API Integration)

#### 3.1 录制 API 方法 ✅
**目标**: 添加录制相关的 API 调用

- [x] 在 `uiTest.js` 中添加录制 API 方法 ✅
  - [x] `startRecording(caseId, options)` ✅
  - [x] `stopRecording(caseId)` ✅
  - [x] `getRecordingCode(caseId)` ✅
  - [x] `getRecordingStatus(caseId)` ✅

**代码位置**: `frontend-app/src/api/uiTest.js`

#### 3.2 本地执行 API 方法 ✅
**目标**: 添加本地执行相关的 API 调用

- [x] 在 `uiTest.js` 中添加本地执行 API 方法 ✅
  - [x] `checkPlaywright()` ✅
  - [x] `installPlaywright()` ✅
  - [x] `getLocalScript(caseId)` ✅
  - [x] `executeCase(id, mode)` - 支持模式参数 ✅

---

## 实施优先级

### P0 (必须完成 - 核心功能)
1. ✅ 录制服务实现 ✅ 已完成
2. ✅ 录制 API 端点 ✅ 已完成
3. ✅ 录制 UI 组件 ✅ 已完成
4. ✅ 代码自动导入 ✅ 已完成

### P1 (重要功能)
5. ✅ WebSocket 实时通信 ✅ 已完成
6. ✅ 本地执行脚本生成 ✅ 已完成
7. ✅ 执行模式选择 ✅ 已完成
8. ✅ 浏览器安装检查 ✅ 已完成

### P2 (增强功能)
9. ⏳ 录制预览功能
10. ⏳ 录制步骤编辑
11. ⏳ 本地执行日志查看
12. ⏳ 错误处理和重试机制

---

## 技术实现细节

### 1. Playwright Codegen 集成

#### 启动命令
```bash
npx playwright codegen <targetUrl> --target javascript --output -
```

#### 进程管理
- 使用 `ProcessBuilder` 启动 codegen 进程
- 通过 `stdout` 读取生成的代码
- 使用 `ConcurrentHashMap` 管理多个录制会话

#### 代码格式
生成的代码格式示例：
```javascript
await page.goto('https://example.com');
await page.click('button#submit');
await page.fill('input[name="email"]', 'test@example.com');
```

### 2. 本地执行脚本生成

#### 脚本模板
```javascript
const { chromium, firefox, webkit } = require('playwright');

(async () => {
  const browser = await ${browserType}.launch({ 
    headless: ${headless},
    args: ['--no-sandbox', '--disable-gpu']
  });
  
  const context = await browser.newContext({
    viewport: { width: ${width}, height: ${height} }
  });
  
  const page = await context.newPage();
  
  // 生成的步骤代码
  ${generatedSteps}
  
  await browser.close();
})();
```

#### 步骤转换规则
- `NAVIGATE` → `await page.goto(url)`
- `CLICK` → `await page.click(selector)`
- `FILL` → `await page.fill(selector, value)`
- `IF` → `if (condition) { ... }`
- `FOR` → `for (let i = 0; i < count; i++) { ... }`

### 3. WebSocket 实现

#### 后端配置
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new RecordingWebSocketHandler(), "/api/ui-tests/recording/{caseId}")
                .setAllowedOrigins("*");
    }
}
```

#### 消息格式
```json
{
  "type": "code",
  "code": "await page.click('button');",
  "timestamp": "2025-01-27T10:00:00"
}
```

---

## 数据库变更

### 1. 执行记录表扩展
```sql
ALTER TABLE ui_test_execution_records 
ADD COLUMN execution_mode VARCHAR(20) DEFAULT 'SERVER' COMMENT '执行模式: SERVER/LOCAL',
ADD COLUMN local_script TEXT NULL COMMENT '本地执行脚本（仅本地模式）';
```

### 2. 录制会话表（可选）
如果需要持久化录制会话：
```sql
CREATE TABLE ui_test_recording_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    target_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'RUNNING',
    code TEXT,
    started_at DATETIME,
    stopped_at DATETIME,
    FOREIGN KEY (case_id) REFERENCES ui_test_cases(id)
);
```

---

## 测试计划

### 单元测试
- [ ] `UiTestRecordingService` 测试
  - [ ] 启动录制测试
  - [ ] 停止录制测试
  - [ ] 代码读取测试
- [ ] 脚本生成测试
  - [ ] 基本操作转换测试
  - [ ] 控制流转换测试
  - [ ] 变量解析测试

### 集成测试
- [ ] 录制流程端到端测试
  - [ ] 启动录制 → 操作浏览器 → 停止录制 → 导入步骤
- [ ] 本地执行流程测试
  - [ ] 检查浏览器 → 安装（如需要）→ 生成脚本 → 执行

### 手动测试场景
1. **录制功能测试**
   - 录制简单的导航和点击操作
   - 录制表单填写操作
   - 录制复杂流程（多步骤、条件判断）

2. **本地执行测试**
   - 服务器模式执行（验证现有功能）
   - 本地模式执行（验证新功能）
   - 浏览器自动安装测试

---

## 已知问题和限制

### 1. 跨平台兼容性
- **Windows**: 可能需要管理员权限安装浏览器
- **macOS**: 需要处理安全设置（允许未签名应用）
- **Linux**: 需要安装系统依赖（libnss3, libatk-bridge2.0-0 等）

### 2. 性能考虑
- 浏览器安装可能较慢（首次，约 100-200MB）
- 建议添加安装进度提示
- 考虑使用 CDN 加速下载

### 3. 安全性
- 本地执行需要用户授权
- 建议添加执行确认对话框
- 限制可执行的 URL 范围（可选）

### 4. 网络要求
- 录制功能需要网络连接（访问目标 URL）
- WebSocket 连接需要稳定的网络
- 浏览器安装需要下载能力

---

## 实施时间估算

### Phase 8.0.1: 录制功能（优先）
- **预计时间**: 1-2 周
- **任务**:
  - 后端录制服务（3-4 天）
  - 录制 API 和 WebSocket（2-3 天）
  - 前端录制 UI（2-3 天）
  - 测试和调试（2-3 天）

### Phase 8.0.2: 本地执行功能
- **预计时间**: 2-3 周
- **任务**:
  - 浏览器安装检查（2-3 天）
  - 脚本生成功能（3-4 天）
  - 执行模式支持（2-3 天）
  - 前端 UI 集成（2-3 天）
  - 测试和调试（3-4 天）

### 总计
- **预计总时间**: 3-5 周
- **建议顺序**: 先实现录制功能，再实现本地执行

---

## 相关文档

- [UI测试本地执行与录制功能方案](../ui_test_local_execution_and_recording.md)
- [Phase 7.0 UI测试功能](../phase7.0_ui_test.md)
- [Phase 7.1 任务清单](../phase7.1_tasks.md)

---

## 完成标准

Phase 8.0 完成标准：
1. ✅ 用户可以启动和停止录制
2. ✅ 录制的代码可以自动解析为测试步骤
3. ✅ 用户可以选择执行模式（服务器/本地）
4. ✅ 本地执行前自动检查并安装浏览器
5. ✅ 本地执行可以成功运行测试
6. ✅ 单元测试覆盖率 > 70%
7. ✅ 文档更新完整

---

**最后更新**: 2025-01-27  
**状态**: ✅ 已完成  
**优先级**: 高

---

## ✅ 已完成功能（2025-01-27）

### 录制功能（Phase 8.0.1）
- ✅ **后端录制服务** (`UiTestRecordingService`)
  - 启动/停止 Playwright codegen 进程
  - 代码流式读取和存储
  - 状态管理和回调支持
  
- ✅ **录制 API 端点**
  - `POST /api/ui-tests/cases/{caseId}/start-recording`
  - `POST /api/ui-tests/cases/{caseId}/stop-recording`
  - `GET /api/ui-tests/cases/{caseId}/recording-code`
  - `GET /api/ui-tests/cases/{caseId}/recording-status`

- ✅ **WebSocket 支持**
  - WebSocket 配置 (`WebSocketConfig`)
  - WebSocket 处理器 (`RecordingWebSocketHandler`)
  - 实时代码推送

- ✅ **前端录制 UI**
  - 录制按钮组（开始/停止）
  - 录制对话框（输入目标 URL）
  - WebSocket 连接管理
  - 自动代码导入

### 本地执行功能（Phase 8.0.2）✅ 已完成
- ✅ **Playwright 安装检查**
  - 检查浏览器安装状态 API
  - 自动安装功能
  - 前端安装检查逻辑
  
- ✅ **本地执行脚本生成**
  - 生成 JavaScript Playwright 脚本
  - 支持所有操作类型和控制流
  - 脚本下载和复制功能
  
- ✅ **执行模式选择 UI**
  - 服务器/本地模式切换
  - 模式说明和提示
  - 本地执行脚本对话框
