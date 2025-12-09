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
      <el-table-column prop="id" label="ID" width="80" />
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

    <!-- Create/Edit Drawer -->
    <el-drawer v-model="drawerVisible" :title="isEdit ? 'Edit Test Plan' : 'New Test Plan'" size="60%">
      <div class="drawer-content">
        <el-form label-width="100px">
          <el-form-item label="Name" required>
            <el-input v-model="currentPlan.name" />
          </el-form-item>
          <el-form-item label="Description">
             <el-input type="textarea" v-model="currentPlan.description" />
          </el-form-item>
          <el-form-item label="Project" required>
            <el-select v-model="currentPlan.projectId" placeholder="Select Project" @change="handleProjectChange">
               <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
            </el-select>
          </el-form-item>
          
          <el-divider content-position="left">Test Cases Management</el-divider>
          
          <div class="case-manager" v-if="currentPlan.projectId">
             <!-- Left: Available Cases -->
             <div class="case-list-panel">
               <h4>Available Cases</h4>
               <el-input v-model="caseSearch" placeholder="Search..." prefix-icon="Search" size="small" style="margin-bottom: 5px;" />
               <div class="case-list-container">
                 <div 
                    v-for="c in filteredAvailableCases" 
                    :key="c.id" 
                    class="case-item available"
                    @click="addToPlan(c)"
                 >
                    <span class="case-name">{{ c.caseName }}</span>
                    <el-icon><Plus /></el-icon>
                 </div>
               </div>
             </div>
             
             <!-- Right: Selected Cases -->
             <div class="case-list-panel">
               <h4>Selected Cases (In Order)</h4>
               <div class="case-list-container">
                  <div v-if="selectedCases.length === 0" class="empty-msg">No cases selected</div>
                  <div 
                    v-for="(c, index) in selectedCases" 
                    :key="c.id + '_' + index" 
                    class="case-item selected"
                  >
                     <span class="index-badge">{{ index + 1 }}</span>
                     <span class="case-name">{{ c.caseName }}</span>
                     <div class="actions">
                        <el-button link size="small" @click="moveUp(index)" :disabled="index === 0"><el-icon><ArrowUp /></el-icon></el-button>
                        <el-button link size="small" @click="moveDown(index)" :disabled="index === selectedCases.length - 1"><el-icon><ArrowDown /></el-icon></el-button>
                        <el-button link type="danger" size="small" @click="removeFromPlan(index)"><el-icon><Close /></el-icon></el-button>
                     </div>
                  </div>
               </div>
             </div>
          </div>
          <div v-else class="empty-msg">Please select a project first.</div>
        </el-form>
      </div>
      <template #footer>
        <div style="flex: auto">
          <el-button @click="drawerVisible = false">Cancel</el-button>
          <el-button type="primary" @click="savePlan">Save</el-button>
        </div>
      </template>
    </el-drawer>

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
    selectedCases.value = plan.testCases ? [...plan.testCases] : []
    
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
        // We assume we can fetch all cases by project. 
        // TestCaseApi usually supports filtering by Module. 
        // Need to support Project filtering in Backend or iterate modules here.
        // Backend TestCaseService.executeAllCases logic implies we can find by ProjectId?
        // Let's check TestCaseApi. If not, we might need to fetch modules first.
        // Or create a new endpoint /api/cases?projectId=...
        // For now, let's fetch all cases (if list is small) or fetch modules then cases.
        // Backend `TestCaseController` getAll() doesn't seem to support projectId param filtering explicitly yet?
        // Wait, `TestCaseService.executeAllCases` does.
        // Let's assume for now we can get all cases and filter in frontend or use existing endpoint if any.
        // Actually, let's look at `testCase.js`.
        // It has `getAll(params)`. Backend `TestCaseController.getAll` might not support filtering.
        // But `testModuleApi` can get modules by project. Then get cases by module.
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

const filteredAvailableCases = computed(() => {
    if (!caseSearch.value) return allProjectCases.value
    return allProjectCases.value.filter(c => c.caseName.toLowerCase().includes(caseSearch.value.toLowerCase()))
})

const addToPlan = (c) => {
    // Clone to allow duplicates? Usually test suites might allow running same case twice?
    // Let's allow duplicates for now, or just reference.
    // If I add it, I push to selected.
    selectedCases.value.push(c)
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
        testCases: selectedCases.value.map(c => ({ id: c.id }))
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
.drawer-content {
  padding: 20px;
}
.case-manager {
  display: flex;
  gap: 20px;
  height: 400px;
}
.case-list-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
  background-color: #f9faFC;
}
.case-list-container {
  flex: 1;
  overflow-y: auto;
  margin-top: 10px;
}
.case-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  background: white;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  margin-bottom: 2px;
}
.case-item.available:hover {
  background-color: #e6f7ff;
}
.case-item.selected {
  cursor: default;
}
.index-badge {
  display: inline-block;
  width: 20px;
  text-align: center;
  font-size: 12px;
  color: #909399;
  font-weight: bold;
}
.case-name {
  flex: 1;
  margin-left: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.empty-msg {
  text-align: center;
  color: #909399;
  margin-top: 20px;
}
</style>
