<template>
  <div class="user-guide-container">
    <el-card class="guide-header">
      <h1>UI 测试操作指南</h1>
      <p class="subtitle">了解如何使用 IF 条件分支和 FOR 循环构建强大的UI测试流程</p>
    </el-card>

    <el-row :gutter="20">
      <!-- Navigation Actions -->
      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#409EFF"><Position /></el-icon>
              <span>导航类 (Navigation)</span>
            </div>
          </template>
          
          <div class="content">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="Navigate">
                <strong>功能</strong>: 打开指定 URL<br>
                <strong>Selector</strong>: [留空]<br>
                <strong>Value</strong>: https://example.com<br>
                <strong>示例</strong>: <code>Value → "https://www.baidu.com"</code>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <!-- Basic Interaction Actions -->
      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#67C23A"><Pointer /></el-icon>
              <span>基础交互 (Basic Interaction)</span>
            </div>
          </template>
          
          <div class="content">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="Click">
                <strong>功能</strong>: 点击元素<br>
                <strong>Selector</strong>: #submit-btn<br>
                <strong>Value</strong>: [留空]<br>
                <strong>示例</strong>: <code>Selector → "#login-button"</code>
              </el-descriptions-item>
              
              <el-descriptions-item label="Double Click">
                <strong>功能</strong>: 双击元素<br>
                <strong>Selector</strong>: .file-item<br>
                <strong>Value</strong>: [留空]<br>
                <strong>示例</strong>: <code>Selector → ".text-editor"</code> (打开编辑器)
              </el-descriptions-item>

              <el-descriptions-item label="Right Click">
                <strong>功能</strong>: 右键点击元素（触发上下文菜单）<br>
                <strong>Selector</strong>: .context-menu-trigger<br>
                <strong>Value</strong>: [留空]<br>
                <strong>示例</strong>: <code>Selector → ".file-icon"</code> (打开右键菜单)
              </el-descriptions-item>

              <el-descriptions-item label="Fill/Type">
                <strong>功能</strong>: 在输入框中输入文本<br>
                <strong>Selector</strong>: #username<br>
                <strong>Value</strong>: admin<br>
                <strong>示例</strong>: <code>Selector → "#email" | Value → "user@test.com"</code>
              </el-descriptions-item>

              <el-descriptions-item label="Hover">
                <strong>功能</strong>: 鼠标悬停在元素上（显示提示或子菜单）<br>
                <strong>Selector</strong>: .dropdown-trigger<br>
                <strong>Value</strong>: [留空]<br>
                <strong>示例</strong>: <code>Selector → ".menu-item"</code> (显示子菜单)
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <!-- Advanced Interaction -->
      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#E6A23C"><Mouse /></el-icon>
              <span>高级交互 (Advanced Interaction)</span>
            </div>
          </template>
          
          <div class="content">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="Press Key">
                <strong>功能</strong>: 在元素上按下键盘按键<br>
                <strong>Selector</strong>: #search-box<br>
                <strong>Value</strong>: Enter / Escape / Tab<br>
                <strong>示例</strong>: <code>Selector → "#search" | Value → "Enter"</code> (提交搜索)<br>
                <el-tag type="info" size="small" style="margin-top: 5px;">常用键: Enter, Escape, Tab, ArrowDown, Control+A</el-tag>
              </el-descriptions-item>

              <el-descriptions-item label="Select Option">
                <strong>功能</strong>: 在下拉列表中选择选项<br>
                <strong>Selector</strong>: #country-select<br>
                <strong>Value</strong>: China (选项文本或 value 属性)<br>
                <strong>示例</strong>: <code>Selector → "#province" | Value → "广东"</code>
              </el-descriptions-item>

              <el-descriptions-item label="Drag & Drop">
                <strong>功能</strong>: 拖拽元素到目标位置<br>
                <strong>Selector</strong>: .draggable-item (源元素)<br>
                <strong>Value</strong>: .drop-zone (目标元素)<br>
                <strong>示例</strong>: <code>Selector → "#file-1" | Value → "#upload-area"</code>
              </el-descriptions-item>

              <el-descriptions-item label="Scroll To">
                <strong>功能</strong>: 滚动页面使元素可见<br>
                <strong>Selector</strong>: #footer<br>
                <strong>Value</strong>: [留空]<br>
                <strong>示例</strong>: <code>Selector → "#bottom-section"</code> (滚动到底部)
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <!-- Wait Actions -->
      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#909399"><Timer /></el-icon>
              <span>等待类 (Wait)</span>
            </div>
          </template>
          
          <div class="content">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="Wait For Selector">
                <strong>功能</strong>: 等待元素出现在 DOM 中<br>
                <strong>Selector</strong>: #loading-complete<br>
                <strong>Value</strong>: [留空]<br>
                <strong>示例</strong>: <code>Selector → ".success-message"</code> (等待成功提示)<br>
                <el-tag type="warning" size="small" style="margin-top: 5px;">默认超时 30 秒</el-tag>
              </el-descriptions-item>

              <el-descriptions-item label="Wait Load State">
                <strong>功能</strong>: 等待页面加载完成（networkidle）<br>
                <strong>Selector</strong>: [留空]<br>
                <strong>Value</strong>: [留空]<br>
                <strong>示例</strong>: 在 Navigate 后使用，确保页面完全加载
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <!-- Assertion Actions -->
      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#F56C6C"><CircleCheck /></el-icon>
              <span>断言类 (Assertions)</span>
            </div>
          </template>
          
          <div class="content">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="Assert Visible">
                <strong>功能</strong>: 断言元素可见（失败则停止测试）<br>
                <strong>Selector</strong>: #success-msg<br>
                <strong>Value</strong>: [留空]<br>
                <strong>示例</strong>: <code>Selector → ".error-alert"</code> (验证错误提示已显示)
              </el-descriptions-item>

              <el-descriptions-item label="Assert Not Visible">
                <strong>功能</strong>: 断言元素不可见或不存在<br>
                <strong>Selector</strong>: #loading-spinner<br>
                <strong>Value</strong>: [留空]<br>
                <strong>示例</strong>: <code>Selector → ".loading"</code> (验证加载动画已消失)
              </el-descriptions-item>

              <el-descriptions-item label="Assert Text">
                <strong>功能</strong>: 断言元素包含指定文本<br>
                <strong>Selector</strong>: #status-label<br>
                <strong>Value</strong>: 成功<br>
                <strong>示例</strong>: <code>Selector → "h1" | Value → "欢迎"</code> (验证标题文本)
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <!-- Control Flow (IF/FOR) -->
      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#409EFF"><Operation /></el-icon>
              <span>控制流 (Control Flow) - IF 条件分支</span>
            </div>
          </template>
          
          <div class="content">
            <p><strong>功能</strong>: 仅在满足条件时执行子步骤</p>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="使用方法">
                1. 选择 <el-tag type="primary">IF Condition</el-tag><br>
                2. Selector/Target 列输入条件: <code>#selector</code> (可见) 或 <code>!#selector</code> (不可见)<br>
                3. 点击 <el-button type="success" size="small" circle><el-icon><Plus /></el-icon></el-button> 添加子步骤
              </el-descriptions-item>
            </el-descriptions>
            <div class="code-example" style="margin-top: 15px;">
              <pre>示例: 登录检测
