<template>
  <el-dialog
    :model-value="visible"
    title="Report Details"
    width="85%"
    @update:model-value="$emit('update:visible', $event)"
    destroy-on-close
  >
    <div class="report-detail" id="report-content" v-if="report" v-loading="loadingLogs">
      <!-- Header Info -->
      <div class="detail-header">
        <div class="info-item">
          <span class="label">Case Name:</span>
          <span class="value">{{ report.caseName }}</span>
        </div>
        <div class="info-item">
          <span class="label">Status:</span>
          <el-tag :type="report.status === 'PASS' ? 'success' : 'danger'">{{ report.status }}</el-tag>
        </div>
        <div class="info-item">
          <span class="label">Duration:</span>
          <span class="value">{{ report.duration }} ms</span>
        </div>
        <div class="info-item">
          <span class="label">Executed At:</span>
          <span class="value">{{ report.executedAt }}</span>
        </div>
      </div>

      <!-- Summary Detail (Error Message etc.) -->
      <div v-if="report.detail" class="detail-section">
        <h3>Summary Detail</h3>
        <pre class="log-block summary-log">{{ report.detail }}</pre>
      </div>

      <!-- Execution Logs (Structured) -->
      <div class="detail-section">
        <div class="section-header">
          <h3>Step-by-Step logs</h3>
          <el-button 
            v-if="report.status !== 'PASS'" 
            type="warning" 
            size="small" 
            @click="handleDiagnose" 
            :loading="diagnosing"
          >
            <el-icon><MagicStick /></el-icon> AI Diagnose
          </el-button>
        </div>

        <el-collapse v-if="stepLogs.length > 0">
          <el-collapse-item v-for="(log, index) in stepLogs" :key="log.id" :name="index">
            <template #title>
              <div class="log-item-title">
                <el-tag :type="log.responseStatus >= 200 && log.responseStatus < 300 ? 'success' : 'danger'" size="small" class="status-tag">
                  {{ log.responseStatus || 'ERR' }}
                </el-tag>
                <span class="step-name">{{ log.stepName }}</span>
                <span class="request-url">{{ log.requestUrl }}</span>
              </div>
            </template>

            <div class="log-content">
              <el-tabs type="border-card">
                <el-tab-pane label="Request">
                  <div class="tab-content">
                    <p><strong>URL:</strong> {{ log.requestUrl }}</p>
                    <p><strong>Headers:</strong></p>
                    <pre class="json-block">{{ formatJson(log.requestHeaders) }}</pre>
                    <p v-if="log.requestBody"><strong>Body:</strong></p>
                    <pre v-if="log.requestBody" class="json-block">{{ formatJson(log.requestBody) }}</pre>
                  </div>
                </el-tab-pane>
                <el-tab-pane label="Response">
                  <div class="tab-content">
                    <p><strong>Status:</strong> {{ log.responseStatus }}</p>
                    <p><strong>Headers:</strong></p>
                    <pre class="json-block">{{ formatJson(log.responseHeaders) }}</pre>
                    <p><strong>Body:</strong></p>
                    <pre class="json-block">{{ formatJson(log.responseBody) }}</pre>
                  </div>
                </el-tab-pane>
                <el-tab-pane label="Variables">
                   <div class="tab-content">
                    <pre class="json-block">{{ formatJson(log.variableSnapshot) }}</pre>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>
          </el-collapse-item>
        </el-collapse>
        <el-empty v-else-if="!loadingLogs" description="No detailed logs available" />
      </div>

      <!-- AI Diagnosis Result -->
      <div v-if="aiDiagnosis" class="detail-section ai-diagnosis">
        <h3><el-icon><MagicStick /></el-icon> AI Analysis</h3>
        <div class="ai-content">{{ aiDiagnosis }}</div>
      </div>
    </div>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">Close</el-button>
      <el-button type="primary" @click="exportPDF" :loading="exporting">
        <el-icon><Download /></el-icon> Export PDF
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import { ElMessage } from 'element-plus'
import { MagicStick, Download } from '@element-plus/icons-vue'
import { aiApi } from '../api/ai'
import { reportApi } from '../api/report'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  report: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible'])
const exporting = ref(false)
const diagnosing = ref(false)
const loadingLogs = ref(false)
const aiDiagnosis = ref('')
const stepLogs = ref([])

watch(() => props.visible, async (newVal) => {
  if (newVal) {
    aiDiagnosis.value = ''
    stepLogs.value = []
    if (props.report?.id) {
      await loadStepLogs()
    }
  }
})

const loadStepLogs = async () => {
  loadingLogs.value = true
  try {
    stepLogs.value = await reportApi.getLogs(props.report.id)
  } catch (error) {
    console.error('Failed to load logs:', error)
    ElMessage.error('Failed to load detailed logs')
  } finally {
    loadingLogs.value = false
  }
}

const formatJson = (val) => {
  if (!val) return ''
  try {
    const obj = typeof val === 'string' ? JSON.parse(val) : val
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return val
  }
}

const handleDiagnose = async () => {
  diagnosing.value = true
  aiDiagnosis.value = ''
  try {
    const result = await aiApi.diagnose({
      caseName: props.report.caseName,
      logs: props.report.detail,
      stepLogs: stepLogs.value // Pass richer logs to AI if possible
    })
    aiDiagnosis.value = result
  } catch (error) {
    ElMessage.error('AI Diagnosis failed: ' + (error.response?.data?.error || error.message))
  } finally {
    diagnosing.value = false
  }
}

const exportPDF = async () => {
  exporting.value = true
  try {
    const element = document.getElementById('report-content')
    if (!element) return

    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      logging: false
    })

    const imgData = canvas.toDataURL('image/png')
    const pdf = new jsPDF('p', 'mm', 'a4')
    const pdfWidth = pdf.internal.pageSize.getWidth()
    const pdfHeight = pdf.internal.pageSize.getHeight()
    const imgWidth = pdfWidth
    const imgHeight = (canvas.height * pdfWidth) / canvas.width

    let heightLeft = imgHeight
    let position = 0

    pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
    heightLeft -= pdfHeight

    while (heightLeft >= 0) {
      position = heightLeft - imgHeight
      pdf.addPage()
      pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
      heightLeft -= pdfHeight
    }

    pdf.save(`report_${props.report.id}.pdf`)
    ElMessage.success('PDF exported successfully')
  } catch (error) {
    console.error(error)
    ElMessage.error('Failed to export PDF')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.detail-header {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.info-item {
  display: flex;
  align-items: center;
}

.label {
  font-weight: bold;
  margin-right: 10px;
  color: #606266;
}

.log-block {
  background-color: #282c34;
  color: #abb2bf;
  padding: 15px;
  border-radius: 4px;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  white-space: pre-wrap;
}

.summary-log {
  max-height: 150px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.log-item-title {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.status-tag {
  min-width: 45px;
  text-align: center;
}

.step-name {
  font-weight: bold;
  color: #303133;
}

.request-url {
  color: #909399;
  font-size: 0.9em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-content {
  padding: 10px 0;
}

.tab-content {
  font-size: 0.9em;
}

.json-block {
  background-color: #fafafa;
  border: 1px solid #eaeaea;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  white-space: pre-wrap;
  font-family: monospace;
}

.ai-diagnosis {
  margin-top: 20px;
  padding: 15px;
  border: 1px solid #e6a23c;
  border-radius: 4px;
  background-color: #fdf6ec;
}

.ai-diagnosis h3 {
  color: #e6a23c;
  margin-top: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-content {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #606266;
}
</style>

