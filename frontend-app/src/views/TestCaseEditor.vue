<template>
  <div class="test-case-editor">
    <!-- Header -->
    <div class="editor-header">
      <div class="header-left">
        <el-button @click="$router.back()" link>
          <el-icon><ArrowLeft /></el-icon> Back
        </el-button>
        <el-divider direction="vertical" />
        <span class="page-title">{{ isEditMode ? 'Edit Test Case' : 'New Test Case' }}</span>
      </div>
      <div class="header-actions">
        <el-button type="info" @click="handleDryRun" :loading="store.loading">
          <el-icon><View /></el-icon> Dry Run
        </el-button>
        <el-button type="success" @click="handleRun" :loading="store.loading">
          <el-icon><VideoPlay /></el-icon> Run
        </el-button>
        <el-button type="primary" @click="handleSave" :loading="store.loading">
          <el-icon><Check /></el-icon> Save
        </el-button>
      </div>
    </div>

    <!-- Main Content -->
    <div class="editor-main">
      <!-- Left Sidebar: Step List -->
      <div class="sidebar">
        <step-list
          v-model:steps="store.currentCase.steps"
          :selected-index="selectedStepIndex"
          @select="selectedStepIndex = $event"
          @add="store.addStep"
          @remove="handleRemoveStep"
          @update-step="handleUpdateStep"
          @open-library="openLibrary"
        />
      </div>

      <!-- Center: Step Detail or Case Settings -->
      <div class="content-area">
        <el-tabs v-model="activeTab" type="border-card" class="main-tabs">
          <el-tab-pane label="Step Details" name="step">
            <step-detail v-model="currentStep" />
          </el-tab-pane>
          <el-tab-pane label="Case Settings" name="settings">
            <div class="settings-form">
              <el-form label-width="140px">
                <el-form-item label="Case Name" required>
                  <el-input v-model="store.currentCase.caseName" placeholder="Enter test case name" />
                </el-form-item>
                
                <el-form-item label="HTTP Method" required>
                  <el-radio-group v-model="store.currentCase.method">
                    <el-radio label="GET">GET</el-radio>
                    <el-radio label="POST">POST</el-radio>
                    <el-radio label="PUT">PUT</el-radio>
                    <el-radio label="DELETE">DELETE</el-radio>
                    <el-radio label="PATCH">PATCH</el-radio>
                  </el-radio-group>
                </el-form-item>
                
                <el-form-item label="Request URL" required>
                  <variable-input v-model="store.currentCase.url" placeholder="http://api.example.com/endpoint or ${base_url}/users">
                    <template #prepend>
                      <el-icon><Link /></el-icon>
                    </template>
                  </variable-input>
                  <div class="help-text">Use ${variable_name} for variable substitution. Type $ to autocomplete.</div>
                </el-form-item>

                <el-form-item label="Headers">
                  <monaco-editor v-model="store.currentCase.headers" language="json" height="150px" />
                  <div class="help-text">JSON headers object</div>
                </el-form-item>
                
                <el-form-item label="Request Body">
                  <monaco-editor v-model="store.currentCase.body" language="json" height="200px" />
                  <div class="help-text">JSON request body (for POST/PUT/PATCH methods)</div>
                </el-form-item>
                
                <el-divider />
                
                <el-form-item label="Project" required>
                  <el-select v-model="store.currentCase.projectId" placeholder="Select Project" @change="loadModules" style="width: 100%">
                    <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Module" required>
                  <el-select v-model="store.currentCase.moduleId" placeholder="Select Module" :disabled="!store.currentCase.projectId" style="width: 100%">
                    <el-option v-for="m in filteredModules" :key="m.id" :label="m.moduleName" :value="m.id" />
                  </el-select>
                </el-form-item>
                
                <el-divider />
                
                <el-form-item label="Global Assertion">
                  <monaco-editor v-model="store.currentCase.assertionScript" language="groovy" height="200px" />
                  <div class="help-text">Groovy script for assertions (e.g., status_code == 200)</div>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- Dry Run Dialog -->
    <el-dialog v-model="showDryRunDialog" title="Dry Run" width="600px">
      <el-form>
        <el-form-item label="Environment">
          <el-select v-model="dryRunEnv" placeholder="Select environment" style="width: 100%">
            <el-option
              v-for="env in environments"
              :key="env.id"
              :label="env.envName"
              :value="env.envName"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDryRunDialog = false">Cancel</el-button>
        <el-button type="primary" @click="executeDryRun" :loading="store.loading" :disabled="!dryRunEnv">Run</el-button>
      </template>
    </el-dialog>

    <!-- Execute Dialog -->
    <el-dialog v-model="showExecuteDialog" title="Execute Test Case" width="600px">
      <el-form>
        <el-form-item label="Environment">
          <el-select v-model="executeEnv" placeholder="Select environment" style="width: 100%">
            <el-option
              v-for="env in environments"
              :key="env.id"
              :label="env.envName"
              :value="env.envName"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showExecuteDialog = false">Cancel</el-button>
        <el-button type="success" @click="executeRun" :loading="store.loading" :disabled="!executeEnv">Execute</el-button>
      </template>
    </el-dialog>

    <!-- Step Library Drawer -->
    <el-drawer v-model="showLibraryDrawer" title="Step Library" size="30%">
      <div class="library-content">
        <el-input v-model="templateSearch" placeholder="Search templates..." prefix-icon="Search" style="margin-bottom: 15px;" />
        
        <div class="template-list">
          <div v-for="tpl in stepTemplates" :key="tpl.id" class="template-item">
            <div class="tpl-info">
              <el-tag size="small" :type="tpl.method === 'GET' ? 'success' : 'primary'">{{ tpl.method }}</el-tag>
              <span class="tpl-name">{{ tpl.name }}</span>
            </div>
            <el-button type="primary" size="small" link @click="importTemplate(tpl)">Add</el-button>
          </div>
          <div v-if="stepTemplates.length === 0" class="empty-tpl">
             No templates found. Go to Library to create one.
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- Execution Result Drawer -->
    <el-drawer v-model="showResult" :title="resultTitle" size="50%">
      <div v-if="store.executionResult" class="result-content">
        <!-- Dry Run Result -->
        <div v-if="isDryRunResult">
          <h3>Resolved URL</h3>
          <pre>{{ store.executionResult.resolvedUrl }}</pre>
          
          <h3>Resolved Body</h3>
          <monaco-editor :model-value="store.executionResult.resolvedBody" language="json" read-only height="300px" />
          
          <h3>Variables</h3>
          <pre>{{ JSON.stringify(store.executionResult.variables, null, 2) }}</pre>
        </div>

        <!-- Actual Execution Result -->
        <div v-else>
          <el-tag :type="store.executionResult.status === 'PASS' ? 'success' : 'danger'" size="large" style="margin-bottom: 20px;">
            {{ store.executionResult.status }}
          </el-tag>
          
          <h3>Duration</h3>
          <p>{{ store.executionResult.duration }} ms</p>

          <h3>Request</h3>
          <monaco-editor :model-value="JSON.stringify(store.executionResult.request || {}, null, 2)" language="json" read-only height="200px" />

          <h3>Response</h3>
          <monaco-editor :model-value="JSON.stringify(store.executionResult.response || {}, null, 2)" language="json" read-only height="300px" />
          
          <h3>Step Execution Details</h3>
          <el-collapse v-if="store.executionResult.logs && store.executionResult.logs.length > 0">
            <el-collapse-item v-for="(log, index) in store.executionResult.logs" :key="index" :name="index">
              <template #title>
                <div class="log-title">
                  <el-tag size="small" :type="log.responseStatus >= 200 && log.responseStatus < 300 ? 'success' : 'danger'" style="margin-right: 10px;">
                    {{ log.responseStatus || 'ERR' }}
                  </el-tag>
                  <span style="font-weight: bold;">{{ log.stepName }}</span>
                  <span style="margin-left: 10px; color: #909399; font-size: 12px;">{{ log.requestUrl }}</span>
                </div>
              </template>
              
              <el-tabs type="card">
                <el-tab-pane label="Request">
                  <div class="log-detail-item">
                     <h4>Headers</h4>
                     <monaco-editor :model-value="log.requestHeaders || '{}'" language="json" read-only height="150px" />
                  </div>
                  <div class="log-detail-item">
                     <h4>Body</h4>
                     <monaco-editor :model-value="log.requestBody || ''" language="json" read-only height="150px" />
                  </div>
                </el-tab-pane>
                <el-tab-pane label="Response">
                  <div class="log-detail-item">
                     <h4>Headers</h4>
                     <monaco-editor :model-value="log.responseHeaders || '{}'" language="json" read-only height="150px" />
                  </div>
                  <div class="log-detail-item">
                     <h4>Body</h4>
                     <monaco-editor :model-value="log.responseBody || ''" language="json" read-only height="200px" />
                  </div>
                </el-tab-pane>
                <el-tab-pane label="Variables">
                  <monaco-editor :model-value="log.variableSnapshot || '{}'" language="json" read-only height="300px" />
                </el-tab-pane>
              </el-tabs>
            </el-collapse-item>
          </el-collapse>
          <div v-else>
             <pre>{{ store.executionResult.detail }}</pre>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTestCaseStore } from '../stores/testCaseStore'
