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
               <h4>Available Test Cases ({{ caseTotal }})</h4>
               <el-input 
                 v-model="caseSearch" 
                 placeholder="Search cases..." 
                 prefix-icon="Search" 
                 clearable
                 size="default"
                 @input="handleCaseSearch"
                 @clear="handleCaseSearch"
               />
             </div>
             <div class="case-list-container" v-loading="loadingCases">
               <div v-if="allProjectCases.length === 0 && !loadingCases" class="empty-msg">
                 No cases found. Try adjusting your search or select a different project.
               </div>
               <div 
                  v-for="c in allProjectCases" 
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
             <div class="case-pagination" v-if="caseTotal > 0">
               <el-pagination
                 v-model:current-page="casePage"
                 v-model:page-size="casePageSize"
                 :page-sizes="[10, 20, 50, 100]"
                 :total="caseTotal"
                 layout="total, sizes, prev, pager, next"
                 @size-change="handleCasePageSizeChange"
                 @current-change="handleCasePageChange"
                 small
               />
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
                  class="case-item-wrapper"
                >
                  <div class="case-item selected">
                   <span class="index-badge">{{ index + 1 }}</span>
                   <div class="case-info">
                      <span class="case-name">{{ c.caseName }}</span>
                      <div class="case-badges">
                        <el-tag v-if="c.steps && c.steps.length > 0" size="small" type="info" effect="plain">
                          {{ c.steps.length }} Steps
                        </el-tag>
                        <el-tag v-if="c.parameterOverrides" size="small" type="warning" effect="plain" :title="formatParamOverrides(c.parameterOverrides)">
                          Params: {{ formatParamOverrides(c.parameterOverrides) }}
                        </el-tag>
                      </div>
                   </div>
                   <div class="actions">
                      <el-button link type="info" size="small" @click="toggleCaseDetails(index)" title="View Details">
                        <el-icon><component :is="expandedCaseIndex === index ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
                      </el-button>
                      <el-button link type="success" size="small" @click="editCase(c, index)" title="Edit Case">
                        <el-icon><EditPen /></el-icon> Edit
                      </el-button>
                      <el-divider direction="vertical" />
                      <el-button link size="small" @click="moveUp(index)" :disabled="index === 0"><el-icon><ArrowUp /></el-icon></el-button>
                      <el-button link size="small" @click="moveDown(index)" :disabled="index === selectedCases.length - 1"><el-icon><ArrowDown /></el-icon></el-button>
                      <el-button link type="danger" size="small" @click="removeFromPlan(index)"><el-icon><Close /></el-icon></el-button>
                   </div>
                  </div>
                  
                  <!-- Expanded Details -->
                  <el-collapse-transition>
                    <div v-if="expandedCaseIndex === index" class="case-details-panel">
                      <div class="detail-section">
                        <strong>Request:</strong>
                        <el-tag type="success" size="small" style="margin-left: 8px">{{ c.method }}</el-tag>
                        <span style="margin-left: 8px; color: #606266">{{ c.url }}</span>
                      </div>
                      
                      <div v-if="c.parameterOverrides" class="detail-section">
                        <strong>Parameter Overrides:</strong>
                        <div style="margin-top: 8px;">
                          <el-tag 
                            v-for="(value, key) in parseParamOverrides(c.parameterOverrides)" 
                            :key="key" 
                            size="small" 
                            type="warning" 
                            effect="plain"
                            style="margin-right: 8px; margin-bottom: 4px;"
                          >
                            {{ key }} = {{ value }}
                          </el-tag>
                        </div>
                      </div>
                      
                      <div v-if="c.steps && c.steps.length > 0" class="detail-section">
                        <strong>Steps ({{ c.steps.length }}):</strong>
                        <div class="step-mini-list">
                          <div v-for="(step, idx) in c.steps" :key="step.id" class="step-mini-item">
                            <el-tag size="small" :type="step.method === 'GET' ? 'success' : 'primary'">{{ step.method }}</el-tag>
                            <span class="step-mini-name">{{ step.stepName || 'Step ' + (idx + 1) }}</span>
                          </div>
                        </div>
                      </div>
                      
                      <div v-if="c.assertionScript" class="detail-section">
                        <strong>Global Assertion:</strong>
                        <el-tag type="success" size="small" style="margin-left: 8px">Configured</el-tag>
                      </div>
                    </div>
                  </el-collapse-transition>
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

    <!-- Case Detail Drawer -->
    <case-detail-drawer
      v-model="showCaseDrawer"
      :case-id="editingCaseId"
      :plan-id="currentPlan?.id"
      :available-variables="getCurrentAvailableVariables()"
      @saved="handleCaseSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { testPlanApi } from '../api/testPlan'
