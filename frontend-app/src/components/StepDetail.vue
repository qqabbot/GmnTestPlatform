<template>
  <div class="step-detail" v-if="step">
    <el-form label-position="top" size="default">
      <!-- Basic Info -->
      <div class="form-row">
        <el-form-item label="Step Name" style="flex: 1; margin-right: 20px;">
          <el-input v-model="step.stepName" placeholder="Enter step name" />
        </el-form-item>
        <el-form-item label="Method" style="width: 120px;">
          <el-select v-model="step.method">
            <el-option value="GET" label="GET" />
            <el-option value="POST" label="POST" />
            <el-option value="PUT" label="PUT" />
            <el-option value="DELETE" label="DELETE" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="URL">
        <variable-input v-model="step.url" placeholder="http://api.example.com/resource">
          <template #prepend>
            <el-icon><Link /></el-icon>
          </template>
        </variable-input>
      </el-form-item>

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
             <el-button type="secondary" size="small" @click="addExtractor">
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
import { ref, watch, computed } from 'vue'
import MonacoEditor from './MonacoEditor.vue'
import VariableInput from './VariableInput.vue'

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

// Initialize local state from prop
watch(() => props.modelValue, (val) => {
  if (val) {
    step.value = JSON.parse(JSON.stringify(val))
    // Attempt to parse assertions/extractors from existing properties if they existed, 
    // or just start empty. For Phase 3.2, strict persistence of UI state isn't mandatory if backend doesn't support it,
    // but better user experience would be to save them.
    // We will store them in `step` object as temporary fields if they exist.
    assertions.value = step.value._ui_assertions || []
    extractors.value = step.value._ui_extractors || []
  } else {
    step.value = null
    assertions.value = []
    extractors.value = []
  }
}, { immediate: true })

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
watch([step, assertions, extractors], () => {
  if (!step.value) return
  
  // Save UI state
  step.value._ui_assertions = assertions.value
  step.value._ui_extractors = extractors.value
  
  // Compile to assertionScript (Simple Generation)
  // Only compile if we are NOT currently editing the script directly (or if lists are not empty)
  // Ideally, we should have a flag "useRawScript".
  // For now, we always compile IF there are items in the list.
  // If list is empty, we don't overwrite, allowing manual script.
  if (assertions.value.length > 0 || extractors.value.length > 0) {
      let script = ""
      
      // Assertions
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
          script += `vars.put("${e.variable}", jsonPath(response, "${e.expression}"))\n`
        }
      })
      
      step.value.assertionScript = script
  }

  debounceEmit()
}, { deep: true })

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
