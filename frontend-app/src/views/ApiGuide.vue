<template>
  <div class="user-guide-container">
    <el-card class="guide-header">
      <h1>API 测试操作指南</h1>
      <p class="subtitle">全面了解系统架构、测试层级、请求配置及测试计划的使用方法</p>
    </el-card>

    <el-row :gutter="20">
      <!-- 1. 系统架构与层级关系 -->
      <el-col :span="24">
        <el-card class="guide-section architecture-card">
          <template #header>
            <div class="section-title">
              <el-icon color="#409EFF"><Connection /></el-icon>
              <span>系统架构与层级 (Design Structure)</span>
            </div>
          </template>
          <div class="content">
            <p>平台采用 <strong>Project > Module > Case</strong> 的三层管理模型，确保用例资产的有序组织：</p>
            <div class="hierarchy-diagram">
              <div class="h-node project">
                <el-icon><Folder /></el-icon> <strong>Project (项目)</strong>
                <p>顶级容器，通常对应一个微服务或产品线。管理全局变量和环境配置。</p>
              </div>
              <div class="h-arrow">↓</div>
              <div class="h-node module">
                <el-icon><Grid /></el-icon> <strong>Module (模块)</strong>
                <p>项目下的功能分组。例如：用户中心、订单管理、支付网关。</p>
              </div>
              <div class="h-arrow">↓</div>
              <div class="h-node case">
                <el-icon><Document /></el-icon> <strong>Test Case (测试用例)</strong>
                <p>具体的测试逻辑载体，可包含单个请求或多个步骤。</p>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 2. Test Case 与 Steps 的关系 -->
      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#67C23A"><List /></el-icon>
              <span>Test Case vs Steps (用例与步骤)</span>
            </div>
          </template>
          <div class="content">
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="sub-section">
                  <h4>单一请求 (Single Request)</h4>
                  <p>传统的单接口测试。用例本身即为一个完整的 HTTP 请求。</p>
                  <el-tag type="info" size="small">场景：接口冒烟测试、简单查询</el-tag>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="sub-section">
                  <h4>多步骤流程 (Multi-Step Flow)</h4>
                  <p>一个用例内包含多个步骤，步骤间可传递变量。</p>
                  <el-tag type="success" size="small">场景：登录 -> 下单 -> 支付 业务链</el-tag>
                </div>
              </el-col>
            </el-row>
            <div class="code-example" style="margin-top: 15px;">
              <pre>用例：用户购买流程
  Step 1: POST /login (提取 token)
  Step 2: POST /order (引用 ${token})
  Step 3: GET /order/status (引用 ${order_id})</pre>
            </div>
            <el-alert type="warning" :closable="false" style="margin-top: 10px;">
              注意：当用例包含 Steps 时，用例主表中的 URL/Method 将被忽略，系统会按顺序执行 Steps。
            </el-alert>
          </div>
        </el-card>
      </el-col>

      <!-- 3. Test Plan 的使用 -->
      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#E6A23C"><DataLine /></el-icon>
              <span>测试计划 (Test Plan)</span>
            </div>
          </template>
          <div class="content">
            <p><strong>测试计划</strong> 是用例的执行集合，用于回归测试或 CI/CD 集成。</p>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="隔离设计">
                每个计划通过 <strong>Overrides (覆盖)</strong> 机制实现数据隔离。
              </el-descriptions-item>
              <el-descriptions-item label="操作方法">
                1. 选择多个用例加入计划。<br>
                2. 点击用例旁的「编辑」图标进行计划内修改。<br>
                3. 支持针对<strong>步骤级别</strong>的覆盖（URL/Body/断言等）。
              </el-descriptions-item>
              <el-descriptions-item label="变量优先级">
                测试计划定义的变量 > 全局变量。
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <!-- 4. 变量提取 (Extractors) - 详细版 -->
      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#67C23A"><Download /></el-icon>
              <span>变量提取 (Extractors In-Depth)</span>
            </div>
          </template>
          <div class="content">
            <p>提取器用于将响应内容保存到变量 <code>${var}</code> 中，供后续步骤使用。执行时机：<strong>请求结束立即执行</strong>。</p>
            
            <el-tabs type="border-card">
              <el-tab-pane label="JSONPath">
                <p>适用于 JSON 响应格式。</p>
                <el-table :data="jsonPathExamples" size="small" border>
                  <el-table-column prop="desc" label="场景" width="180" />
                  <el-table-column prop="expr" label="表达式 (Expression)" />
                  <el-table-column prop="result" label="提取结果" />
                </el-table>
                <div class="tip-box">
                  提示：如果字段包含特殊字符（如中横线），请使用 <code>$['x-token']</code> 语法。
                </div>
              </el-tab-pane>
              
              <el-tab-pane label="REGEX (正则)">
                <p>利用正则表达式从整个响应文本中捕捉内容。</p>
                <div class="sub-section">
                  <strong>语法</strong>: 使用 <code>()</code> 定义捕获组。系统将保存第一个捕获组的内容。<br>
                  <strong>示例</strong>: 响应为 <code>"sid":"abc-123"</code><br>
                  <strong>表达式</strong>: <code>"sid":"([^"]+)"</code> -> 变量将获得 <code>abc-123</code>
                </div>
              </el-tab-pane>

              <el-tab-pane label="HEADER">
                <p>从 HTTP 响应头中获取值。</p>
                <div class="sub-section">
                  <strong>表达式</strong>: 填写 Header 的名称（不区分大小写）。<br>
                  <strong>示例</strong>: <code>Set-Cookie</code> 或 <code>X-Request-Id</code>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>
      </el-col>

      <!-- 5. 前置脚本 (Pre-request Script) -->
      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#E6A23C"><Operation /></el-icon>
              <span>前置脚本 (Pre-request Script)</span>
            </div>
          </template>
          <div class="content">
            <p>在请求发送**之前**执行，常用于动态生成数据、设置默认变量或处理加密逻辑。</p>
            <el-table :data="preScriptExamples" size="small" border>
              <el-table-column prop="scenario" label="应用场景" width="180" />
              <el-table-column prop="code" label="代码逻辑 (Groovy)" />
            </el-table>
            <div class="tip-box">
              提示：<code>vars</code> 是全局共享的变量池，在前置脚本中 <code>put</code> 的变量可在当前请求的 URL、Headers 或 Body 中通过 <code>${var}</code> 引用。
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 6. 断言校验 (Assertions) - 详细版 -->
      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#F56C6C"><CircleCheck /></el-icon>
              <span>断言校验 (Assertions In-Depth)</span>
            </div>
          </template>
          <div class="content">
            <p>断言决定了测试步骤的成败。支持多种内置函数和脚本逻辑。</p>
            
            <el-collapse accordion>
              <el-collapse-item title="1. 状态码与基础断言 (Basic)" name="1">
                <div class="code-example">
                  <pre>// 检查状态码