import { projectApi } from '../api/project'
import { testModuleApi } from '../api/testModule'
import { testCaseApi } from '../api/testCase'
import { environmentApi } from '../api/environment'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowUp, ArrowDown, Plus, Delete, EditPen, Setting, Close, VideoPlay } from '@element-plus/icons-vue'
import CaseDetailDrawer from '../components/CaseDetailDrawer.vue'

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
const casePage = ref(1)
const casePageSize = ref(20)
const caseTotal = ref(0)
const loadingCases = ref(false)

// Execution State
const runDialogVisible = ref(false)
const runEnv = ref('')
const executing = ref(false)
const currentRunPlan = ref(null)
const resultDialogVisible = ref(false)
const executionResults = ref([])

// Case Drawer State
const editingCaseId = ref(null)
const editingCaseIndex = ref(-1)
const showCaseDrawer = ref(false)
const expandedCaseIndex = ref(-1)
const planVariables = ref({}) // Store discovered variables per step index

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

const loadProjectCases = async (projectId, resetPage = true) => {
    if (!projectId) {
        allProjectCases.value = []
        caseTotal.value = 0
        return
    }
    
    if (resetPage) {
        casePage.value = 1
    }
    
    loadingCases.value = true
    try {
        const page = casePage.value - 1 // Backend uses 0-based page
        const keyword = caseSearch.value.trim()
        const response = await testCaseApi.getByProject(projectId, page, casePageSize.value, keyword)
        
        allProjectCases.value = response.cases || []
        caseTotal.value = response.total || 0
    } catch (e) {
        console.error(e)
        ElMessage.error('Failed to load project cases')
        allProjectCases.value = []
        caseTotal.value = 0
    } finally {
        loadingCases.value = false
    }
}

const handleCaseSearch = () => {
    // Reset to first page when searching
    casePage.value = 1
    if (currentPlan.value.projectId) {
        loadProjectCases(currentPlan.value.projectId, false)
    }
}

const handleCasePageChange = (page) => {
    if (currentPlan.value.projectId) {
        loadProjectCases(currentPlan.value.projectId, false)
    }
}

const handleCasePageSizeChange = (size) => {
    casePage.value = 1 // Reset to first page when changing page size
    if (currentPlan.value.projectId) {
        loadProjectCases(currentPlan.value.projectId, false)
    }
}

const toggleCaseDetails = (index) => {
    expandedCaseIndex.value = expandedCaseIndex.value === index ? -1 : index
}

const editCase = async (c, index) => {
    editingCaseId.value = c.id
    editingCaseIndex.value = index
    
    // Analyze variables if editing a plan
    if (currentPlan.value && currentPlan.value.id) {
        await analyzePlanVariables()
    }
    
    showCaseDrawer.value = true
}

const getCurrentAvailableVariables = () => {
    if (editingCaseIndex.value < 0) return []
    const context = planVariables.value[editingCaseIndex.value]
    return context ? context.availableVariables : []
}

const analyzePlanVariables = async () => {
    if (!currentPlan.value.id) return
    try {
        const analysis = await testPlanApi.analyzeVariables(currentPlan.value.id)
        planVariables.value = analysis
        planVariables.value = analysis || []
    } catch (e) {
        console.error('Failed to analyze variables:', e)
    }
}


const handleCaseSaved = async (updatedCase) => {
    // If we are editing an existing plan, refresh the whole plan to get correct mapped overrides
    if (currentPlan.value && currentPlan.value.id) {
        try {
            const plan = await testPlanApi.getById(currentPlan.value.id)
            selectedCases.value = plan.testCases ? plan.testCases.map(c => ({
                ...c,
                parameterOverrides: c.parameterOverrides || ''
            })) : []
            ElMessage.success('Plan overrides updated')
        } catch (e) {
            console.error('Failed to refresh plan after case save:', e)
            // Fallback: update matching case locally
            const index = selectedCases.value.findIndex(c => c.id === updatedCase.id)
            if (index !== -1) {
                selectedCases.value[index] = { ...selectedCases.value[index], ...updatedCase }
            }
        }
    } else {
        // Just adding cases to a new (unsaved) plan
        const index = selectedCases.value.findIndex(c => c.id === updatedCase.id)
        if (index !== -1) {
            selectedCases.value[index] = { ...selectedCases.value[index], ...updatedCase }
        }
    }
}

// Search is now handled by backend, so we don't need client-side filtering
// Keeping this for backward compatibility but it's not used anymore
const filteredAvailableCases = computed(() => {
    return allProjectCases.value
})

// Format parameter overrides for display
const formatParamOverrides = (paramOverrides) => {
    if (!paramOverrides) return ''
    try {
        const obj = JSON.parse(paramOverrides)
        const keys = Object.keys(obj)
        if (keys.length === 0) return ''
        if (keys.length <= 2) {
            return keys.map(k => `${k}=${obj[k]}`).join(', ')
        }
        return `${keys.length} params`
    } catch (e) {
        return 'Invalid JSON'
    }
}

