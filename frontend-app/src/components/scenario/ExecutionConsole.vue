<template>
  <div v-if="visibleLocal" class="execution-console" :style="consoleStyle">
    <div class="console-header">
      <div class="header-left">
        <span class="title">{{ title }}</span>
        <el-tag v-if="status" :type="getStatusType(status)" size="small">{{ status }}</el-tag>
        <span class="log-count">({{ filteredLogs.length }} logs)</span>
      </div>
      <div class="header-actions">
        <el-input
          v-model="searchText"
          size="small"
          placeholder="Search logs..."
          clearable
          style="width: 200px; margin-right: 8px;"
          @input="handleSearch"
        />
        <el-select v-model="filterLevel" size="small" style="width: 100px; margin-right: 8px;" clearable>
          <el-option label="All" value="" />
          <el-option label="Info" value="info" />
          <el-option label="Success" value="success" />
          <el-option label="Error" value="error" />
          <el-option label="Fail" value="fail" />
        </el-select>
        <el-button size="small" @click="exportLogs">Export</el-button>
        <el-button size="small" circle :icon="Close" @click="close" />
      </div>
    </div>
    <div class="console-body" ref="consoleBody">
      <div v-for="(log, index) in filteredLogs" :key="index" class="log-line" :class="log.type">
        <span class="log-time">[{{ formatTime(log.timestamp) }}]</span>
        <span class="log-tag">[{{ log.type.toUpperCase() }}]</span>
        <span class="log-message">{{ log.message }}</span>
      </div>
      <div v-if="isRunning && filteredLogs.length === logs.length" class="log-line running">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>Executing steps...</span>
      </div>
      <div v-if="filteredLogs.length === 0 && logs.length > 0" class="log-line info">
        <span>No logs match the current filter</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted, nextTick, watch, computed } from 'vue'
import { Close, Loading } from '@element-plus/icons-vue'

const props = defineProps({
  scenarioId: [String, Number],
  envKey: {
    type: String,
    default: 'dev'
  },
  title: {
    type: String,
    default: 'Execution Console'
  },
  streamUrl: String,
  fixed: {
    type: Boolean,
    default: false
  },
  offsetLeft: {
    type: String,
    default: '0px'
  },
  visible: {
    type: Boolean,
    default: false
  }
})

const visibleLocal = ref(false)
const logs = ref([])
const status = ref('')
const isRunning = ref(false)
const consoleBody = ref(null)
const searchText = ref('')
const filterLevel = ref('')
let eventSource = null

const consoleStyle = computed(() => ({
  position: props.fixed ? 'fixed' : 'absolute',
  left: props.offsetLeft || '0',
  bottom: '0',
  right: '0',
  zIndex: props.fixed ? '3000' : '2500'
}))

const emit = defineEmits(['step-update', 'variables-update', 'update:visible'])

const start = () => {
  visibleLocal.value = true
  emit('update:visible', true)
  logs.value = []
  status.value = 'RUNNING'
  isRunning.value = true
  
  // Reset all step statuses in parent if needed (handled by parent listening to start)
  emit('step-update', { type: 'reset' })
  
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
  const url = props.streamUrl || `${baseUrl}/scenarios/${props.scenarioId}/execute/stream?envKey=${props.envKey}`
  
  if (eventSource) {
    eventSource.close()
  }
  
  console.log('Starting EventSource with URL:', url)
  eventSource = new EventSource(url)
  
  const onMessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      handleEvent(data)
    } catch (e) {
      console.error('Failed to parse SSE data', e)
    }
  }
  
  const onError = (err) => {
    // If we've already terminated the source, ignore
    if (!eventSource) return
    
    // If not running, it means we probably just closed it ourselves
    if (!isRunning.value) {
      cleanUp()
      return
    }
    
    const errorMsg = eventSource.readyState === EventSource.CLOSED 
      ? 'Connection closed (Check Backend logs)' 
      : 'Connection error (SSE failed)'
    addLog('error', `${errorMsg} [State: ${eventSource.readyState}]`)
    cleanUp()
  }

  const cleanUp = () => {
    if (eventSource) {
        eventSource.removeEventListener('message', onMessage)
        eventSource.removeEventListener('error', onError)
        eventSource.close()
        eventSource = null
    }
    isRunning.value = false
  }

  eventSource.addEventListener('message', onMessage)
  eventSource.addEventListener('error', onError)
}