status_code == 200

// 检查响应文本是否包含关键字
response.body.contains("success")</pre>
                </div>
              </el-collapse-item>

              <el-collapse-item title="2. 结构化断言 (jsonPath / regex)" name="2">
                <p>在脚本中可以直接调用 <code>jsonPath(response, 'path')</code> 函数：</p>
                <div class="code-example">
                  <pre>// 检查响应 JSON 的特定字段
jsonPath(response, '$.code') == 0

// 检查列表长度
jsonPath(response, '$.data.items').size() > 0</pre>
                </div>
              </el-collapse-item>

              <el-collapse-item title="3. 变量交互与复杂脚本 (Scripting)" name="3">
                <p>您可以利用 <code>vars</code> 访问或更新运行时的变量池：</p>
                <div class="code-example">
                  <pre>// 获取之前提取的变量进行对比
vars.get("expected_id") == jsonPath(response, "$.id")

// 动态计算并保存新变量
int score = jsonPath(response, "$.score")
vars.put("is_high_score", score > 90)

// 必须返回 true 或不返回任何布尔值认为成功
return status_code == 200</pre>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
        </el-card>
      </el-col>
      <!-- 6. 常见问题与坑点 (Common Pitfalls) -->
      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#E6A23C"><InfoFilled /></el-icon>
              <span>常见问题与坑点 (FAQ & Pitfalls)</span>
            </div>
          </template>
          <div class="content">
            <el-collapse>
              <el-collapse-item title="Q: 提取器表达式里需要带 ${} 吗？" name="q1">
                <p><strong>不需要。</strong> 提取器的表达式（如 JSONPath）应该是原始路径，例如应该是 <code>$.token</code> 而不是 <code>$.${token}</code>。</p>
                <p><i style="color: #909399">注：系统现在会自动识别并移除误带的 ${}，但建议养成规范习惯。</i></p>
              </el-collapse-item>

              <el-collapse-item title="Q: 字段名包含横线（如 x-token）提取不到？" name="q2">
                <p>在 JSONPath 中，带有特殊字符或横线的 Key 需要使用<strong>括号语法</strong>：</p>
                <div class="code-example">
                  <pre>// 错误写法
