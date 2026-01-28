# UI测试本地执行与录制功能方案

## 问题分析

### 问题1：本地执行与浏览器安装
**需求**：是否可以在任意端点击执行时检查并本地安装playwright所需模块并在执行时在本地回放。

**当前实现**：
- ✅ 后端使用 Playwright Java API 执行测试
- ✅ 浏览器在后端服务器上启动（headless 或 headful）
- ✅ 前端通过 API 调用后端执行
- ❌ 不支持客户端本地执行

**可行性分析**：
- ✅ **技术上可行**，但需要架构调整
- ⚠️ **实现复杂度**：中等

### 问题2：本地录制功能
**需求**：是否可以在本地添加录制功能，录制后自动解析代码到操作步骤。

**当前实现**：
- ✅ 已有代码导入功能（`handleImportCode`），可以解析 Playwright 代码
- ❌ 没有录制功能
- ❌ 没有自动启动 Playwright codegen 的机制

**可行性分析**：
- ✅ **技术上可行**
- ⚠️ **实现复杂度**：中等

---

## 方案1：本地执行与浏览器安装

### 1.1 架构方案

#### 方案A：客户端本地执行（推荐）
**架构**：
```
前端浏览器 → 本地 Playwright 服务 → 本地浏览器执行
```

**实现方式**：
1. **前端集成 Playwright**：
   - 使用 Playwright 的 WebSocket 连接模式
   - 或使用 Electron 包装应用，集成 Playwright Node.js
   - 或使用 Playwright 的远程连接功能

2. **浏览器自动安装**：
   - 使用 `playwright install` 命令
   - 检查浏览器是否已安装
   - 未安装时自动下载

3. **执行流程**：
   ```
   用户点击执行 → 检查浏览器 → 未安装则安装 → 启动本地浏览器 → 执行测试 → 返回结果
   ```

**优点**：
- ✅ 用户可以看到浏览器操作过程（非 headless）
- ✅ 减少服务器负载
- ✅ 支持本地调试

**缺点**：
- ❌ 需要用户安装 Node.js 和 Playwright
- ❌ 跨平台兼容性问题
- ❌ 安全风险（本地执行代码）

#### 方案B：混合模式（推荐用于生产）
**架构**：
```
前端 → 选择执行模式 → 
  - 本地模式：本地执行
  - 服务器模式：服务器执行（当前方式）
```

**实现方式**：
1. 前端提供执行模式选择
2. 本地模式：使用 WebSocket 或 HTTP 连接到本地 Playwright 服务
3. 服务器模式：保持现有实现

**优点**：
- ✅ 灵活性高
- ✅ 向后兼容
- ✅ 用户可选择

**缺点**：
- ❌ 需要维护两套执行逻辑

#### 方案C：Playwright 远程连接（简单但有限制）
**架构**：
```
前端 → 后端启动 Playwright Server → WebSocket 连接 → 前端控制浏览器
```

**实现方式**：
1. 后端启动 Playwright 的远程服务器
2. 前端通过 WebSocket 连接控制浏览器
3. 浏览器在服务器上运行，但前端可以看到操作

**优点**：
- ✅ 实现相对简单
- ✅ 浏览器仍在服务器上，安全性好

**缺点**：
- ❌ 需要网络连接
- ❌ 延迟较高
- ❌ 不适合大规模并发

### 1.2 推荐实现方案：方案B（混合模式）

#### 技术栈
- **前端**：Vue 3 + Playwright（通过 Node.js 集成或 Electron）
- **后端**：保持现有 Spring Boot + Playwright Java
- **通信**：WebSocket 或 HTTP API

#### 实现步骤

##### 步骤1：前端添加本地执行支持

**1.1 创建本地 Playwright 服务检查器**

```typescript
// frontend-app/src/utils/playwrightLocal.ts
export class PlaywrightLocalService {
  private static instance: PlaywrightLocalService;
  private playwrightInstalled: boolean = false;
  private browserInstalled: boolean = false;

  async checkPlaywrightInstalled(): Promise<boolean> {
    try {
      // 通过 API 检查或本地检查
      const response = await fetch('/api/ui-tests/check-playwright');
      const data = await response.json();
      this.playwrightInstalled = data.installed;
      return this.playwrightInstalled;
    } catch (e) {
      return false;
    }
  }

  async installPlaywright(): Promise<void> {
    // 调用后端 API 触发安装
    const response = await fetch('/api/ui-tests/install-playwright', {
      method: 'POST'
    });
    if (!response.ok) {
      throw new Error('Failed to install Playwright');
    }
  }

  async executeLocally(caseId: number): Promise<any> {
    // 本地执行逻辑
    const response = await fetch(`/api/ui-tests/cases/${caseId}/execute-local`, {
      method: 'POST'
    });
    return response.json();
  }
}
```

