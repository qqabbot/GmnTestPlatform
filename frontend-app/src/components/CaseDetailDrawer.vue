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

      <!-- Basic Info -->
      <el-form label-width="140px" size="default">
        <el-form-item label="Case Name" :required="!planId">
          <template #label>
            <div class="form-label-with-badge">
              <span>Case Name</span>
              <el-tag v-if="planId && isOverridden('caseName')" type="warning" size="small" effect="plain" class="override-badge">Modified</el-tag>
            </div>
          </template>
          <div class="input-with-reset">
            <el-input 
              v-model="caseData.caseName" 
              :placeholder="planId ? (originalData?.caseName || 'Enter name override') : 'Enter test case name'" 
            />
            <el-button v-if="planId && isOverridden('caseName')" link type="primary" @click="resetField('caseName')">Reset</el-button>
          </div>
        </el-form-item>
        
        <el-form-item label="HTTP Method" :required="!planId">
          <template #label>
            <div class="form-label-with-badge">
              <span>HTTP Method</span>
              <el-tag v-if="planId && isOverridden('method')" type="warning" size="small" effect="plain" class="override-badge">Modified</el-tag>
            </div>
          </template>
          <div class="input-with-reset">
            <el-radio-group v-model="caseData.method">
              <el-radio label="GET">GET</el-radio>
              <el-radio label="POST">POST</el-radio>
              <el-radio label="PUT">PUT</el-radio>
              <el-radio label="DELETE">DELETE</el-radio>
              <el-radio label="PATCH">PATCH</el-radio>
            </el-radio-group>
            <el-button v-if="planId && isOverridden('method')" link type="primary" @click="resetField('method')">Reset</el-button>
          </div>
        </el-form-item>
        
        <el-form-item label="Request URL" :required="!planId">
          <template #label>
            <div class="form-label-with-badge">
              <span>Request URL</span>
              <el-tag v-if="planId && isOverridden('url')" type="warning" size="small" effect="plain" class="override-badge">Modified</el-tag>
            </div>
          </template>
          <div class="input-with-reset">
            <el-input 
              v-model="caseData.url" 
              :placeholder="planId ? (originalData?.url || 'Enter URL override') : 'http://api.example.com/endpoint'" 
            />
            <el-button v-if="planId && isOverridden('url')" link type="primary" @click="resetField('url')">Reset</el-button>
          </div>
        </el-form-item>

        <!-- Variable Discovery Section (Only in Plan Mode) -->
        <div v-if="planId" class="variable-helper">
          <el-icon><InfoFilled /></el-icon>
          <span>Available Variables: </span>
          <el-tag 
            v-for="v in availableVariables" 
            :key="v" 
            size="small" 
            class="var-tag"
            @click="insertVariable(v)"
          >
           {{ v }}
          </el-tag>
        </div>

        <!-- Steps Section -->
        <el-divider>
          <span>Test Steps</span>
          <el-tag v-if="planId && isOverridden('steps')" type="warning" size="small" effect="plain" style="margin-left: 10px;">Modified in Plan</el-tag>
        </el-divider>
        
        <div v-if="caseData.steps && caseData.steps.length > 0" class="steps-section">
          <div v-for="(step, index) in caseData.steps" :key="step.id || index" class="step-card">
            <div class="step-header">
              <span class="step-index">{{ index + 1 }}</span>
              <el-tag :type="step.method === 'GET' ? 'success' : 'primary'" size="small">
                {{ step.method }}
              </el-tag>
              <span class="step-name">{{ step.stepName || 'Unnamed Step' }}</span>
              <el-button v-if="planId" link type="primary" size="small" @click="editStepOverride(step, index)">Override</el-button>
            </div>
            <div class="step-url">{{ step.url }}</div>
          </div>
        </div>
        <div v-else class="empty-steps">
          <el-empty description="No steps configured" :image-size="60" />
        </div>

        <!-- Assertion Section -->
        <el-divider>
          <span>Global Assertion</span>
          <el-tag v-if="planId && isOverridden('assertionScript')" type="warning" size="small" effect="plain" style="margin-left: 10px;">Modified</el-tag>
        </el-divider>
        
        <el-form-item label="Assertion Script">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
            <span class="label-text">Groovy Script</span>
            <el-button v-if="planId && isOverridden('assertionScript')" link type="primary" @click="resetField('assertionScript')">Reset</el-button>
          </div>
          <el-input
            v-model="caseData.assertionScript"
            type="textarea"
            :rows="6"
            placeholder="vars.put('token', jsonPath(response, '$.token'))"
          />
        </el-form-item>
      </el-form>

      <!-- Action Buttons -->
      <div class="drawer-actions">
        <el-button @click="handleClose">Cancel</el-button>
        <el-button v-if="!planId" type="primary" @click="openFullEditor">
          Open Full Editor
          <el-icon><EditPen /></el-icon>
        </el-button>
        <el-button type="success" @click="saveChanges" :loading="saving">
          {{ planId ? 'Save Overrides' : 'Save Changes' }}
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
import { EditPen, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { testCaseApi } from '../api/testCase'
import { testPlanApi } from '../api/testPlan'
import StepOverrideDialog from './StepOverrideDialog.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  caseId: {
    type: Number,
    default: null
  },
  planId: {
    type: Number,
    default: null
  },
  availableVariables: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const router = useRouter()
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const caseData = ref(null)
const originalData = ref(null) // The base template
const saving = ref(false)
const loading = ref(false)

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
    // The parent component should pass the case with overrides, but we'll also try to fetch from plan
    if (props.planId) {
      try {
        // Fetch the plan to get overrides for this case
        const plan = await testPlanApi.getById(props.planId)
        const planCase = plan.testCases?.find(c => c.id === id)
        
        if (planCase) {
          // Merge overrides into caseData
          caseData.value = JSON.parse(JSON.stringify(template))
          
          // Apply overrides if they exist
          if (planCase.caseNameOverride) caseData.value.caseName = planCase.caseNameOverride
          if (planCase.urlOverride) caseData.value.url = planCase.urlOverride
          if (planCase.methodOverride) caseData.value.method = planCase.methodOverride
          if (planCase.headersOverride) caseData.value.headers = planCase.headersOverride
          if (planCase.bodyOverride) caseData.value.body = planCase.bodyOverride
          if (planCase.assertionScriptOverride) caseData.value.assertionScript = planCase.assertionScriptOverride
          
          // Parse stepsOverride
          if (planCase.stepsOverride) {
            try {
              stepsOverridden.value = JSON.parse(planCase.stepsOverride)
            } catch (e) {
              stepsOverridden.value = []
            }
          } else {
            stepsOverridden.value = []
          }
        } else {
          // Case not found in plan, use template
          caseData.value = JSON.parse(JSON.stringify(template))
          stepsOverridden.value = []
        }
      } catch (e) {
        console.error('Failed to load plan overrides:', e)
        // Fallback to template
        caseData.value = JSON.parse(JSON.stringify(template))
        stepsOverridden.value = []
      }
    } else {
      // Not in plan mode, use template as-is
      caseData.value = JSON.parse(JSON.stringify(template))
      stepsOverridden.value = []
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

const saveChanges = async () => {
  if (!caseData.value) return
  
  saving.value = true
  try {
    if (props.planId) {
      // Save Overrides
      const overrides = {
        planId: props.planId,
        caseId: caseData.value.id,
        caseNameOverride: isOverridden('caseName') ? caseData.value.caseName : null,
        urlOverride: isOverridden('url') ? caseData.value.url : null,
        methodOverride: isOverridden('method') ? caseData.value.method : null,
        assertionScriptOverride: isOverridden('assertionScript') ? caseData.value.assertionScript : null,
        stepsOverride: stepsOverridden.value.length > 0 ? JSON.stringify(stepsOverridden.value) : null,
        enabled: true
      }
      
      await testPlanApi.saveCaseOverrides(props.planId, caseData.value.id, overrides)
      ElMessage.success('Plan overrides saved')
    } else {
      // Normal Update
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
</style>