$.x-token

// 正确写法
$['x-token']</pre>
                </div>
              </el-collapse-item>

              <el-collapse-item title="Q: 脚本中如何使用正则表达式？" name="q3">
                <p>我们在脚本环境中内置了 <code>regex(text, pattern)</code> 函数：</p>
                <div class="code-example">
                  <pre>// 从 Body 提取版本号
def version = regex(response.body, "version: ([0-9.]+)")
assert version == "1.0.1"</pre>
                </div>
              </el-collapse-item>
              
              <el-collapse-item title="Q: 手动编辑脚本后，UI 卡片会同步吗？" name="q4">
                <p><strong>会。</strong> 我们现在支持脚本与 UI 的**双向同步**。您在脚本中按照规范格式（如 <code>assert status_code == 200</code> 或 <code>vars.put(...)</code>）编写的代码会自动解析并显示在 UI 卡片中，反之亦然。</p>
                <p><i style="color: #909399">注意：如果编写了过于复杂的逻辑（如循环、条件分支），UI 界面可能无法完全呈现，但脚本仍会正常执行。</i></p>
              </el-collapse-item>
            </el-collapse>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { 
  Position, 
  CircleCheck, 
  Download, 
  Operation, 
  InfoFilled,
  Connection,
  Folder,
  Grid,
  Document,
  List,
  DataLine
} from '@element-plus/icons-vue'

const jsonPathExamples = [
  { desc: '获取根节点对象', expr: '$', result: '整个响应 JSON' },
  { desc: '获取简单字段', expr: '$.id', result: '10001' },
  { desc: '获取嵌套字段', expr: '$.data.user.name', result: '"Tom"' },
  { desc: '获取数组首个元素', expr: '$.items[0].id', result: '1' },
  { desc: '通配符获取所有', expr: '$.items[*].name', result: '["A", "B", ...]' },
  { desc: '特殊字符处理', expr: "$['x-token']", result: '"eyJ..."' }
]

const preScriptExamples = [
  { scenario: '设置默认变量', code: 'if(!vars.containsKey("id")) { vars.put("id", "123") }' },
  { scenario: '生成当前时间戳', code: 'vars.put("now", System.currentTimeMillis())' },
  { scenario: '生成随机 UUID', code: 'vars.put("uuid", UUID.randomUUID().toString())' }
]
</script>

<style scoped>
.user-guide-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.guide-header {
  margin-bottom: 20px;
  text-align: center;
}

.guide-header h1 {
  font-size: 32px;
  color: #303133;
  margin: 0 0 10px 0;
}

.subtitle {
  font-size: 16px;
  color: #909399;
  margin: 0;
}

.guide-section {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: bold;
}

.architecture-card .content {
  padding: 10px 0;
}

.hierarchy-diagram {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 15px;
}

.h-node {
  width: 100%;
  max-width: 600px;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  background: #fff;
}

.h-node p {
  margin: 5px 0 0 28px;
  font-size: 13px;
  color: #606266;
}

.h-arrow {
  font-size: 20px;
  color: #409EFF;
  margin: 5px 0;
}

.project { border-left: 5px solid #409EFF; }
.module { border-left: 5px solid #67C23A; }
.case { border-left: 5px solid #E6A23C; }

.sub-section h4 {
  margin-bottom: 10px;
  color: #303133;
}

.code-example {
  background-color: #2c3e50;
  color: #ecf0f1;
  padding: 15px;
  border-radius: 4px;
  margin: 15px 0;
  overflow-x: auto;
}

.code-example pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
}

.content code {
  background-color: #f4f4f5;
  padding: 2px 6px;
  border-radius: 3px;
  color: #e6a23c;
  font-family: 'Courier New', monospace;
}

.sub-section {
  padding: 10px;
  background: #f9f9f9;
  border-left: 3px solid #67C23A;
  margin: 10px 0;
  line-height: 1.6;
}

.tip-box {
  margin-top: 15px;
  font-size: 13px;
  color: #909399;
  font-style: italic;
}
</style>