import { projectApi } from '../api/project'
import { testModuleApi } from '../api/testModule'
import { environmentApi } from '../api/environment'
import { stepTemplateApi } from '../api/stepTemplate'
import StepList from '../components/StepList.vue'
import StepDetail from '../components/StepDetail.vue'
import MonacoEditor from '../components/MonacoEditor.vue'
import VariableInput from '../components/VariableInput.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const store = useTestCaseStore()

const isEditMode = computed(() => !!route.params.id)
const selectedStepIndex = ref(-1)
const activeTab = ref('settings')
const projects = ref([])
const modules = ref([])
const environments = ref([])
const showDryRunDialog = ref(false)
const showExecuteDialog = ref(false)
const dryRunEnv = ref('')
const executeEnv = ref('')
const showResult = ref(false)
const isDryRun = ref(false)

// Step Library State
const showLibraryDrawer = ref(false)
const stepTemplates = ref([])
const templateSearch = ref('')

const openLibrary = async () => {
    showLibraryDrawer.value = true
    try {
        // Fetch templates for current project (if available) or all
        const projectId = store.currentCase.projectId
        stepTemplates.value = await stepTemplateApi.getAll({ projectId })
    } catch (e) {
        ElMessage.error('Failed to load step templates')
    }
}

const importTemplate = (template) => {
    // Copy logic
    const newStep = {
        stepName: template.name, // Use template name
        method: template.method,
        url: template.url,
        headers: template.headers || '{}',
        body: template.body || '',
        assertionScript: template.assertionScript || '',
        enabled: true,
        // Reset ID and Order
        stepOrder: store.currentCase.steps.length + 1
    }
    // Add to store
    store.currentCase.steps = [...store.currentCase.steps, newStep]
    ElMessage.success('Step added from library')
    // Don't close drawer to allow multiple adds
}