// Parse parameter overrides for display
const parseParamOverrides = (paramOverrides) => {
    if (!paramOverrides) return {}
    try {
        return JSON.parse(paramOverrides)
    } catch (e) {
        return {}
    }
}

const addToPlan = (c) => {
    // Allow adding the same case multiple times
    // Each instance can have different parameter overrides
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

/* View Controls */
.view-controls {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}

/* Card Grid Layout */
.plan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
  padding: 10px 0;
}

.plan-card {
  transition: all 0.3s ease;
  border-radius: 12px;
  overflow: hidden;
}

.plan-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.plan-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.plan-card .card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.plan-card .more-icon {
  cursor: pointer;
  font-size: 20px;
  color: #909399;
  transition: color 0.2s;
}

.plan-card .more-icon:hover {
  color: #409eff;
}

.plan-info {
  padding: 15px 0;
  min-height: 100px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #606266;
  font-size: 14px;
}

.info-item .el-icon {
  font-size: 18px;
}

.plan-description {
  margin-top: 12px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.plan-actions {
  display: flex;
  gap: 10px;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
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
  min-height: 200px;
}

.case-pagination {
  padding: 10px;
  border-top: 1px solid #ebeef5;
  background-color: #fafafa;
  display: flex;
  justify-content: center;
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

/* New styles for expanded case details */
.case-item-wrapper {
  margin-bottom: 8px;
}

.case-badges {
  display: flex;
  gap: 5px;
  margin-top: 4px;
}

.case-details-panel {
  padding: 15px;
  background-color: #f9fafb;
  border-left: 3px solid #409eff;
  margin-top: 8px;
  border-radius: 4px;
}

.detail-section {
  margin-bottom: 12px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section strong {
  color: #606266;
  font-size: 13px;
}

.step-mini-list {
  margin-top: 8px;
  max-height: 150px;
  overflow-y: auto;
}

.step-mini-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  background-color: white;
  border-radius: 4px;
  margin-bottom: 4px;
}

.step-mini-name {
  font-size: 12px;
  color: #606266;
}

/* Parameter form styles */
.param-form {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 15px;
  background-color: #fafafa;
}

.param-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.param-header h4 {
  margin: 0;
  color: #303133;
  font-size: 14px;
}

.empty-params {
  padding: 20px;
}

.param-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.param-item {
  display: flex;
  align-items: center;
  padding: 10px;
  background-color: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

/* Wizard Dialog Styles */
.wizard-dialog .wizard-steps {
  margin: 30px 0;
  padding: 0 60px;
}

.wizard-content {
  padding: 20px 60px;
  min-height: 500px;
}

.step-panel {
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.step-title {
  margin-bottom: 30px;
  text-align: center;
}

.step-title h3 {
  margin: 0 0 10px 0;
  font-size: 24px;
  color: #303133;
}

.step-title p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.wizard-form {
  max-width: 600px;
  margin: 0 auto;
}

.wizard-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
}

.wizard-nav {
  display: flex;
  gap: 10px;
}

/* Selection Panel (Step 2) */
.project-required-msg {
  padding: 60px;
}

.selection-panel {
  min-height: 500px;
}

.selection-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.selection-stats {
  display: flex;
  gap: 10px;
}

.selection-grid {
  display: grid;
  gap: 20px;
}

.available-cases h4 {
  margin: 0 0 15px 0;
  color: #303133;
  font-size: 16px;
}

.case-checkbox-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
  max-height: 450px;
  overflow-y: auto;
  padding-right: 10px;
}

.case-checkbox-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background-color: #fff;
  transition: all 0.2s;
  cursor: pointer;
}

.case-checkbox-item:hover {
  background-color: #f5f7fa;
  border-color: #409eff;
}

.case-checkbox-item.is-selected {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.case-checkbox-content {
  flex: 1;
  overflow: hidden;
}

.case-checkbox-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
}

.case-checkbox-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.case-checkbox-meta .case-url {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.case-pagination-wizard {
  margin-top: 15px;
  display: flex;
  justify-content: center;
}

.empty-selection {
  padding: 60px;
  text-align: center;
}

/* Order Config Panel (Step 3) */
.order-config-panel {
  max-width: 900px;
  margin: 0 auto;
}

.order-case-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  margin-bottom: 12px;
  background-color: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  transition: all 0.2s;
}

.order-case-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.order-item-left {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
}

.drag-handle {
  cursor: move;
  font-size: 20px;
  color: #909399;
}

.order-badge {
  min-width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.order-case-info {
  flex: 1;
}

.order-case-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.order-case-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #606266;
}

.order-item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Review Panel (Step 4) */
.review-descriptions {
  margin-bottom: 30px;
}

.review-timeline h4 {
  margin: 20px 0 15px 0;
  color: #303133;
}

.timeline-case {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.timeline-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
  font-size: 12px;
}

.param-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #e6a23c;
}

</style>