IF Condition: "!#user-profile"
  └─ Click → #login-button
  └─ Fill → #username → "admin"
  └─ Fill → #password → "123456"
  └─ Click → #submit-btn</pre>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#67C23A"><Refresh /></el-icon>
              <span>控制流 (Control Flow) - FOR 循环</span>
            </div>
          </template>
          
          <div class="content">
            <p><strong>功能</strong>: 重复执行子步骤指定次数或遍历元素集合</p>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="使用方法">
                1. 选择 <el-tag type="success">FOR Loop</el-tag><br>
                2. Selector/Target 列输入: 固定次数 <code>3</code> 或 CSS选择器 <code>.item</code><br>
                3. 点击 <el-button type="success" size="small" circle><el-icon><Plus /></el-icon></el-button> 添加子步骤<br>
                4. 子步骤中可使用 <el-tag type="warning" effect="dark">${i}</el-tag> 变量 (从 0 开始)
              </el-descriptions-item>
            </el-descriptions>
            <div class="code-example" style="margin-top: 15px;">
              <pre>示例 1: 固定次数
FOR Loop: "5"
  └─ Click → #refresh-btn

示例 2: 批量创建用户
FOR Loop: "3"
  └─ Click → #add-user-btn
  └─ Fill → #username → user_$&#123;i&#125;
  └─ Fill → #email → user_$&#123;i&#125;@test.com
  └─ Click → #save-btn