const handleEvent = (event) => {
  if (event.variables) {
    emit('variables-update', event.variables)
  }
  
  switch (event.type) {
    case 'scenario_start':
    case 'case_start':
      addLog('info', `Started: ${event.stepName}`)
      break
    case 'step_start':
      addLog('info', `-> Executing step: ${event.stepName}`)
      emit('step-update', { stepId: event.stepId, status: 'RUNNING' })
      break
    case 'step_complete':
      const color = event.status === 'PASS' ? 'success' : 'fail'
      addLog(color, `<- Step completed: ${event.stepName} [${event.status}]`)
      emit('step-update', { stepId: event.stepId, status: event.status, result: event.result })
      emit('variables-update', event.variables)
      break
    case 'request':
      addLog('request', event.payload)
      break
    case 'response':
      addLog('response', event.payload)
      break
    case 'scenario_complete':
    case 'case_complete':
      const type = event.type === 'case_complete' ? 'Case' : 'Scenario'
      addLog('info', `${type} completed. Final status: ${event.status}`)
      status.value = event.status
      isRunning.value = false
      // No extra manual close here, it's handled by ensuring listeners are removed in cleanUp/onError
      // or we can explicitly trigger a clean shutdown
      if (eventSource) {
        eventSource.close()
        eventSource = null
      }
      break
    case 'info':
      addLog('info', event.payload)
      break
    case 'error':
      addLog('error', `Error: ${event.payload}`)
      isRunning.value = false
      status.value = 'FAIL'
      if (eventSource) {
        eventSource.close()
        eventSource = null
      }
      break
  }
}

const addLog = (type, message) => {
  logs.value.push({
    type,
    message,
    timestamp: Date.now()
  })
  scrollToBottom()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (consoleBody.value) {
      consoleBody.value.scrollTop = consoleBody.value.scrollHeight
    }
  })
}

const close = () => {
  visibleLocal.value = false
  emit('update:visible', false)
  isRunning.value = false
  if (eventSource) {
    eventSource.close()
  }
}

watch(() => props.visible, (newVal) => {
  visibleLocal.value = newVal
}, { immediate: true })

const formatTime = (ts) => {
  return new Date(ts).toLocaleTimeString()
}

const getStatusType = (s) => {
  const map = { RUNNING: 'warning', PASS: 'success', FAIL: 'danger', ERROR: 'danger' }
  return map[s] || 'info'
}

const filteredLogs = computed(() => {
  let filtered = logs.value
  
  // Filter by level
  if (filterLevel.value) {
    filtered = filtered.filter(log => log.type === filterLevel.value)
  }
  
  // Filter by search text
  if (searchText.value && searchText.value.trim()) {
    const search = searchText.value.toLowerCase()
    filtered = filtered.filter(log => 
      log.message.toLowerCase().includes(search) ||
      log.type.toLowerCase().includes(search)
    )
  }
  
  return filtered
})

const handleSearch = () => {
  scrollToBottom()
}

const exportLogs = () => {
  const content = logs.value.map(log => 
    `[${formatTime(log.timestamp)}] [${log.type.toUpperCase()}] ${log.message}`
  ).join('\n')
  
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `scenario-${props.scenarioId}-execution-${Date.now()}.log`
  a.click()
  URL.revokeObjectURL(url)
}

onUnmounted(() => {
  if (eventSource) {
    eventSource.close()
  }
})

defineExpose({ start })

</script>

<style scoped>
.execution-console {
  /* Dynamic styles injected via :style */
  height: 250px;
  background: #1e1e1e;
  color: #d4d4d4;
  display: flex;
  flex-direction: column;
  z-index: 3000;
  border-top: 1px solid #333;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.console-header {
  height: 32px;
  background: #2d2d2d;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 12px;
  font-size: 12px;
  border-bottom: 1px solid #3c3c3c;
}

.title {
  margin-right: 12px;
  font-weight: bold;
}

.console-body {
  flex: 1;
  padding: 8px 12px;
  overflow-y: auto;
  font-size: 13px;
}

.log-line {
  margin-bottom: 4px;
}

.log-time {
  color: #888;
  margin-right: 8px;
}

.log-tag {
  margin-right: 8px;
  font-weight: bold;
}

.info .log-tag { color: #569cd6; }
.success .log-tag { color: #4ec9b0; }
.fail .log-tag { color: #f44747; }
.error .log-tag { color: #f44747; }
.request .log-tag { color: #9cdcfe; } /* Light Blue */
.response .log-tag { color: #ce9178; } /* Light Orange/Brown */
.running { color: #ce9178; }

.log-message {
  word-break: break-all;
}

.is-loading {
  margin-right: 8px;
  vertical-align: middle;
}
</style>
