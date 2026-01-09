<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="70%"
    :before-close="handleClose"
    v-loading="loading"
  >
    <div v-if="caseData" class="drawer-content">
      <el-alert
        v-if="planId"
        title="Plan-Specific Overrides"
        type="info"
        description="Changes made here only apply to this Test Plan. The original Test Case remains unchanged."
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />

      <!-- Plan Mode: Simplified Configuration -->
      <div v-if="planId">
        <!-- 0. Source Case Reference (Read-only) -->
        <el-collapse v-model="activeCollapse" style="margin-bottom: 20px;">
          <el-collapse-item name="source">
            <template #title>
              <el-icon><InfoFilled /></el-icon>
              <span style="margin-left: 8px; font-weight: bold;">Source Case Reference (Read-only)</span>
            </template>
            <div class="source-info">
              <div class="info-row">
                <span class="info-label">Endpoint:</span>
                <el-tag size="small">{{ originalData?.method }}</el-tag>
                <span class="info-value">{{ originalData?.url }}</span>
              </div>
              
              <div v-if="originalData?.body" class="info-group">
                <div class="info-label">Request Body:</div>
                <div class="code-preview">
                  <pre>{{ originalData.body }}</pre>
                </div>
              </div>

              <div v-if="originalData?.steps?.length" class="info-group">
                <div class="info-label">Steps ({{ originalData.steps.length }}):</div>
                <div class="steps-list">
                  <div v-for="(step, idx) in originalData.steps" :key="idx" class="step-summary">
                    <span class="step-index-mini">{{ idx + 1 }}</span>
                    <el-tag size="small" type="info" style="margin: 0 5px">{{ step.method }}</el-tag>
                    <span class="step-url-mini">{{ step.url }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>

        <!-- 1. Parameter Mapping -->
        <el-divider content-position="left">
          <el-icon><Setting /></el-icon>
          <span style="margin-left: 8px">Parameter Mappings (Input)</span>
        </el-divider>
        <p class="section-desc">Map variables from previous steps to this case's inputs (e.g., URL or Body).</p>
        
        <el-table :data="parameterList" border size="small" style="margin-bottom: 10px">
          <el-table-column label="Target Variable" width="180">
            <template #default="{ row }">
              <el-input v-model="row.key" placeholder="e.g. userId" />
            </template>
          </el-table-column>
          <el-table-column label="Source (Expression)">
            <template #default="{ row }">
              <el-input v-model="row.value" placeholder="e.g. ${loginCase.userId} or 123">
                <template #append>
                  <el-dropdown trigger="click" @command="(val) => row.value = val">
                    <el-button link><el-icon><ArrowDown /></el-icon></el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item v-for="v in availableVariables" :key="v" :command="v">{{ v }}</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </template>
              </el-input>
            </template>
          </el-table-column>
          <el-table-column label="Action" width="80" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link @click="removeParameter($index)"><el-icon><Close /></el-icon></el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button type="primary" link @click="addParameter"><el-icon><Plus /></el-icon> Add Parameter</el-button>

        <!-- 2. Variable Extractors -->
        <el-divider content-position="left">
          <el-icon><Search /></el-icon>
          <span style="margin-left: 8px">Variable Extractors (Output)</span>
        </el-divider>
        <p class="section-desc">Extract values from response to be used by subsequent cases.</p>
        
        <el-table :data="extractors" border size="small" style="margin-bottom: 10px">
          <el-table-column label="Save To Variable" width="180">
            <template #default="{ row }">
              <el-input v-model="row.variable" placeholder="e.g. new_token" />
            </template>
          </el-table-column>
          <el-table-column label="Source" width="120">
            <template #default="{ row }">
              <el-select v-model="row.source">
                <el-option label="JSONPath" value="json" />
                <el-option label="Regex" value="regex" />
                <el-option label="Header" value="header" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="Expression">
            <template #default="{ row }">
              <el-input v-model="row.expression" :placeholder="row.source === 'json' ? '$.data.id' : 'expression'" />
            </template>
          </el-table-column>
          <el-table-column label="Action" width="80" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link @click="removeExtractor($index)"><el-icon><Close /></el-icon></el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button type="primary" link @click="addExtractor"><el-icon><Plus /></el-icon> Add Extractor</el-button>

        <!-- 3. Assertion Logic -->
        <el-divider content-position="left">
          <el-icon><Checked /></el-icon>
          <span style="margin-left: 8px">Custom Assertion Logic</span>
        </el-divider>
        <el-input
          v-model="caseData.assertionScript"
          type="textarea"
          :rows="5"
          placeholder="// Any custom Groovy logic here (extractors handled above automatically)"
          style="font-family: monospace;"
        />
      </div>

      <!-- Normal Mode: Full Preview (If needed for ref) or other views -->
      <div v-else>
        <!-- Existing full form for editing original case - mostly keeping as is -->
        <el-form label-width="120px">
           <el-form-item label="Case Name">
             <el-input v-model="caseData.caseName" />
           </el-form-item>
           <!-- URL and Method already shown or needed? Keeping it simple for now -->
            <el-form-item label="Request Info">
              <el-tag>{{ caseData.method }}</el-tag>
              <span style="margin-left: 10px">{{ caseData.url }}</span>
            </el-form-item>
            
            <el-divider>Steps</el-divider>
            <div class="steps-list">
               <div v-for="(step, idx) in caseData.steps" :key="idx" class="step-summary">
                 <el-tag size="small">{{ step.method }}</el-tag> {{ step.url }}
               </div>
            </div>

            <el-divider>Logic</el-divider>
            <el-input
              v-model="caseData.assertionScript"
              type="textarea"
              :rows="8"
            />
        </el-form>
      </div>

      <div class="drawer-actions">
        <el-button @click="handleClose">Close</el-button>
        <el-button v-if="!planId" type="primary" @click="openFullEditor">Full Editor</el-button>
        <el-button type="success" :loading="saving" @click="saveChanges">
          {{ planId ? 'Update Plan Step' : 'Update Case' }}
        </el-button>
      </div>
    </div>

    <!-- Step Override Dialog -->
    <step-override-dialog
      v-model="showStepDialog"
      :step="currentStep"
      :override="currentStepOverride"
      @confirm="handleStepOverrideConfirm"
    />
  </el-drawer>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { EditPen, InfoFilled, Search, Checked, Plus, ArrowDown, Setting, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { testCaseApi } from '../api/testCase'
import { testPlanApi } from '../api/testPlan'
import StepOverrideDialog from './StepOverrideDialog.vue'

const props = defineProps({
  modelValue: Boolean,
  caseId: [Number, String],
  planId: [Number, String],
  availableVariables: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const saving = ref(false)
const caseData = ref(null)
const originalData = ref(null)
const availableVariables = ref([])
const extractors = ref([])
const parameterList = ref([]) // For UI editing of parameterOverrides
const activeCollapse = ref(['source']) // Keep source expanded by default

// Extractor sources
const extractorSources = [
  { label: 'JSONPath', value: 'json' },
  { label: 'Regex', value: 'regex' },
  { label: 'Header', value: 'header' }
]
const router = useRouter()

// Step Override State
const showStepDialog = ref(false)
const currentStep = ref(null)
const currentStepOverride = ref(null)
const stepsOverridden = ref([]) // List of {stepId, ...changes}

const drawerTitle = computed(() => {
  if (loading.value) return 'Loading case data...'
  if (!caseData.value) return 'Test Case Details'
  const name = caseData.value.caseName || 'Unnamed Case'
  return props.planId ? `Plan Override: ${name}` : `Edit Case: ${name}`
})

watch([() => props.caseId, () => props.modelValue], async ([newId, isVisible]) => {
  if (isVisible && newId) {
    await loadCaseData(newId)
  } else if (!isVisible) {
    caseData.value = null
    originalData.value = null
  }
}, { immediate: true })

const loadCaseData = async (id) => {
  loading.value = true
  try {
    // 1. Load the original test case as a template
    const template = await testCaseApi.getById(id)
    originalData.value = JSON.parse(JSON.stringify(template))
    
    // 2. If planId is present, we need to load plan-specific overrides
    if (props.planId) {
      try {
        const plan = await testPlanApi.getById(props.planId)
        const planCase = plan.testCases?.find(c => c.id === id)
        
        caseData.value = JSON.parse(JSON.stringify(template))
        
        if (planCase) {
          // Simplified: only parameterOverrides and assertionScriptOverride
          if (planCase.parameterOverrides) {
             caseData.value.parameterOverrides = planCase.parameterOverrides
             try {
                const params = JSON.parse(planCase.parameterOverrides)
                parameterList.value = Object.entries(params).map(([key, value]) => ({ key, value }))
             } catch (e) {
                parameterList.value = []
             }
          }
          
          if (planCase.assertionScriptOverride) {
            caseData.value.assertionScript = planCase.assertionScriptOverride
            extractors.value = parseExtractorsFromScript(planCase.assertionScriptOverride)
          } else {
            extractors.value = []
          }
        } else {
          parameterList.value = []
          extractors.value = []
        }
      } catch (e) {
        console.error('Failed to load plan overrides:', e)
        caseData.value = JSON.parse(JSON.stringify(template))
      }
    } else {
      caseData.value = JSON.parse(JSON.stringify(template))
    }
  } catch (error) {
    ElMessage.error('Failed to load case data')
    visible.value = false
  } finally {
    loading.value = false
  }
}

const isOverridden = (field) => {
  if (!props.planId || !caseData.value || !originalData.value) return false
  
  if (field === 'steps') {
    return stepsOverridden.value.length > 0
  }
  
  return caseData.value[field] !== originalData.value[field]
}

const resetField = (field) => {
  if (!originalData.value) return
  caseData.value[field] = JSON.parse(JSON.stringify(originalData.value[field]))
}

const handleClose = () => {
  // Check for unsaved changes
  if (JSON.stringify(caseData.value) !== JSON.stringify(originalData.value)) {
    // If not in plan mode, or if modified in plan mode
    // ElMessage.warning('You have unsaved changes')
  }
  visible.value = false
}

const openFullEditor = () => {
  if (caseData.value && caseData.value.id) {
    router.push(`/testing/cases/${caseData.value.id}/edit`)
    visible.value = false
  }
}

const insertVariable = (v) => {
  // Simple append for now, can be improved with cursor position later
  if (caseData.value.url !== undefined) {
    if (!caseData.value.url.includes('${' + v + '}')) {
       caseData.value.url += '${' + v + '}'
    }
  }
}

const editStepOverride = (step, index) => {
  currentStep.value = step
  currentStepOverride.value = stepsOverridden.value.find(o => o.stepId === step.id) || null
  showStepDialog.value = true
}

const handleStepOverrideConfirm = (override) => {
  const index = stepsOverridden.value.findIndex(o => o.stepId === currentStep.value.id)
  
  if (override) {
    if (index !== -1) {
      stepsOverridden.value[index] = override
    } else {
      stepsOverridden.value.push(override)
    }
  } else if (index !== -1) {
    // If null returned, it means values match original, remove override
    stepsOverridden.value.splice(index, 1)
  }
}

// Extractor Management
const addExtractor = () => {
  extractors.value.push({ variable: '', source: 'json', expression: '' })
}

const removeExtractor = (index) => {
  extractors.value.splice(index, 1)
}

const parseExtractorsFromScript = (script) => {
  const extracted = []
  if (!script) return extracted
  const lines = script.split('\n')
  for (const line of lines) {
    const jsonMatch = line.match(/vars\.put\s*\(\s*["']([^"']+)["']\s*,\s*jsonPath\s*\(\s*response\s*,\s*['"]([^'"]+)['"]\s*\)\s*\)/)
    if (jsonMatch) {
      extracted.push({ variable: jsonMatch[1], source: 'json', expression: jsonMatch[2] })
      continue
    }
    const regexMatch = line.match(/vars\.put\s*\(\s*["']([^"']+)["']\s*,\s*regex\s*\(\s*response\s*,\s*['"]([^'"]+)['"]\s*\)\s*\)/)
    if (regexMatch) {
      extracted.push({ variable: regexMatch[1], source: 'regex', expression: regexMatch[2] })
      continue
    }
    const headerMatch = line.match(/vars\.put\s*\(\s*["']([^"']+)["']\s*,\s*headers\s*\[\s*["']([^"']+)["']\s*\]\s*\)/)
    if (headerMatch) {
      extracted.push({ variable: headerMatch[1], source: 'header', expression: headerMatch[2] })
    }
  }
  return extracted
}

const compileExtractorsToScript = () => {
  let script = ''
  extractors.value.forEach(ext => {
    if (!ext.variable || !ext.expression) return
    if (ext.source === 'json') script += `vars.put("${ext.variable}", jsonPath(response, '${ext.expression}'))\n`
    else if (ext.source === 'regex') script += `vars.put("${ext.variable}", regex(response, '${ext.expression}'))\n`
    else if (ext.source === 'header') script += `vars.put("${ext.variable}", headers["${ext.expression}"])\n`
  })
  return script.trim()
}

// Parameter Override Management
const addParameter = () => {
  parameterList.value.push({ key: '', value: '' })
}

const removeParameter = (index) => {
  parameterList.value.splice(index, 1)
}

const saveChanges = async () => {
  if (!caseData.value) return
  
  saving.value = true
  try {
    if (props.planId) {
      // Compile extractors into assertion script
      const extractorScript = compileExtractorsToScript()
      const currentScript = caseData.value.assertionScript || ''
      const assertionLines = currentScript.split('\n').filter(line => !line.includes('vars.put'))
      const finalAssertionScript = [...assertionLines, extractorScript].join('\n').trim()

      // Compile parameters into JSON
      const paramsObj = {}
      parameterList.value.forEach(p => { if (p.key) paramsObj[p.key] = p.value })

      const overrides = {
        planId: props.planId,
        caseId: caseData.value.id,
        parameterOverrides: JSON.stringify(paramsObj),
        assertionScriptOverride: finalAssertionScript,
        enabled: true
      }
      
      await testPlanApi.saveCaseParameters(props.planId, caseData.value.id, overrides)
      ElMessage.success('Plan configuration saved')
    } else {
      await testCaseApi.update(caseData.value.id, caseData.value)
      ElMessage.success('Case updated successfully')
    }
    
    emit('saved', caseData.value)
    visible.value = false
  } catch (error) {
    ElMessage.error('Failed to save changes: ' + (error.response?.data?.message || error.message))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.drawer-content {
  padding: 0 20px 20px 20px;
}

.form-label-with-badge {
  display: flex;
  align-items: center;
  gap: 8px;
}

.override-badge {
  font-size: 10px;
  height: 18px;
  padding: 0 4px;
}

.input-with-reset {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.input-with-reset .el-input {
  flex: 1;
}

.variable-helper {
  margin: -10px 0 20px 140px;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.var-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.var-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.steps-section {
  margin-bottom: 20px;
}

.step-card {
  background-color: #f5f7fa;
  border-left: 3px solid #409eff;
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 4px;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.step-index {
  background-color: #409eff;
  color: white;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.step-name {
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.step-url {
  font-size: 12px;
  color: #606266;
  margin-left: 34px;
  font-family: 'Courier New', monospace;
}

.empty-steps {
  padding: 20px;
  text-align: center;
}

.code-preview {
  background-color: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.code-preview pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #303133;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.placeholder-text {
  color: #909399;
  font-size: 13px;
}

.label-text {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.drawer-actions {
  position: sticky;
  bottom: 0;
  background-color: white;
  padding: 15px 0;
  border-top: 1px solid #dcdfe6;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 30px;
}

.section-desc {
  font-size: 13px;
  color: #909399;
  margin: -10px 0 15px 0;
}

.variable-helper {
  margin: 15px 0;
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.var-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.var-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.step-summary {
  margin-bottom: 5px;
  font-size: 13px;
  display: flex;
  align-items: center;
}

.step-index-mini {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  background-color: #909399;
  color: white;
  border-radius: 50%;
  font-size: 10px;
}

.step-url-mini {
  font-family: monospace;
  font-size: 11px;
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.source-info {
  padding: 10px;
  background-color: #f8f9fb;
  border-radius: 4px;
}

.info-row {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-label {
  font-size: 13px;
  font-weight: bold;
  color: #606266;
  min-width: 80px;
}

.info-group {
  margin-top: 12px;
}

.info-value {
  font-family: monospace;
  font-size: 13px;
  color: #303133;
}
</style>
