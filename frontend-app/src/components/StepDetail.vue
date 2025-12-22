<template>
  <div class="step-detail" v-if="step">
    <el-form label-position="top" size="default">
      <!-- Basic Info -->
      <div class="form-row">
        <el-form-item label="Step Name" style="flex: 1; margin-right: 20px;">
          <el-input v-model="step.stepName" placeholder="Enter step name" />
        </el-form-item>
      </div>

      <!-- Step Type Selection -->
      <el-form-item label="Step Type">
        <el-radio-group v-model="stepType" @change="handleStepTypeChange">
          <el-radio label="custom">Custom Step</el-radio>
          <el-radio label="reference">Reference Existing Case</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- Custom Step Fields -->
      <template v-if="stepType === 'custom'">
        <el-form-item label="Method" style="width: 120px;">
          <el-select v-model="step.method">
            <el-option value="GET" label="GET" />
            <el-option value="POST" label="POST" />
            <el-option value="PUT" label="PUT" />
            <el-option value="DELETE" label="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="URL">
          <variable-input v-model="step.url" placeholder="http://api.example.com/resource">
            <template #prepend>
              <el-icon><Link /></el-icon>
            </template>
          </variable-input>
        </el-form-item>
      </template>

      <!-- Reference Case Selection -->
      <template v-if="stepType === 'reference'">
        <el-form-item label="Reference Case" required>
          <el-select
            v-model="step.referenceCaseId"
            placeholder="Select a test case to reference"
            filterable
            style="width: 100%"
            @change="handleReferenceCaseChange"
          >
            <el-option
              v-for="caseOption in availableCases"
              :key="caseOption.id"
              :label="`${caseOption.caseName} (${caseOption.method} ${caseOption.url})`"
              :value="caseOption.id"
            />
          </el-select>
          <div class="help-text" v-if="step.referenceCaseId && selectedReferenceCase">
            Referencing: {{ selectedReferenceCase.caseName }}
          </div>
        </el-form-item>
      </template>

      <!-- Tabs for Details -->
      <el-tabs v-model="activeTab" class="detail-tabs">
        
        <!-- Headers -->
        <el-tab-pane label="Headers" name="headers">
          <monaco-editor
            v-model="step.headers"
            language="json"
            height="300px"
          />
          <div class="help-text">Enter headers as JSON object, e.g. {"Content-Type": "application/json"}</div>
        </el-tab-pane>

        <!-- Body -->
        <el-tab-pane label="Body" name="body">
          <div class="section-header" style="margin-bottom: 10px; display: flex; justify-content: flex-end;">
            <el-button 
              type="warning" 
              size="small" 
              link 
              @click="handleMockBody"
              :loading="mockingBody"
            >
              <el-icon><MagicStick /></el-icon> AI Mock Body
            </el-button>
          </div>
          <monaco-editor
            v-model="step.body"
            language="json"
            height="400px"
          />
        </el-tab-pane>

        <!-- Assertions -->
        <el-tab-pane label="Assertions" name="assertions">
          <div class="list-actions">
             <el-button type="primary" size="small" @click="addAssertion">
               <el-icon><Plus /></el-icon> Add Assertion
             </el-button>
          </div>
          
          <div v-if="assertions.length === 0" class="empty-list">
            No assertions. Add one to validate response.
          </div>
          
          <div v-else class="cards-container">
            <el-card v-for="(item, index) in assertions" :key="index" shadow="hover" class="item-card">
              <div class="card-header">
                <span class="card-title">Assertion #{{ index + 1 }}</span>
                <el-button type="danger" link @click="removeAssertion(index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <div class="card-body">
                <el-row :gutter="10">
                  <el-col :span="6">
                    <el-select v-model="item.source" placeholder="Source" size="small">
                      <el-option label="Status Code" value="status" />
                      <el-option label="JSON Body" value="json" />
                      <el-option label="Header" value="header" />
                    </el-select>
                  </el-col>
                  <el-col :span="8" v-if="item.source !== 'status'">
                    <el-input v-model="item.property" placeholder="Property (e.g. $.id)" size="small" />
                  </el-col>
                  <el-col :span="5">
                    <el-select v-model="item.operator" placeholder="Op" size="small">
                      <el-option label="Equals" value="==" />
                      <el-option label="Not Equals" value="!=" />
                      <el-option label="Contains" value="contains" />
                    </el-select>
                  </el-col>
                  <el-col :span="item.source !== 'status' ? 5 : 13">
                    <el-input v-model="item.expected" placeholder="Expected Value" size="small" />
                  </el-col>
                </el-row>
              </div>
            </el-card>
          </div>
          
          <!-- Sync Warning -->
          <div class="sync-warning">
             <el-alert title="Assertions specific to this step. Will be compiled to assertionScript." type="info" show-icon :closable="false" />
          </div>
        </el-tab-pane>

        <!-- Extractors (New) -->
        <el-tab-pane label="Extractors" name="extractors">
           <div class="list-actions">
             <el-button size="small" @click="addExtractor">
               <el-icon><Plus /></el-icon> Add Extractor
             </el-button>
          </div>

          <div v-if="extractors.length === 0" class="empty-list">
            No extractors. Add one to save variables.
          </div>

           <div v-else class="cards-container">
            <el-card v-for="(item, index) in extractors" :key="index" shadow="hover" class="item-card">
              <div class="card-header">
                <span class="card-title">Extractor #{{ index + 1 }}</span>
                <el-button type="danger" link @click="removeExtractor(index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <div class="card-body">
                <el-row :gutter="10">
                   <el-col :span="6">
                    <el-select v-model="item.source" placeholder="Source" size="small">
                      <el-option label="JSON Body" value="json" />
                      <el-option label="Header" value="header" />
                    </el-select>
                  </el-col>
                  <el-col :span="10">
                    <el-input v-model="item.expression" placeholder="Expression (e.g. $.token)" size="small" />
                  </el-col>
                  <el-col :span="8">
                    <el-input v-model="item.variable" placeholder="Var Name" size="small">
                      <template #prepend>$</template>
                    </el-input>
                  </el-col>
                </el-row>
              </div>
            </el-card>
          </div>
        </el-tab-pane>
        
        <!-- Script (Raw) -->
        <el-tab-pane label="Script" name="script">
           <div style="margin-bottom: 5px;">
             <el-alert type="success" :closable="false" show-icon>
               <template #title>
                 <b>Groovy Script Examples:</b>
               </template>
               <template #default>
                 <pre style="margin: 5px 0; font-family: monospace; background: #f4f4f5; padding: 5px; border-radius: 4px;">// Assertions
