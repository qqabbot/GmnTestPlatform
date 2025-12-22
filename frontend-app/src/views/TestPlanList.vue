<template>
  <div class="test-plan-list">
    <div class="header">
      <h2>Test Plans</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon> New Plan
      </el-button>
    </div>

    <!-- Filter -->
    <div class="filter-bar">
      <el-select v-model="filterProjectId" placeholder="Filter by Project" clearable @change="loadPlans">
        <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
      </el-select>
    </div>

    <!-- Plan Table -->
    <el-table :data="plans" style="width: 100%" v-loading="loading">
      <el-table-column label="ID" width="80">
        <template #default="scope">
          {{ scope.row.id }}
        </template>
      </el-table-column>
      <el-table-column prop="name" label="Plan Name" min-width="200" />
      <el-table-column prop="project.projectName" label="Project" width="150" />
      <el-table-column label="Cases" width="100">
        <template #default="scope">
          {{ scope.row.testCases ? scope.row.testCases.length : 0 }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="250" fixed="right">
        <template #default="scope">
          <el-button type="success" size="small" @click="openRunDialog(scope.row)">
            <el-icon><VideoPlay /></el-icon> Run
          </el-button>
          <el-button type="primary" size="small" @click="openEditDialog(scope.row)">Edit</el-button>
          <el-button type="danger" size="small" @click="deletePlan(scope.row)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Fullscreen Dialog -->
    <el-dialog 
      v-model="drawerVisible" 
      :title="isEdit ? 'Edit Test Plan' : 'New Test Plan'" 
      fullscreen
      class="fullscreen-dialog"
      :close-on-click-modal="false"
    >
      <div class="edit-container">
        <!-- Main Form (Top Area) -->
        <div class="plan-form-header">
          <el-form :inline="true" label-width="100px" size="default">
            <el-form-item label="Plan Name" required>
              <el-input v-model="currentPlan.name" placeholder="Enter plan name" style="width: 300px" />
            </el-form-item>
            <el-form-item label="Project" required>
              <el-select v-model="currentPlan.projectId" placeholder="Select Project" @change="handleProjectChange" style="width: 200px">
                 <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="Description">
               <el-input v-model="currentPlan.description" placeholder="Optional description" style="width: 400px" />
            </el-form-item>
          </el-form>
        </div>
        
        <!-- Case Manager (Main Content Area) -->
        <div class="case-manager-fullscreen" v-if="currentPlan.projectId">
           <!-- Left: Available Cases -->
           <div class="case-list-panel flex-1">
             <div class="panel-header">
               <h4>Available Test Cases</h4>
               <el-input 
                 v-model="caseSearch" 
                 placeholder="Search cases..." 
                 prefix-icon="Search" 
                 clearable
                 size="default" 
               />
             </div>
             <div class="case-list-container">
               <div 
                  v-for="c in filteredAvailableCases" 
                  :key="c.id" 
                  class="case-item available"
                  @click="addToPlan(c)"
               >
                  <div class="case-info">
                    <span class="case-name">{{ c.caseName }}</span>
                    <span class="case-meta">{{ c.method }} {{ c.url }}</span>
                  </div>
                  <el-button type="primary" link icon="Plus">Add</el-button>
               </div>
             </div>
           </div>
           
           <!-- Right: Selected Cases -->
           <div class="case-list-panel flex-1 selected-panel">
             <div class="panel-header">
               <h4>Selected Cases Sequence ({{ selectedCases.length }})</h4>
               <div class="panel-tips">Drag to reorder or use arrows</div>
             </div>
             <div class="case-list-container">
                <div v-if="selectedCases.length === 0" class="empty-msg">No cases selected for this plan.</div>
                <div 
                  v-for="(c, index) in selectedCases" 
                  :key="c.id + '_' + index" 
                  class="case-item selected"
                >
                   <span class="index-badge">{{ index + 1 }}</span>
                   <div class="case-info">
                      <span class="case-name">{{ c.caseName }}</span>
                      <span class="case-meta" v-if="c.parameterOverrides">
                        <el-tag size="small" type="warning" effect="plain">Overrides Set</el-tag>
                      </span>
                   </div>
                   <div class="actions">
                      <el-button link type="primary" size="small" @click="configureParams(c)" title="Parameter Overrides">
                        <el-icon><Setting /></el-icon> Params
                      </el-button>
                      <el-divider direction="vertical" />
                      <el-button link size="small" @click="moveUp(index)" :disabled="index === 0"><el-icon><ArrowUp /></el-icon></el-button>
                      <el-button link size="small" @click="moveDown(index)" :disabled="index === selectedCases.length - 1"><el-icon><ArrowDown /></el-icon></el-button>
                      <el-button link type="danger" size="small" @click="removeFromPlan(index)"><el-icon><Close /></el-icon></el-button>
                   </div>
                </div>
             </div>
           </div>
        </div>
        <div v-else class="empty-layout">
          <el-empty description="Please select a project to manage test cases" />
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="drawerVisible = false" size="large">Discard Changes</el-button>
          <el-button type="primary" @click="savePlan" size="large">Save Plan</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Parameter Config Dialog -->
    <el-dialog v-model="paramDialogVisible" title="Parameter Overrides" width="500px">
      <div style="margin-bottom: 10px;">
        <el-alert title="Enter JSON to override variables for this step." type="info" :closable="false" />
      </div>
      <el-input 
        v-model="currentParamOverride" 
        type="textarea" 
        :rows="10" 
        placeholder='{ "key": "value" }'
      />
      <template #footer>
        <el-button @click="paramDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="saveParams">Confirm</el-button>
      </template>
    </el-dialog>

    <!-- Run Dialog -->
    <el-dialog v-model="runDialogVisible" title="Run Test Plan" width="400px">
      <el-form>
        <el-form-item label="Environment">
          <el-select v-model="runEnv" placeholder="Select Environment" style="width: 100%">
            <el-option v-for="env in environments" :key="env.id" :label="env.envName" :value="env.envName" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="runDialogVisible = false">Cancel</el-button>
        <el-button type="success" @click="executePlan" :loading="executing">Execute</el-button>
      </template>
    </el-dialog>
    
    <!-- Execution Result Dialog (Simple List) -->
    <el-dialog v-model="resultDialogVisible" title="Execution Results" width="70%">
       <el-table :data="executionResults" height="400">
          <el-table-column prop="caseId" label="Case ID" width="80" />
          <el-table-column prop="caseName" label="Case Name" />
          <el-table-column prop="status" label="Status" width="100">
             <template #default="scope">
                <el-tag :type="scope.row.status === 'PASS' ? 'success' : 'danger'">{{ scope.row.status }}</el-tag>
             </template>
          </el-table-column>
          <el-table-column prop="duration" label="Time (ms)" width="100" />
          <el-table-column prop="detail" label="Details" show-overflow-tooltip />
       </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { testPlanApi } from '../api/testPlan'
import { projectApi } from '../api/project'
import { testModuleApi } from '../api/testModule' // Need raw test cases? No, we need TestCaseApi.
import { testCaseApi } from '../api/testCase'
import { environmentApi } from '../api/environment'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const plans = ref([])
const projects = ref([])
const environments = ref([])
const filterProjectId = ref(null)

// Drawer State
const drawerVisible = ref(false)
const isEdit = ref(false)
const currentPlan = ref({
    name: '',
    description: '',
    projectId: null
})

// Case Management State
const allProjectCases = ref([])
const selectedCases = ref([])
const caseSearch = ref('')

// Execution State
const runDialogVisible = ref(false)
const runEnv = ref('')
const executing = ref(false)
const currentRunPlan = ref(null)
const resultDialogVisible = ref(false)
const executionResults = ref([])

// Parameter Config State
const paramDialogVisible = ref(false)
const currentParamOverride = ref('')
const currentEditingCase = ref(null)

onMounted(async () => {
    loadData()
})

const loadData = async () => {
    loading.value = true
    try {
        const [pData, envData] = await Promise.all([
            projectApi.getAll(),
            environmentApi.getAll()
        ])
        projects.value = pData
        environments.value = envData
        if(envData.length > 0) runEnv.value = envData[0].envName
        
        await loadPlans()
    } catch (e) {
        ElMessage.error('Failed to load initial data')
    } finally {
        loading.value = false
    }
}

const loadPlans = async () => {
    try {
        plans.value = await testPlanApi.getAll({ projectId: filterProjectId.value })
    } catch (e) {
        console.error(e)
    }
}

const openCreateDialog = () => {
    isEdit.value = false
    currentPlan.value = { name: '', description: '', projectId: null }
    selectedCases.value = []
    allProjectCases.value = []
    drawerVisible.value = true
}

const openEditDialog = async (plan) => {
    isEdit.value = true
    // Deep copy to avoid mutating list directly
    currentPlan.value = {
        id: plan.id,
        name: plan.name,
        description: plan.description,
        projectId: plan.project ? plan.project.id : null
    }
    
    // Load cases for this project
    if (currentPlan.value.projectId) {
        await loadProjectCases(currentPlan.value.projectId)
    }
    
    // Load selected cases from plan (plan.testCases is available from list API? Yes, EAGER/default)
    // Ensure parameterOverrides is preserved
    selectedCases.value = plan.testCases ? plan.testCases.map(c => ({
        ...c,
        parameterOverrides: c.parameterOverrides || ''
    })) : []
    
    drawerVisible.value = true
}

const handleProjectChange = async (newVal) => {
    selectedCases.value = [] // Clear selection on project change
    if (newVal) {
        await loadProjectCases(newVal)
    } else {
        allProjectCases.value = []
    }
}

const loadProjectCases = async (projectId) => {
    try {
        const modules = await testModuleApi.getAll({ projectId })
        let cases = []
        for (const m of modules) {
             const modCases = await testCaseApi.getAll(m.id)
             cases = cases.concat(modCases)
        }
        allProjectCases.value = cases
    } catch (e) {
        console.error(e)
        ElMessage.error('Failed to load project cases')
    }
}

const configureParams = (c) => {
    currentEditingCase.value = c
    // beautify json if possible
    try {
        if (c.parameterOverrides) {
             const obj = JSON.parse(c.parameterOverrides)
             currentParamOverride.value = JSON.stringify(obj, null, 2)
        } else {
             currentParamOverride.value = ''
        }
    } catch (e) {
        currentParamOverride.value = c.parameterOverrides || ''
    }
    paramDialogVisible.value = true
}

const saveParams = () => {
    // Validate JSON
    if (currentParamOverride.value.trim()) {
        try {
            JSON.parse(currentParamOverride.value)
        } catch (e) {
             ElMessage.error('Invalid JSON format')
             return
        }
    }
    
    // Save compressed JSON string
    if (currentEditingCase.value) {
        currentEditingCase.value.parameterOverrides = currentParamOverride.value
    }
    paramDialogVisible.value = false
}

const filteredAvailableCases = computed(() => {
    if (!caseSearch.value) return allProjectCases.value
    return allProjectCases.value.filter(c => c.caseName.toLowerCase().includes(caseSearch.value.toLowerCase()))
})

const addToPlan = (c) => {
    selectedCases.value.push({
        ...c,
        parameterOverrides: ''
    })
}

const removeFromPlan = (index) => {
    selectedCases.value.splice(index, 1)
}

const moveUp = (index) => {
    if (index > 0) {
        const temp = selectedCases.value[index]
        selectedCases.value[index] = selectedCases.value[index - 1]
        selectedCases.value[index - 1] = temp
    }
}

const moveDown = (index) => {
    if (index < selectedCases.value.length - 1) {
        const temp = selectedCases.value[index]
        selectedCases.value[index] = selectedCases.value[index + 1]
        selectedCases.value[index + 1] = temp
    }
}

const savePlan = async () => {
    if (!currentPlan.value.name || !currentPlan.value.projectId) {
        ElMessage.warning('Name and Project are required')
        return
    }
    
    // Construct payload
    const payload = {
        ...currentPlan.value,
        project: { id: currentPlan.value.projectId },
        testCases: selectedCases.value.map(c => ({ 
            id: c.id,
            parameterOverrides: c.parameterOverrides // Send overrides
        }))
    }
    
    try {
        if (isEdit.value) {
            await testPlanApi.update(payload.id, payload)
            ElMessage.success('Plan updated')
        } else {
            await testPlanApi.create(payload)
            ElMessage.success('Plan created')
        }
        drawerVisible.value = false
        loadPlans()
    } catch (e) {
        ElMessage.error('Failed to save plan')
    }
}

const deletePlan = async (row) => {
    try {
        await ElMessageBox.confirm('Are you sure to delete this plan?', 'Warning', { type: 'warning' })
        await testPlanApi.delete(row.id)
        ElMessage.success('Deleted')
        loadPlans()
    } catch (e) {
        // cancel
    }
}

const openRunDialog = (row) => {
    currentRunPlan.value = row
    runDialogVisible.value = true
}

const executePlan = async () => {
    executing.value = true
    try {
        const results = await testPlanApi.execute(currentRunPlan.value.id, runEnv.value)
        executionResults.value = results
        runDialogVisible.value = false
        resultDialogVisible.value = true
    } catch (e) {
        ElMessage.error('Execution Failed: ' + e.message)
    } finally {
        executing.value = false
    }
}
</script>

<style scoped>
.test-plan-list {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.filter-bar {
  margin-bottom: 20px;
}
.edit-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.plan-form-header {
  padding: 15px 20px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
  margin-bottom: 0px;
}
.case-manager-fullscreen {
  display: flex;
  gap: 20px;
  flex: 1;
  padding: 20px;
  overflow: hidden; /* Prevent body scroll */
}
.flex-1 {
  flex: 1;
}
.case-list-panel {
  display: flex;
  flex-direction: column;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
  overflow: hidden;
}
.panel-header {
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
  background-color: #fafafa;
}
.panel-header h4 {
  margin: 0 0 10px 0;
  color: #303133;
}
.panel-tips {
  font-size: 12px;
  color: #909399;
}
.case-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}
.case-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
  transition: all 0.2s;
}
.case-item:hover {
  background-color: #f0f7ff;
}
.case-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}
.case-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}
.case-meta {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.actions {
  display: flex;
  align-items: center;
  gap: 5px;
}
.index-badge {
  background-color: #409eff;
  color: white;
  min-width: 24px;
  height: 24px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  margin-right: 15px;
}
.selected-panel {
  border-left: 4px solid #409eff;
}
.empty-layout {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dialog-footer {
  padding: 10px 20px;
  border-top: 1px solid #dcdfe6;
  text-align: right;
}
</style>