const resultTitle = computed(() => isDryRun.value ? 'Dry Run Result' : 'Execution Result')
const isDryRunResult = computed(() => isDryRun.value)

const currentStep = computed({
  get: () => selectedStepIndex.value >= 0 ? store.currentCase.steps[selectedStepIndex.value] : null,
  set: (val) => {
    if (selectedStepIndex.value >= 0) {
      store.updateStep(selectedStepIndex.value, val)
    }
  }
})

const filteredModules = computed(() => {
  if (!store.currentCase.projectId) return []
  return modules.value.filter(m => m.project && m.project.id === store.currentCase.projectId)
})

watch(selectedStepIndex, (val) => {
  if (val >= 0) {
    activeTab.value = 'step'
  } else {
    activeTab.value = 'settings'
  }
})

const loadData = async () => {
  try {
    // Load initial data in parallel
    const [projectsData, modulesData, environmentsData] = await Promise.all([
      projectApi.getAll(),
      testModuleApi.getAll(),
      environmentApi.getAll()
    ])
    
    projects.value = projectsData
    modules.value = modulesData
    environments.value = environmentsData
    
    // Set default environment if available
    if (environments.value.length > 0) {
      dryRunEnv.value = environments.value[0].envName
      executeEnv.value = environments.value[0].envName
    }
    
    if (isEditMode.value) {
      try {
        await store.loadCase(route.params.id)
        // Set project/module IDs correctly - handle all possible data structures
        if (store.currentCase.module) {
          if (typeof store.currentCase.module === 'object') {
            store.currentCase.moduleId = store.currentCase.module.id
            // Try to get project from module
            if (store.currentCase.module.project) {
              if (typeof store.currentCase.module.project === 'object') {
                store.currentCase.projectId = store.currentCase.module.project.id
              } else {
                store.currentCase.projectId = store.currentCase.module.project
              }
            }
          } else {
            store.currentCase.moduleId = store.currentCase.module
          }
        }
        // If no project found, try to infer from moduleId
        if (!store.currentCase.projectId && store.currentCase.moduleId) {
          const module = modules.value.find(m => m.id === store.currentCase.moduleId)
          if (module && module.project) {
            store.currentCase.projectId = typeof module.project === 'object' ? module.project.id : module.project
          }
        }
      } catch (loadError) {
        console.error('Error loading test case:', loadError)
        ElMessage.error('Failed to load test case: ' + (loadError.message || 'Unknown error'))
        router.push('/cases')
      }
    } else {
      store.resetCase()
      // Default to settings tab for new cases
      activeTab.value = 'settings'
    }
  } catch (error) {
    console.error('Failed to load initial data:', error)
    ElMessage.error('Failed to load projects and modules')
  }
}