**1.2 修改执行按钮逻辑**

```vue
<!-- frontend-app/src/views/UiTestCaseEditor.vue -->
<template>
  <el-button-group>
    <el-button @click="handleExecute('server')">服务器执行</el-button>
    <el-button @click="handleExecute('local')">本地执行</el-button>
  </el-button-group>
</template>

<script setup>
const handleExecute = async (mode: 'server' | 'local') => {
  if (mode === 'local') {
    // 检查并安装 Playwright
    const localService = new PlaywrightLocalService();
    const installed = await localService.checkPlaywrightInstalled();
    if (!installed) {
      ElMessageBox.confirm(
        '需要安装 Playwright 浏览器，是否继续？',
        '提示',
        { type: 'info' }
      ).then(async () => {
        ElMessage.loading('正在安装 Playwright...');
        await localService.installPlaywright();
        ElMessage.success('安装完成');
        await executeLocal();
      });
    } else {
      await executeLocal();
    }
  } else {
    // 现有服务器执行逻辑
    await handleExecuteServer();
  }
};
</script>
```

##### 步骤2：后端添加本地执行支持

**2.1 添加 Playwright 安装检查 API**

```java
// backend/src/main/java/com/testing/automation/controller/UiTestController.java

@GetMapping("/check-playwright")
public ResponseEntity<Map<String, Object>> checkPlaywright() {
    Map<String, Object> result = new HashMap<>();
    try {
        // 检查 Playwright 是否已安装
        Playwright playwright = Playwright.create();
        BrowserType chromium = playwright.chromium();
        
        // 尝试启动浏览器（会触发自动安装）
        boolean installed = true;
        try {
            Browser browser = chromium.launch(new BrowserType.LaunchOptions().setHeadless(true));
            browser.close();
        } catch (Exception e) {
            installed = false;
        }
        
        playwright.close();
        result.put("installed", installed);
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        result.put("installed", false);
        result.put("error", e.getMessage());
        return ResponseEntity.ok(result);
    }
}

@PostMapping("/install-playwright")
public ResponseEntity<Map<String, Object>> installPlaywright() {
    Map<String, Object> result = new HashMap<>();
    try {
        // Playwright Java 会自动下载浏览器
        Playwright playwright = Playwright.create();
        BrowserType chromium = playwright.chromium();
        Browser browser = chromium.launch();
        browser.close();
        playwright.close();
        
        result.put("success", true);
        result.put("message", "Playwright browsers installed successfully");
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        result.put("success", false);
        result.put("error", e.getMessage());
        return ResponseEntity.status(500).body(result);
    }
}
```

**2.2 添加本地执行模式支持**

```java
// backend/src/main/java/com/testing/automation/service/UiTestRunner.java

public UiTestExecutionRecord executeCase(Long caseId, boolean localMode) {
    if (localMode) {
        // 本地执行模式：返回执行脚本，由前端执行
        return prepareLocalExecution(caseId);
    } else {
        // 服务器执行模式：现有逻辑
        return executeCase(caseId);
    }
}

private UiTestExecutionRecord prepareLocalExecution(Long caseId) {
    UiTestCase uiCase = uiTestMapper.findCaseById(caseId);
    List<UiTestStep> steps = uiTestMapper.findStepsByCaseId(caseId);
    
    // 生成可执行的 JavaScript 代码
    String script = generatePlaywrightScript(uiCase, steps);
    
    UiTestExecutionRecord record = new UiTestExecutionRecord();
    record.setCaseId(caseId);
    record.setStatus("PENDING_LOCAL");
    record.setLocalScript(script); // 存储脚本供前端执行
    uiTestMapper.insertRecord(record);
    
    return record;
}

private String generatePlaywrightScript(UiTestCase uiCase, List<UiTestStep> steps) {
    StringBuilder script = new StringBuilder();
    script.append("const { chromium } = require('playwright');\n");
    script.append("(async () => {\n");
    script.append("  const browser = await chromium.launch({ headless: false });\n");
    script.append("  const context = await browser.newContext();\n");
    script.append("  const page = await context.newPage();\n");
    
    for (UiTestStep step : steps) {
        script.append(generateStepCode(step));
    }
    
    script.append("  await browser.close();\n");
    script.append("})();\n");
    return script.toString();
}
```

