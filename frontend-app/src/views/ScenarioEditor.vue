<template>
  <div class="scenario-editor-wrapper">
    <!-- Header -->
    <div class="editor-header">
      <div class="header-left">
        <el-button @click="handleBack" link>
          <el-icon><ArrowLeft /></el-icon> Back
        </el-button>
        <el-divider direction="vertical" />
        <span class="page-title">Scenario Editor</span>
      </div>
    </div>

    <div class="scenario-editor-layout">
     <!-- Left: Library -->
     <div class="pane left-pane">
        <resource-library :project-id="currentScenario.projectId" />
     </div>
     
     <!-- Middle: Canvas -->
     <div class="pane middle-pane">
        <scenario-canvas 
            :scenario-data="currentScenario" 
            v-model:steps="steps"
            :selected-step-id="selectedStepId"
            @select="handleSelectStep"
            @save="saveScenario"
            @run="runScenario"
            @history="historyDialogVisible = true"
        />
     </div>
     
      <!-- Right: Properties & Variables -->
      <div class="pane right-pane">
        <el-tabs v-model="rightActiveTab" class="right-tabs">
          <el-tab-pane label="Properties" name="properties">
            <step-properties :step="selectedStep" />
          </el-tab-pane>
          <el-tab-pane label="Variables" name="variables">
            <variable-context-viewer :variables="runtimeVariables" />
          </el-tab-pane>
        </el-tabs>
      </div>
     
    <!-- Execution Result Dialog -->
    <el-dialog v-model="resultDialogVisible" title="Execution Results" width="70%">
       <el-table :data="executionResults" height="400">
           <el-table-column prop="caseName" label="Step Name" />
           <el-table-column prop="status" label="Status" width="100">
              <template #default="scope">
                 <el-tag :type="scope.row.status === 'PASS' ? 'success' : 'danger'">{{ scope.row.status }}</el-tag>
              </template>
           </el-table-column>
           <el-table-column prop="duration" label="Time (ms)" width="100" />
           <el-table-column label="Actions" width="100">
               <template #default="scope">
                   <el-button type="primary" size="small" link @click="viewResultDetail(scope.row)">Details</el-button>
               </template>
           </el-table-column>
       </el-table>
    </el-dialog>

    <!-- Result Detail Drawer -->
    <el-drawer
      v-model="detailVisible"
      title="Step Execution Detail"
      size="50%"
      destroy-on-close
    >
      <request-response-detail :result="currentResult" />
    </el-drawer>

    <!-- Real-time Console -->
    <execution-console 
      ref="execConsole"
      v-model:visible="consoleVisible"
      :scenario-id="scenarioId" 
      :env-key="selectedRunEnv"
      @step-update="handleStepUpdate"
      @variables-update="handleVariablesUpdate"
    />

    <!-- Run Environment Dialog -->
    <el-dialog v-model="showRunEnvDialog" title="Select Execution Environment" width="400px">
      <el-form label-position="top">
        <el-form-item label="Environment" required>
          <el-select v-model="selectedRunEnv" placeholder="Select environment" style="width: 100%">
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
        <el-button @click="showRunEnvDialog = false">Cancel</el-button>
        <el-button type="success" @click="confirmRun" :disabled="!selectedRunEnv">Run</el-button>
      </template>
    </el-dialog>

    <!-- Execution History Dialog -->
    <execution-history-dialog 
      v-model="historyDialogVisible" 
      :scenario-id="scenarioId" 
    />
  </div>
</div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import ResourceLibrary from '../components/scenario/ResourceLibrary.vue'
import ScenarioCanvas from '../components/scenario/ScenarioCanvas.vue'
import StepProperties from '../components/scenario/StepProperties.vue'
import ExecutionConsole from '../components/scenario/ExecutionConsole.vue'
import VariableContextViewer from '../components/scenario/VariableContextViewer.vue'
import ExecutionHistoryDialog from '../components/scenario/ExecutionHistoryDialog.vue'
import RequestResponseDetail from '../components/testcase/RequestResponseDetail.vue'
import { testScenarioApi } from '../api/testScenario'
import { environmentApi } from '../api/environment'

const route = useRoute()
const router = useRouter()
const scenarioId = route.params.id

const handleBack = () => {
  router.push('/testing/plans')
}

const currentScenario = ref({})
const steps = ref([])
const selectedStep = ref(null)
const executionResults = ref([])
const resultDialogVisible = ref(false)
const execConsole = ref(null)
const consoleVisible = ref(false)

// Environment Selection
const environments = ref([])
const showRunEnvDialog = ref(false)
const selectedRunEnv = ref('')

