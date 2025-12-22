<template>
  <el-dialog
    :model-value="visible"
    title="Report Details"
    width="80%"
    @update:model-value="$emit('update:visible', $event)"
    destroy-on-close
  >
    <div class="report-detail" id="report-content" v-if="report">
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

      <!-- Logs/Details -->
      <div class="detail-section">
        <div class="section-header">
          <h3>Execution Log</h3>
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
        <pre class="log-block">{{ report.detail }}</pre>
      </div>

      <!-- AI Diagnosis Result -->
      <div v-if="aiDiagnosis" class="detail-section ai-diagnosis">
        <h3><el-icon><MagicStick /></el-icon> AI Analysis</h3>
        <div class="ai-content">{{ aiDiagnosis }}</div>
      </div>

      <!-- Request/Response (Placeholder if data available) -->
      <!-- Note: Currently the report entity might only store a summary string in 'detail'. 
           If we have structured request/response, we would display it here. 
           For now, we display the 'detail' field which contains the logs. -->
      
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
const aiDiagnosis = ref('')

watch(() => props.visible, (newVal) => {
  if (newVal) {
    aiDiagnosis.value = ''
  }
})

const handleDiagnose = async () => {
  diagnosing.value = true
  aiDiagnosis.value = ''
  try {
    const result = await aiApi.diagnose({
      caseName: props.report.caseName,
      logs: props.report.detail
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
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
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