---

## 方案2：本地录制功能

### 2.1 架构设计

#### 方案A：集成 Playwright Codegen（推荐）
**流程**：
```
用户点击录制 → 启动 Playwright codegen → 用户在浏览器中操作 → 
自动生成代码 → 解析代码 → 转换为测试步骤 → 保存到编辑器
```

**实现方式**：
1. **后端启动 Codegen 服务**：
   - 使用 Playwright 的 `codegen` 命令
   - 通过 WebSocket 或 HTTP 流式返回生成的代码

2. **前端接收并解析**：
   - 实时接收生成的代码
   - 使用现有的 `handleImportCode` 逻辑解析
   - 自动添加到步骤列表

#### 方案B：浏览器扩展录制
**流程**：
```
用户安装浏览器扩展 → 在网页中操作 → 扩展记录操作 → 
发送到平台 → 解析为测试步骤
```

**优点**：
- ✅ 用户体验好
- ✅ 不需要启动额外服务

**缺点**：
- ❌ 需要开发浏览器扩展
- ❌ 跨浏览器兼容性问题

### 2.2 推荐实现方案：方案A（Playwright Codegen）

#### 实现步骤

##### 步骤1：后端添加录制服务

**1.1 创建录制服务**

```java
// backend/src/main/java/com/testing/automation/service/UiTestRecordingService.java

@Service
@Slf4j
public class UiTestRecordingService {
    
    private final Map<Long, Process> recordingProcesses = new ConcurrentHashMap<>();
    
    public void startRecording(Long caseId, String targetUrl) {
        try {
            // 启动 Playwright codegen
            ProcessBuilder pb = new ProcessBuilder(
                "npx", "playwright", "codegen",
                targetUrl,
                "--target", "javascript",
                "--output", "-" // 输出到 stdout
            );
            
            Process process = pb.start();
            recordingProcesses.put(caseId, process);
            
            // 启动线程读取输出
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 通过 WebSocket 或 SSE 发送到前端
                        sendCodeToFrontend(caseId, line);
                    }
                } catch (IOException e) {
                    log.error("Error reading codegen output", e);
                }
            }).start();
            
        } catch (IOException e) {
            log.error("Failed to start recording", e);
            throw new RuntimeException("Failed to start recording", e);
        }
    }
    
    public void stopRecording(Long caseId) {
        Process process = recordingProcesses.remove(caseId);
        if (process != null) {
            process.destroy();
        }
    }
    
    private void sendCodeToFrontend(Long caseId, String code) {
        // 通过 WebSocket 或 SSE 发送
        // 实现细节...
    }
}
```

**1.2 添加录制 API**

```java
// backend/src/main/java/com/testing/automation/controller/UiTestController.java

@PostMapping("/cases/{caseId}/start-recording")
public ResponseEntity<Map<String, Object>> startRecording(
    @PathVariable Long caseId,
    @RequestBody Map<String, String> request) {
    
    String targetUrl = request.get("targetUrl");
    recordingService.startRecording(caseId, targetUrl);
    
    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("message", "Recording started");
    return ResponseEntity.ok(result);
}

@PostMapping("/cases/{caseId}/stop-recording")
public ResponseEntity<Map<String, Object>> stopRecording(@PathVariable Long caseId) {
    recordingService.stopRecording(caseId);
    
    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("message", "Recording stopped");
    return ResponseEntity.ok(result);
}

@GetMapping("/cases/{caseId}/recording-code")
public ResponseEntity<String> getRecordingCode(@PathVariable Long caseId) {
    // 获取已录制的代码
    String code = recordingService.getRecordedCode(caseId);
    return ResponseEntity.ok(code);
}
```

##### 步骤2：前端添加录制功能

**2.1 添加录制按钮和逻辑**