assert status_code == 200
assert jsonPath(response, "$.data.id") != null

// Extractor (Save to variable)
vars.put("new_token", jsonPath(response, "$.token"))</pre>
               </template>
             </el-alert>
           </div>
           <monaco-editor v-model="step.assertionScript" language="groovy" height="400px" />
           <div class="help-text">
             Groovy script for advanced assertions and variable extraction.<br>
             Note: Changes here will NOT sync back to the Assertions/Extractors UI cards.
           </div>
        </el-tab-pane>

      </el-tabs>
    </el-form>
  </div>
  <div v-else class="empty-selection">
    <el-empty description="Select a step to edit details" />
  </div>
</template>

<script setup>
import { ref, watch, computed, onUnmounted } from 'vue'
import MonacoEditor from './MonacoEditor.vue'
import VariableInput from './VariableInput.vue'
import { testCaseApi } from '../api/testCase'
import { aiApi } from '../api/ai'
import { MagicStick, Plus, Delete, Link } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue'])

const step = ref(null)
const activeTab = ref('body')
const assertions = ref([])
const extractors = ref([])
const stepType = ref('custom') // 'custom' or 'reference'
const availableCases = ref([])
const selectedReferenceCase = ref(null)
const mockingBody = ref(false)

const handleMockBody = async () => {
  if (!step.value.url) {
    ElMessage.warning('Please enter a URL first')
    return
  }
  mockingBody.value = true
  try {
    const result = await aiApi.generateMock({
      url: step.value.url,
      method: step.value.method,
      description: step.value.stepName
    })
    step.value.body = result
    ElMessage.success('Mock body generated')
  } catch (error) {
    ElMessage.error('AI Mock Generation failed: ' + error.message)
  } finally {
    mockingBody.value = false
  }
}

// Initialize local state from prop
// Watch moved to bottom

// State management
const addAssertion = () => {
  assertions.value.push({ source: 'json', property: '', operator: '==', expected: '' })
}
const removeAssertion = (index) => {
  assertions.value.splice(index, 1)
}

const addExtractor = () => {
  extractors.value.push({ source: 'json', expression: '', variable: '' })
}
const removeExtractor = (index) => {
  extractors.value.splice(index, 1)
}

// Watch for changes in UI lists and update step + emit
// Deep watcher moved to after debounceEmit

let updateTimer = null
const debounceEmit = () => {
  if (updateTimer) clearTimeout(updateTimer)
  updateTimer = setTimeout(() => {
    if (step.value) {
        emit('update:modelValue', step.value)
    }
    updateTimer = null
  }, 300)
}

onUnmounted(() => {
  if (updateTimer) {
    clearTimeout(updateTimer)
    updateTimer = null
  }
})