示例 3: 遍历元素
FOR Loop: ".product-item"
  └─ Click → .product-item:nth-child($&#123;i&#125;) .add-to-cart</pre>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#E6A23C"><Connection /></el-icon>
              <span>嵌套使用</span>
            </div>
          </template>
          
          <div class="content">
            <p>IF 和 FOR 可以相互嵌套，创建复杂的测试逻辑：</p>
            <div class="code-example">
              <pre>FOR Loop: "10"
  └─ IF Condition: ".error-msg"
      └─ Click → #retry-btn
  └─ Click → #next-btn</pre>
            </div>
            <p><strong>说明</strong>: 循环 10 次，每次检查是否有错误消息，如果有则点击重试</p>
          </div>
        </el-card>
      </el-col>

      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#F56C6C"><Warning /></el-icon>
              <span>注意事项</span>
            </div>
          </template>
          
          <div class="content">
            <el-alert type="warning" :closable="false">
              <ul style="margin: 0; padding-left: 20px;">
                <li><strong>子步骤添加</strong>: 必须点击 IF/FOR 行的<strong>绿色 "+" 按钮</strong>，不是顶部的"Add Step"</li>
                <li><strong>变量作用域</strong>: <code>${i}</code> 只在 FOR Loop 的子步骤中有效</li>
                <li><strong>条件限制</strong>: 当前 IF 仅支持元素可见性检测，后续可扩展更多条件类型</li>
                <li><strong>嵌套深度</strong>: 理论上无限制，但建议不超过 3 层以保持可读性</li>
              </ul>
            </el-alert>
          </div>
        </el-card>
      </el-col>

      <el-col :span="24">
        <el-card class="guide-section">
          <template #header>
            <div class="section-title">
              <el-icon color="#909399"><Document /></el-icon>
              <span>快速参考</span>
            </div>
          </template>
          
          <div class="content">
            <el-table :data="referenceData" border style="width: 100%">
              <el-table-column prop="feature" label="功能" width="120" />
              <el-table-column prop="syntax" label="语法" width="200" />
              <el-table-column prop="description" label="说明" />
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Operation, Refresh, Plus, Position, Pointer, Mouse, Timer, CircleCheck, Warning, Document, Connection } from '@element-plus/icons-vue'

const referenceData = ref([
  {
    feature: 'IF 可见',
    syntax: '#selector',
    description: '元素存在且可见时执行子步骤'
  },
  {
    feature: 'IF 不可见',
    syntax: '!#selector',
    description: '元素不存在或不可见时执行子步骤'
  },
  {
    feature: 'FOR 固定',
    syntax: '5',
    description: '固定循环 5 次'
  },
  {
    feature: 'FOR 动态',
    syntax: '.item',
    description: '根据页面中 .item 元素的数量循环'
  },
  {
    feature: '循环变量',
    syntax: '${i}',
    description: '当前循环索引 (从 0 开始)，可用于选择器和值'
  }
])
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

.content h3 {
  color: #409EFF;
  margin-top: 20px;
  margin-bottom: 10px;
  font-size: 16px;
}

.content h3:first-child {
  margin-top: 0;
}

.content ol, .content ul {
  line-height: 1.8;
  color: #606266;
}

.content code {
  background-color: #f4f4f5;
  padding: 2px 6px;
  border-radius: 3px;
  color: #e6a23c;
  font-family: 'Courier New', monospace;
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
  white-space: pre;
}

.el-collapse {
  border: none;
}

:deep(.el-collapse-item__header) {
  background-color: #f5f7fa;
  padding-left: 15px;
  font-weight: bold;
  color: #409EFF;
}

:deep(.el-collapse-item__content) {
  padding: 15px;
  background-color: #ffffff;
}
</style>
