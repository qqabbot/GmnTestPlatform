<template>
  <div class="test-scenario-list">
    <div class="header">
      <h2>Test Scenarios</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon> New Scenario
      </el-button>
    </div>

    <!-- Filter -->
    <div class="filter-bar">
      <el-select v-model="filterProjectId" placeholder="Filter by Project" clearable @change="loadScenarios">
        <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
      </el-select>
    </div>

    <!-- Scenario Table -->
    <el-table :data="scenarios" style="width: 100%" v-loading="loading">
      <el-table-column label="ID" width="80" prop="id" />
      <el-table-column prop="name" label="Scenario Name" min-width="200" />
      <el-table-column prop="project.projectName" label="Project" width="150" />
      <el-table-column label="Created At" width="180">
          <template #default="scope">
              {{ new Date(scope.row.createdAt).toLocaleString() }}
          </template>
      </el-table-column>
      <el-table-column label="Actions" width="250" fixed="right">
        <template #default="scope">
          <el-button type="success" size="small" @click="openRunDialog(scope.row)">
            <el-icon><VideoPlay /></el-icon> Run
          </el-button>
          <el-button type="info" size="small" @click="showHistory(scope.row)">History</el-button>
          <el-button type="primary" size="small" @click="editScenario(scope.row)">Edit</el-button>
          <el-button type="danger" size="small" @click="deleteScenario(scope.row)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create Dialog -->
    <el-dialog v-model="createDialogVisible" title="New Scenario" width="500px">
      <el-form label-position="top">
        <el-form-item label="Name" required>
          <el-input v-model="newScenario.name" placeholder="Scenario Name" />
        </el-form-item>
        <el-form-item label="Project" required>
             <el-select v-model="newScenario.projectId" placeholder="Select Project" style="width: 100%">
                 <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
             </el-select>
        </el-form-item>
        <el-form-item label="Description">
          <el-input type="textarea" v-model="newScenario.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="createScenario">Create</el-button>
      </template>
    </el-dialog>
    
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
      fixed="true"
      v-model:visible="consoleVisible"
      :scenario-id="selectedScenarioId"
      @step-update="handleStepUpdate"
    />

  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { testScenarioApi } from '../api/testScenario'
import { projectApi } from '../api/project'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, VideoPlay, Timer } from '@element-plus/icons-vue'
import ExecutionHistoryDialog from '../components/scenario/ExecutionHistoryDialog.vue'
import ExecutionConsole from '../components/scenario/ExecutionConsole.vue'

const router = useRouter()
const loading = ref(false)
const scenarios = ref([])
const projects = ref([])
const filterProjectId = ref(null)

const createDialogVisible = ref(false)
const newScenario = ref({ name: '', description: '', projectId: null })

const resultDialogVisible = ref(false)
const executionResults = ref([])

const historyDialogVisible = ref(false)
const selectedScenarioId = ref(null)
const execConsole = ref(null)
const consoleVisible = ref(false)

onMounted(async () => {
    loadProjects()
    loadScenarios()
})

const loadProjects = async () => {
    try {
        projects.value = await projectApi.getAll()
    } catch (e) {
        console.error(e)
    }
}

const loadScenarios = async () => {
    loading.value = true
    try {
        if (filterProjectId.value) {
            scenarios.value = await testScenarioApi.getAll({ projectId: filterProjectId.value })
        } else {
            scenarios.value = await testScenarioApi.getAll()
        }
    } catch (e) {
        console.error(e)
        ElMessage.error('Failed to load scenarios')
    } finally {
        loading.value = false
    }
}

const openCreateDialog = () => {
    newScenario.value = { name: '', description: '', projectId: null }
    createDialogVisible.value = true
}

const createScenario = async () => {
    if (!newScenario.value.name || !newScenario.value.projectId) {
        ElMessage.warning('Name and Project are required')
        return
    }
    
    try {
        const created = await testScenarioApi.create(newScenario.value)
        ElMessage.success('Created successfully')
        createDialogVisible.value = false
        // Navigate to editor
        router.push(`/testing/scenarios/${created.id}`)
    } catch (e) {
        ElMessage.error('Creation failed')
    }
}

const editScenario = (row) => {
    router.push(`/testing/scenarios/${row.id}`)
}

const deleteScenario = async (row) => {
     try {
        await ElMessageBox.confirm('Are you sure to delete this scenario?', 'Warning', { type: 'warning' })
        await testScenarioApi.delete(row.id)
        ElMessage.success('Deleted')
        loadScenarios()
    } catch (e) {
        // cancel
    }
}

const openRunDialog = (row) => {
    selectedScenarioId.value = row.id
    consoleVisible.value = true
    nextTick(() => {
        if (execConsole.value) {
            execConsole.value.start()
        }
    })
}

const handleStepUpdate = (event) => {
    if (event.type === 'reset') {
        executionResults.value = []
        return
    }
    
    if (event.result) {
        const existingIdx = executionResults.value.findIndex(r => r.caseId === event.result.caseId)
        if (existingIdx >= 0) {
            executionResults.value[existingIdx] = event.result
        } else {
            executionResults.value.push(event.result)
        }
    }
    
    // Auto show results dialog when complete
    if (event.type === 'scenario_complete') {
        // Optional: resultDialogVisible.value = true
    }
}

const showHistory = (row) => {
    selectedScenarioId.value = row.id
    historyDialogVisible.value = true
}
</script>

<style scoped>
.test-scenario-list {
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
</style>