// Watch for changes in UI lists and update step + emit (Moved here to ensure debounceEmit is defined)
// Watch for UI lists changes -> Compile script + Sync UI state
watch([assertions, extractors], () => {
  if (!step.value) return
  
  // Save UI state
  step.value._ui_assertions = assertions.value
  step.value._ui_extractors = extractors.value
  
  if (assertions.value.length > 0 || extractors.value.length > 0) {
      let script = ""
      assertions.value.forEach(a => {
        if (a.source === 'status') {
          script += `assert status_code ${a.operator} ${a.expected}\n`
        } else if (a.source === 'json') {
          script += `assert jsonPath(response, "${a.property}") ${a.operator} "${a.expected}"\n`
        } else if (a.source === 'header') {
          script += `assert headers["${a.property}"] ${a.operator} "${a.expected}"\n`
        }
      })
      extractors.value.forEach(e => {
        if (e.source === 'json' && e.variable) {
          let varName = e.variable.trim()
          if (varName.startsWith('${') && varName.endsWith('}')) {
            varName = varName.substring(2, varName.length - 1)
          } else if (varName.startsWith('{') && varName.endsWith('}')) {
            varName = varName.substring(1, varName.length - 1)
          } else if (varName.startsWith('$')) {
            varName = varName.substring(1)
          }
          script += `vars.put("${varName}", jsonPath(response, '${e.expression}'))\n`
        }
      })
      step.value.assertionScript = script
  }
  // No emit here. The modification to 'step' will trigger the step watcher below.
}, { deep: true })

// Watch for any changes in step (including those caused by the watcher above) and emit
watch(step, () => {
  if (step.value && !isInitializing.value) {
    debounceEmit()
  }
}, { deep: true })

// Cache for available cases to avoid repeated API calls
let casesCache = null
let casesCacheTime = 0
const CACHE_DURATION = 60000 // 1 minute cache

const loadAvailableCases = async () => {
  try {
    // Use cache if available and not expired
    const now = Date.now()
    if (casesCache && (now - casesCacheTime) < CACHE_DURATION) {
      availableCases.value = casesCache
      return
    }
    
    // Load all test cases for reference selection
    // In the future, we could filter by current project/module
    const cases = await testCaseApi.getAll()
    availableCases.value = cases
    // Update cache
    casesCache = cases
    casesCacheTime = now
  } catch (error) {
    console.error('Failed to load test cases for reference:', error)
    availableCases.value = []
  }
}

const loadReferenceCase = async (caseId) => {
  try {
    selectedReferenceCase.value = await testCaseApi.getById(caseId)
  } catch (error) {
    console.error('Failed to load referenced case:', error)
    selectedReferenceCase.value = null
  }
}

const handleStepTypeChange = (type) => {
  if (type === 'reference') {
    // Clear custom step fields when switching to reference
    step.value.url = ''
    step.value.method = 'GET'
    step.value.body = ''
    step.value.headers = '{}'
    // Load available cases only when switching to reference type
    loadAvailableCases()
  } else {
    // Clear reference when switching to custom
    step.value.referenceCaseId = null
    selectedReferenceCase.value = null
    // Clear available cases to avoid unnecessary data
    availableCases.value = []
  }
  debounceEmit()
}

const handleReferenceCaseChange = async (caseId) => {
  if (caseId) {
    await loadReferenceCase(caseId)
    if (selectedReferenceCase.value) {
      // Update step name to reflect referenced case
      step.value.stepName = step.value.stepName || `Reference: ${selectedReferenceCase.value.caseName}`
    }
  } else {
    selectedReferenceCase.value = null
  }
  debounceEmit()
}

const isInitializing = ref(false)

// Initialize local state from prop (Moved to bottom to ensure functions are defined)
watch(() => props.modelValue, async (val) => {
  if (val) {
    try {
      // Prevent infinite loop: Only update if value actually changed semantically
      if (step.value && JSON.stringify(val) === JSON.stringify(step.value)) {
        return
      }
      
      isInitializing.value = true
      
      step.value = JSON.parse(JSON.stringify(val))
      assertions.value = step.value._ui_assertions || []
      extractors.value = step.value._ui_extractors || []
      
      // Determine step type
      if (step.value.referenceCaseId) {
        stepType.value = 'reference'
        await loadReferenceCase(step.value.referenceCaseId)
        // Only load available cases when step type is reference
        await loadAvailableCases()
      } else {
        stepType.value = 'custom'
        // Don't load cases for custom steps to avoid unnecessary API calls
        availableCases.value = []
      }
      
      // Use nextTick to ensure watchers have fired and completed their immediate effects before unblocking
      setTimeout(() => {
        isInitializing.value = false
      }, 50)
      
    } catch (e) {
      console.error('Error initializing step detail:', e)
      step.value = null
      isInitializing.value = false
    }
  } else {
    step.value = null
    assertions.value = []
    extractors.value = []
    stepType.value = 'custom'
    selectedReferenceCase.value = null
    isInitializing.value = false
  }
}, { immediate: true })

</script>

<style scoped>
.step-detail {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.form-row {
  display: flex;
  justify-content: space-between;
}

.detail-tabs {
  margin-top: 20px;
}

.help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.empty-selection {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.list-actions {
  margin-bottom: 15px;
  text-align: right;
}

.cards-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item-card {
  border-radius: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.card-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.empty-list {
  text-align: center;
  color: #909399;
  padding: 20px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}

.sync-warning {
  margin-top: 15px;
}
</style>
