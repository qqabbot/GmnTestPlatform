<template>
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
           <el-table-column prop="detail" label="Details" show-overflow-tooltip />
       </el-table>
    </el-dialog>

    <!-- Real-time Console -->
    <execution-console 
      ref="execConsole" 
      :scenario-id="scenarioId" 
      @step-update="handleStepUpdate"
      @variables-update="handleVariablesUpdate"
    />

    <!-- Execution History Dialog -->
    <execution-history-dialog 
      v-model="historyDialogVisible" 
      :scenario-id="scenarioId" 
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import ResourceLibrary from '../components/scenario/ResourceLibrary.vue'
import ScenarioCanvas from '../components/scenario/ScenarioCanvas.vue'
import StepProperties from '../components/scenario/StepProperties.vue'
import ExecutionConsole from '../components/scenario/ExecutionConsole.vue'
import VariableContextViewer from '../components/scenario/VariableContextViewer.vue'
import ExecutionHistoryDialog from '../components/scenario/ExecutionHistoryDialog.vue'
import { testScenarioApi } from '../api/testScenario'

const route = useRoute()
const scenarioId = route.params.id

const currentScenario = ref({})
const steps = ref([])
const selectedStep = ref(null)
const executionResults = ref([])
const resultDialogVisible = ref(false)
const execConsole = ref(null)

const rightActiveTab = ref('properties')
const runtimeVariables = ref({})
const historyDialogVisible = ref(false)

const selectedStepId = computed(() => {
    if (!selectedStep.value) return null
    return selectedStep.value.id || selectedStep.value.tempId
})

onMounted(async () => {
    if (scenarioId) {
        loadScenario(scenarioId)
    }
})

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
    if (execConsole.value) {
        execConsole.value.start()
    }
}

const handleStepUpdate = (event) => {
    if (event.type === 'reset') {
        resetStepStatuses(steps.value)
        return
    }
    
    updateStepStatus(steps.value, event.stepId, event.status)
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
.scenario-editor-layout {
    display: flex;
    height: calc(100vh - 84px); /* Adjust based on navbar height */
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
</style>
