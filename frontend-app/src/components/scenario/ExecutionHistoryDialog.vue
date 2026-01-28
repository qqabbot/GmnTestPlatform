<template>
  <el-dialog 
    v-model="visible" 
    title="Execution History" 
    width="90%" 
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="history-container">
      <!-- History List -->
      <div class="history-list">
        <el-table 
          :data="historyRecords" 
          v-loading="loading"
          @row-click="handleRowClick"
          highlight-current-row
          height="100%"
        >
          <el-table-column label="Execution Time" width="180">
            <template #default="scope">
              {{ formatDateTime(scope.row.startedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="Environment" width="100" prop="envKey" />
          <el-table-column label="Status" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Steps" width="150">
            <template #default="scope">
              <span style="color: #67c23a;">{{ scope.row.passedSteps }}</span> / 
              <span style="color: #f56c6c;">{{ scope.row.failedSteps }}</span> / 
              <span>{{ scope.row.totalSteps }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Duration" width="120">
            <template #default="scope">
              {{ formatDuration(scope.row.durationMs) }}
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="100" fixed="right">
            <template #default="scope">
              <el-button 
                type="danger" 
                size="small" 
                link 
                @click.stop="deleteRecord(scope.row)"
              >
                Delete
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Step Logs Detail -->
      <div v-if="selectedRecord" class="logs-detail">
        <div class="detail-header">
          <h4>Step Execution Logs</h4>
          <div class="header-stats">
            <el-tag :type="getStatusType(selectedRecord.status)">
              {{ selectedRecord.status }}
            </el-tag>
            <span class="stats-text">
              Pass: {{ selectedRecord.passedSteps }} / 
              Fail: {{ selectedRecord.failedSteps }} / 
              Total: {{ selectedRecord.totalSteps }}
            </span>
          </div>
        </div>

        <el-table 
          :data="stepLogs" 
          v-loading="loadingLogs"
          height="100%"
          :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
          row-key="id"
          default-expand-all
        >
          <el-table-column label="Step Name" min-width="200" prop="stepName">
            <template #default="scope">
              <span :class="{ 'failed-step': scope.row.status === 'FAIL' }">
                {{ scope.row.stepName }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="Type" width="80" prop="stepType" />
          <el-table-column label="Status" width="80">
            <template #default="scope">
              <el-tag size="small" :type="getStatusType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Request" width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.requestMethod" size="small" :type="getMethodColor(scope.row.requestMethod)">
                {{ scope.row.requestMethod }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Response Code" width="120" prop="responseCode" />
          <el-table-column label="Duration" width="100">
            <template #default="scope">
              {{ scope.row.durationMs }}ms
            </template>
          </el-table-column>
          <el-table-column label="Details" width="100">
            <template #default="scope">
              <el-button 
                size="small" 
                link 
                @click="showLogDetail(scope.row)"
              >
                View
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- Log Detail Drawer -->
    <el-drawer 
      v-model="logDetailVisible" 
      title="Step Detail" 
      size="50%"
      append-to-body
      destroy-on-close
    >
      <request-response-detail :result="currentLog" />
    </el-drawer>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { testScenarioApi } from '../../api/testScenario'
import { ElMessage, ElMessageBox } from 'element-plus'
import RequestResponseDetail from '../testcase/RequestResponseDetail.vue'

const props = defineProps({
  modelValue: Boolean,
  scenarioId: Number
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const loading = ref(false)
const loadingLogs = ref(false)
const historyRecords = ref([])
const selectedRecord = ref(null)
const stepLogs = ref([])
const logDetailVisible = ref(false)
const currentLog = ref(null)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.scenarioId) {
    loadHistory()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const loadHistory = async () => {
  loading.value = true
  try {
    historyRecords.value = await testScenarioApi.getExecutionHistory(props.scenarioId, 20)
  } catch (e) {
    console.error(e)
    ElMessage.error('Failed to load execution history')
  } finally {
    loading.value = false
  }
}

const handleRowClick = async (row) => {
  selectedRecord.value = row
  loadingLogs.value = true
  try {
    stepLogs.value = await testScenarioApi.getExecutionLogs(row.id)
  } catch (e) {
    console.error(e)
    ElMessage.error('Failed to load step logs')
  } finally {
    loadingLogs.value = false
  }
}

const showLogDetail = (log) => {
  currentLog.value = log
  logDetailVisible.value = true
}

const deleteRecord = async (row) => {
  try {
    await ElMessageBox.confirm('Delete this execution record?', 'Warning', { type: 'warning' })
    await testScenarioApi.deleteExecutionRecord(row.id)
    ElMessage.success('Deleted')
    loadHistory()
    if (selectedRecord.value?.id === row.id) {
      selectedRecord.value = null
      stepLogs.value = []
    }
  } catch (e) {
    // cancel
  }
}

const handleClose = () => {
  selectedRecord.value = null
  stepLogs.value = []
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

const formatDuration = (ms) => {
  if (!ms) return '-'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(2)}s`
}

const getStatusType = (status) => {
  const map = { PASS: 'success', FAIL: 'danger', RUNNING: 'warning', PARTIAL: 'info' }
  return map[status] || 'info'
}

const getMethodColor = (method) => {
  const map = { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }
  return map[method] || 'info'
}

const formatJson = (str) => {
  if (!str) return ''
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch (e) {
    return str
  }
}
</script>

<style scoped>
.history-container {
  display: flex;
  gap: 20px;
  height: calc(100vh - 250px);
  min-height: 500px;
  overflow: hidden;
}
.history-list {
  flex: 4;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.logs-detail {
  flex: 6;
  min-width: 0;
  border-left: 1px solid #dcdfe6;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.detail-header h4 {
  margin: 0;
}
.header-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}
.stats-text {
  font-size: 12px;
  color: #909399;
}
.failed-step {
  color: #f56c6c;
  font-weight: 500;
}
.code-block {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
}
</style>