const loadModules = () => {
  store.currentCase.moduleId = null
}

const handleRemoveStep = (index) => {
  store.removeStep(index)
  if (selectedStepIndex.value >= store.currentCase.steps.length) {
    selectedStepIndex.value = store.currentCase.steps.length - 1
  }
}

const handleUpdateStep = (index, updates) => {
  const step = store.currentCase.steps[index]
  if (step) {
    store.updateStep(index, { ...step, ...updates })
  }
}

const handleSave = async () => {
  try {
    // Validate required fields
    if (!store.currentCase.caseName || !store.currentCase.caseName.trim()) {
      ElMessage.warning('Please enter a case name')
      activeTab.value = 'settings'
      return
    }
    if (!store.currentCase.url || !store.currentCase.url.trim()) {
      ElMessage.warning('Please enter a request URL')
      activeTab.value = 'settings'
      return
    }
    if (!store.currentCase.moduleId) {
      ElMessage.warning('Please select a module')
      activeTab.value = 'settings'
      return
    }
    
    // Prepare payload with proper structure
    const payload = {
      ...store.currentCase,
      module: { id: store.currentCase.moduleId }
    }
    
    await store.saveCase()
    ElMessage.success(isEditMode.value ? 'Test case updated successfully' : 'Test case created successfully')
    
    if (!isEditMode.value) {
      router.replace(`/testing/cases/${store.currentCase.id}/edit`)
    }
  } catch (error) {
    console.error('Save failed:', error)
  }
}

const handleDryRun = () => {
  showDryRunDialog.value = true
}

const handleRun = () => {
  showExecuteDialog.value = true
}

const executeDryRun = async () => {
  await store.runDryRun(dryRunEnv.value)
  isDryRun.value = true
  showDryRunDialog.value = false
  showResult.value = true
}

const executeRun = async () => {
  await store.executeCase(executeEnv.value)
  isDryRun.value = false
  showExecuteDialog.value = false
  showResult.value = true
}


onMounted(() => {
  loadData()
})
</script>

<style scoped>
.test-case-editor {
  height: calc(100vh - 80px); /* Adjust based on layout header */
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.editor-header {
  padding: 10px 20px;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
}

.editor-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 300px;
  border-right: 1px solid #dcdfe6;
  background-color: #fcfcfc;
}

.content-area {
  flex: 1;
  background-color: #f5f7fa;
  padding: 10px;
  overflow: hidden;
}

.main-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.el-tabs__content) {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.settings-form {
  padding: 20px;
  max-width: 800px;
}

.result-content {
  padding: 20px;
}

.result-content pre {
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}

.help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.template-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
}
.template-item:hover {
  background-color: #f5f7fa;
}
.tpl-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tpl-name {
  font-size: 14px;
  font-weight: 500;
}
.empty-tpl {
  padding: 20px;
  text-align: center;
  color: #909399;
}

.log-title {
  display: flex;
  align-items: center;
  width: 100%;
}
.log-detail-item h4 {
  margin: 5px 0;
  font-size: 13px;
  color: #606266;
}
</style>
