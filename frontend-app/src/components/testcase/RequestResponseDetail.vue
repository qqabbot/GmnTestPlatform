<template>
  <div class="request-response-detail">
    <el-descriptions title="Request Information" :column="2" border size="small">
      <el-descriptions-item label="URL" :span="2">
        <el-tag v-if="data?.method" size="small" :type="getMethodColor(data.method)" class="method-tag">
          {{ data.method }}
        </el-tag>
        <span class="url-text">{{ data?.requestUrl || 'N/A' }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="Status">
        <el-tag v-if="data?.status" :type="getStatusType(data.status)" size="small">
          {{ data.status }}
        </el-tag>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item label="Status Code">
        <span :class="getResponseCodeClass(data?.responseCode)">
          {{ data?.responseCode || '-' }}
        </span>
      </el-descriptions-item>
      <el-descriptions-item label="Duration">
        {{ data?.duration ? data.duration + 'ms' : '-' }}
      </el-descriptions-item>
    </el-descriptions>

    <div class="detail-sections">
      <div v-if="data?.requestHeaders" class="detail-section">
        <div class="section-header">Request Headers</div>
        <pre class="code-box">{{ formatJson(data.requestHeaders) }}</pre>
      </div>

      <div v-if="data?.requestBody" class="detail-section">
        <div class="section-header">Request Body</div>
        <pre class="code-box">{{ formatJson(data.requestBody) }}</pre>
      </div>

      <div v-if="data?.responseHeaders" class="detail-section">
        <div class="section-header">Response Headers</div>
        <pre class="code-box">{{ formatJson(data.responseHeaders) }}</pre>
      </div>

      <div v-if="data?.responseBody" class="detail-section">
        <div class="section-header">Response Body</div>
        <pre class="code-box">{{ formatJson(data.responseBody) }}</pre>
      </div>

      <div v-if="data?.detail" class="detail-section error">
        <div class="section-header">Execution Detail / Error</div>
        <div class="error-content">{{ data.detail }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  result: {
    type: Object,
    default: () => ({})
  }
})

// Normalize data for both live results and history logs
const data = computed(() => {
  const r = props.result || {}
  return {
    method: r.method || r.requestMethod,
    requestUrl: r.requestUrl,
    status: r.status,
    responseCode: r.responseCode,
    duration: r.duration || r.durationMs,
    requestHeaders: r.requestHeaders,
    requestBody: r.requestBody,
    responseHeaders: r.responseHeaders,
    responseBody: r.responseBody,
    detail: r.detail || r.errorMessage
  }
})

const getMethodColor = (method) => {
  const map = { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }
  return map[method] || 'info'
}

const getStatusType = (status) => {
  return status === 'PASS' || status === 'success' ? 'success' : 'danger'
}

const getResponseCodeClass = (code) => {
  if (!code) return ''
  if (code >= 200 && code < 300) return 'text-success'
  if (code >= 400) return 'text-danger'
  return 'text-warning'
}

const formatJson = (json) => {
  if (!json) return ''
  if (typeof json === 'object') {
    return JSON.stringify(json, null, 2)
  }
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch (e) {
    return json
  }
}
</script>

<style scoped>
.request-response-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.method-tag {
  margin-right: 8px;
}

.url-text {
  word-break: break-all;
  font-family: monospace;
  font-size: 13px;
}

.detail-sections {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.section-header {
  font-size: 12px;
  font-weight: bold;
  color: #909399;
  text-transform: uppercase;
}

.code-box {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 250px;
  overflow-y: auto;
  margin: 0;
  border: 1px solid #ebeef5;
  font-family: inherit;
  white-space: pre-wrap;
  word-break: break-all;
}

.error-content {
  background: #fef0f0;
  color: #f56c6c;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  border: 1px solid #fde2e2;
  white-space: pre-wrap;
}

.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
.text-warning { color: #e6a23c; }
</style>
