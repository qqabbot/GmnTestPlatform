<template>
  <div class="ui-test-report-detail" v-loading="loading">
    <div class="detail-header" v-if="record">
      <div class="header-left">
        <el-button @click="router.back()" link>
          <el-icon><ArrowLeft /></el-icon> Back
        </el-button>
        <span class="report-title">Execution Report #{{ record.id }} (Case ID: {{ record.caseId }})</span>
      </div>
      <div class="header-status">
        <el-tag :type="getStatusType(record.status)" size="large">{{ record.status }}</el-tag>
      </div>
    </div>

    <el-row :gutter="20" v-if="record" class="main-content">
      <!-- Summary Info -->
      <el-col :span="6">
        <el-card shadow="hover" header="Execution Summary">
          <div class="summary-item">
            <span class="label">Start Time:</span>
            <span class="value">{{ formatTime(record.executedAt) }}</span>
          </div>
          <div class="summary-item">
            <span class="label">Duration:</span>
            <span class="value">{{ record.duration }} ms</span>
          </div>
          <el-divider />
          <div v-if="record.errorMessage" class="error-section">
            <div class="label" style="color: #f56c6c;">Error Message:</div>
            <div class="error-msg">{{ record.errorMessage }}</div>
          </div>
          <div v-else class="success-msg">
            <el-icon color="#67c23a"><CircleCheckFilled /></el-icon>
            All steps completed
          </div>
        </el-card>

        <!-- Video Player -->
        <el-card shadow="hover" header="Execution Replay" style="margin-top: 20px;">
          <div v-if="record.videoPath" class="video-container">
            <video controls width="100%" :src="videoUrl">
              Your browser does not support the video tag.
            </video>
          </div>
          <div v-else class="no-video">
            <el-empty description="No video recording available" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <!-- Step Logs -->
      <el-col :span="18">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>Step-by-Step Execution Logs</span>
            </div>
          </template>
          
          <el-table :data="logs" style="width: 100%" stripe border>
            <el-table-column label="#" width="50" type="index" />
            <el-table-column prop="status" label="Status" width="100">
              <template #default="{ row }">
                <el-icon v-if="row.status === 'PASS'" color="#67c23a"><SuccessFilled /></el-icon>
                <el-icon v-else-if="row.status === 'FAIL'" color="#f56c6c"><CircleCloseFilled /></el-icon>
                <el-icon v-else color="#909399"><InfoFilled /></el-icon>
                <span style="margin-left: 5px;">{{ row.status }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="action" label="Action" width="130" />
            <el-table-column prop="selector" label="Element Selector" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <code v-if="row.selector" class="selector-code">{{ row.selector }}</code>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="screenshotPath" label="Screenshot" width="100">
              <template #default="{ row }">
                <el-image
                  v-if="row.screenshotPath"
                  style="width: 80px; height: 45px"
                  :src="getScreenshotUrl(row.screenshotPath)"
                  :preview-src-list="[getScreenshotUrl(row.screenshotPath)]"
                  fit="cover"
                  preview-teleported
                />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="errorDetail" label="Log / Error Detail" min-width="250">
                <template #default="{ row }">
                    <div class="log-detail">{{ row.errorDetail || 'Execution successful' }}</div>
                </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, SuccessFilled, CircleCloseFilled, CircleCheckFilled, InfoFilled } from '@element-plus/icons-vue'
import { uiTestApi } from '../api/uiTest'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const record = ref(null)
const logs = ref([])

// No longer need hardcoded localhost:7777, using relative paths via Vite proxy

const videoUrl = computed(() => {
  if (!record.value || !record.value.videoPath) return ''
  // videoPath is stored as something like "videos/record_123.webm"
  return `/${record.value.videoPath}`
})

const getScreenshotUrl = (path) => {
  if (!path) return ''
  return `/${path}`
}

const loadData = async () => {
  const id = route.params.id
  loading.value = true
  try {
    const res = await uiTestApi.getRecord(id)
    record.value = res
    
    const logsRes = await uiTestApi.getLogsByRecord(id)
    logs.value = logsRes
  } catch (error) {
    ElMessage.error('Failed to load report detail')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const map = {
    'PASS': 'success',
    'FAIL': 'danger',
    'RUNNING': 'primary',
    'ERROR': 'warning'
  }
  return map[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return 'N/A'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(loadData)
</script>

<style scoped>
.ui-test-report-detail {
  padding: 10px;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}
.report-title {
  font-size: 18px;
  font-weight: bold;
}
.summary-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
}
.label {
  color: #909399;
}
.value {
  font-weight: 500;
}
.error-msg {
  background: #fffafa;
  border: 1px solid #fde2e2;
  border-radius: 4px;
  padding: 10px;
  font-size: 12px;
  color: #f56c6c;
  margin-top: 5px;
  white-space: pre-wrap;
}
.video-container {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #000;
}
.log-detail {
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
}
.success-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #67c23a;
  font-weight: 500;
}
.selector-code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}
</style>