```vue
<!-- frontend-app/src/views/UiTestCaseEditor.vue -->
<template>
  <el-button-group>
    <el-button @click="handleStartRecording" :disabled="isRecording">
      {{ isRecording ? '录制中...' : '开始录制' }}
    </el-button>
    <el-button @click="handleStopRecording" :disabled="!isRecording">
      停止录制
    </el-button>
    <el-button @click="handleExecute">执行</el-button>
  </el-button-group>
  
  <!-- 录制对话框 -->
  <el-dialog v-model="showRecordingDialog" title="录制测试步骤">
    <el-input v-model="recordingUrl" placeholder="输入要录制的URL" />
    <template #footer>
      <el-button @click="showRecordingDialog = false">取消</el-button>
      <el-button type="primary" @click="confirmStartRecording">开始录制</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';

const isRecording = ref(false);
const showRecordingDialog = ref(false);
const recordingUrl = ref('');
let recordingWebSocket = null;

const handleStartRecording = () => {
  showRecordingDialog.value = true;
};

const confirmStartRecording = async () => {
  if (!recordingUrl.value) {
    ElMessage.warning('请输入URL');
    return;
  }
  
  try {
    // 启动录制
    await uiTestApi.startRecording(uiCase.value.id, {
      targetUrl: recordingUrl.value
    });
    
    // 建立 WebSocket 连接接收代码
    connectRecordingWebSocket();
    
    isRecording.value = true;
    showRecordingDialog.value = false;
    ElMessage.success('录制已开始，请在浏览器中操作');
  } catch (e) {
    ElMessage.error('启动录制失败：' + e.message);
  }
};

const handleStopRecording = async () => {
  try {
    await uiTestApi.stopRecording(uiCase.value.id);
    
    // 获取完整代码并解析
    const code = await uiTestApi.getRecordingCode(uiCase.value.id);
    if (code) {
      importCode.value = code;
      handleImportCode(); // 使用现有的导入逻辑
    }
    
    isRecording.value = false;
    if (recordingWebSocket) {
      recordingWebSocket.close();
      recordingWebSocket = null;
    }
    
    ElMessage.success('录制已停止，步骤已导入');
  } catch (e) {
    ElMessage.error('停止录制失败：' + e.message);
  }
};

const connectRecordingWebSocket = () => {
  // 建立 WebSocket 连接
  const ws = new WebSocket(`ws://localhost:7777/api/ui-tests/recording/${uiCase.value.id}`);
  
  ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    if (data.type === 'code') {
      // 实时接收代码片段
      importCode.value += data.code + '\n';
    }
  };
  
  ws.onerror = (error) => {
    console.error('Recording WebSocket error:', error);
  };
  
  recordingWebSocket = ws;
};
</script>
```

**2.2 添加 API 方法**

```javascript
// frontend-app/src/api/uiTest.js

export const uiTestApi = {
  // ... 现有方法 ...
  
  startRecording(caseId, options) {
    return request.post(`/ui-tests/cases/${caseId}/start-recording`, options);
  },
  
  stopRecording(caseId) {
    return request.post(`/ui-tests/cases/${caseId}/stop-recording`);
  },
  
  getRecordingCode(caseId) {
    return request.get(`/ui-tests/cases/${caseId}/recording-code`);
  }
};
```

---

## 实现优先级建议

### Phase 1：本地执行基础功能（P0）
1. ✅ 添加执行模式选择（服务器/本地）
2. ✅ 实现 Playwright 安装检查
3. ✅ 实现自动安装功能
4. ✅ 实现本地执行脚本生成

### Phase 2：录制功能（P1）
1. ✅ 集成 Playwright codegen
2. ✅ 实现录制服务
3. ✅ 实现代码实时接收
4. ✅ 实现自动解析和导入

### Phase 3：优化和增强（P2）
1. ⏳ WebSocket 实时通信优化
2. ⏳ 录制预览功能
3. ⏳ 录制步骤编辑
4. ⏳ 错误处理和重试机制

---

## 技术注意事项

### 1. 浏览器安装
- Playwright Java 会自动下载浏览器到 `~/.cache/ms-playwright/`
- 首次执行时会自动触发下载
- 可以通过 `playwright install` 命令手动安装

### 2. 安全性
- 本地执行需要用户授权
- 建议添加执行确认对话框
- 限制可执行的 URL 范围

### 3. 跨平台兼容性
- Windows: 需要管理员权限安装浏览器
- macOS: 需要处理安全设置
- Linux: 需要安装系统依赖

### 4. 性能考虑
- 浏览器安装可能较慢（首次）
- 建议添加安装进度提示
- 考虑使用 CDN 加速下载

---

## 总结

### 问题1：本地执行 ✅ 可行
- **推荐方案**：混合模式（服务器/本地可选）
- **实现难度**：中等
- **预计工作量**：2-3 周

### 问题2：录制功能 ✅ 可行
- **推荐方案**：集成 Playwright codegen
- **实现难度**：中等
- **预计工作量**：1-2 周

### 建议实施顺序
1. 先实现录制功能（用户需求更明确）
2. 再实现本地执行（需要更多架构考虑）

---

**最后更新**: 2025-01-27  
**状态**: 📋 方案设计完成，待实施