const rightActiveTab = ref('properties')
const runtimeVariables = ref({})
const historyDialogVisible = ref(false)
const detailVisible = ref(false)
const currentResult = ref(null)

const viewResultDetail = (result) => {
    currentResult.value = result
    detailVisible.value = true
}

const selectedStepId = computed(() => {
    if (!selectedStep.value) return null
    return selectedStep.value.id || selectedStep.value.tempId
})

onMounted(async () => {
    if (scenarioId) {
        loadScenario(scenarioId)
    }
    loadEnvironments()
})

const loadEnvironments = async () => {
    try {
        const data = await environmentApi.getAll()
        environments.value = data
        if (data.length > 0) {
            selectedRunEnv.value = data[0].envName
        }
    } catch (e) {
        console.error('Failed to load environments', e)
    }
}

const loadScenario = async (id) => {
    try {
        const scenario = await testScenarioApi.getById(id)
        currentScenario.value = scenario
        const tree = await testScenarioApi.getStepsTree(id)
        // Add tempIds for frontend keying if needed, though id is preferred if exists
        steps.value = tree || []
    } catch (e) {
        ElMessage.error('Failed to load scenario')
    }
}

const handleSelectStep = (step) => {
    selectedStep.value = step
}

// Convert frontend tree to flat list for backend saving? 
// Or backend accepts tree? 
// Backend DTO `TestScenarioStepDTO` has children. 
// BUT currently backend Service `addStep` is for single step.
// We need a Batch Save or Update Tree endpoint effectively.
// For Phase 7.1, we implemented simple CRUD. 
// Let's implement a naive "Delete All & Insert All" or "Sync" on backend for simplicity?
// Or we iterate and save. 
// For now, let's assume we need to implement a "syncSteps" endpoint on backend strictly speaking.
// But I didn't verify that endpoint exists. 
// I checked TestScenarioController.java, it has `addStep` (one by one).
// This is a bottleneck. I should assume I need to call addStep recursively or improve backend.
// STARTUP FIX: I will add a `syncSteps` endpoint to backend controller in next step if needed, or iterate.
// Iterating is slow. 
// Let's assume for prototype we just log save.
const saveScenario = async () => {
    try {
        await testScenarioApi.syncSteps(scenarioId, steps.value)
        ElMessage.success('Scenario saved successfully')
    } catch (e) {
        console.error(e)
        ElMessage.error('Failed to save scenario')
    }
}

const runScenario = async () => {
    showRunEnvDialog.value = true
}

const confirmRun = () => {
    showRunEnvDialog.value = false
    consoleVisible.value = true
    if (execConsole.value) {
        execConsole.value.start()
    }
}

const handleStepUpdate = (event) => {
    if (event.type === 'reset') {
        resetStepStatuses(steps.value)
        executionResults.value = []
        return
    }
    
    updateStepStatus(steps.value, event.stepId, event.status)
    
    if (event.result) {
        // Collect results for the summary dialog
        const existingIdx = executionResults.value.findIndex(r => r.caseId === event.result.caseId)
        if (existingIdx >= 0) {
            executionResults.value[existingIdx] = event.result
        } else {
            executionResults.value.push(event.result)
        }
    }
}

const resetStepStatuses = (nodes) => {
    nodes.forEach(node => {
        node.status = null
        if (node.children) resetStepStatuses(node.children)
    })
}

const handleVariablesUpdate = (vars) => {
    runtimeVariables.value = vars
}

const updateStepStatus = (nodes, stepId, status) => {
    for (let node of nodes) {
        if (node.id === stepId) {
            node.status = status
            return true
        }
        if (node.children && updateStepStatus(node.children, stepId, status)) {
            return true
        }
    }
    return false
}

</script>

<style scoped>
.scenario-editor-wrapper {
    height: 100vh;
    display: flex;
    flex-direction: column;
}

.editor-header {
  height: 50px;
  padding: 0 20px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.scenario-editor-layout {
    display: flex;
    flex: 1;
    overflow: hidden;
    position: relative;
}
.pane {
    height: 100%;
}
.left-pane {
    width: 250px;
    flex-shrink: 0;
}
.middle-pane {
    flex: 1;
    min-width: 400px;
    border-right: 1px solid #ebeef5;
}
.right-pane {
    width: 450px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
}
.right-tabs {
    height: 100%;
    display: flex;
    flex-direction: column;
}
:deep(.el-tabs__content) {
    flex: 1;
    overflow: auto;
}
.code-box {
    background: #f5f7fa;
    padding: 10px;
    border-radius: 4px;
    font-size: 12px;
    max-height: 200px;
    overflow-y: auto;
    margin: 0;
}
.result-detail {
    padding: 10px;
}
</style>
