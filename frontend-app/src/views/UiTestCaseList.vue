<template>
  <div class="ui-test-case-list">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>UI Test Cases</span>
          <el-button type="primary" @click="handleNewCase">
            <el-icon><Plus /></el-icon>
            New UI Case
          </el-button>
        </div>
      </template>

      <!-- Filter Section -->
      <div class="filter-section">
        <el-select v-model="selectedProjectId" placeholder="Select Project" style="width: 200px; margin-right: 10px" clearable @change="onProjectChange">
          <el-option v-for="proj in projects" :key="proj.id" :label="proj.projectName" :value="proj.id" />
        </el-select>
        <el-select v-model="selectedModuleId" placeholder="Select Module" style="width: 200px; margin-right: 10px" clearable @change="loadCases">
          <el-option v-for="mod in filteredModules" :key="mod.id" :label="mod.moduleName" :value="mod.id" />
        </el-select>
        <el-input
          v-model="searchText"
          placeholder="Search UI cases..."
          style="width: 300px"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- Table -->
      <el-table
        :data="filteredCases"
        style="width: 100%; margin-top: 20px"
        v-loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="Case Name" min-width="200" />
        <el-table-column prop="startUrl" label="Start URL" min-width="250" show-overflow-tooltip />
        <el-table-column label="Viewport" width="150">
          <template #default="{ row }">
            {{ row.viewportWidth }} x {{ row.viewportHeight }}
          </template>
        </el-table-column>
        <el-table-column label="Mode" width="120">
          <template #default="{ row }">
            <el-tag :type="row.headless === false ? 'success' : 'info'">
              {{ row.headless === false ? 'Headed' : 'Headless' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="success"
              size="small"
              text
              @click="executeCase(row.id)"
            >
              <el-icon><VideoPlay /></el-icon>
            </el-button>
            <el-button
              type="primary"
              size="small"
              text
              @click="router.push(`/ui-testing/cases/${row.id}/edit`)"
            >
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button
              type="danger"
              size="small"
              text
              @click="deleteCase(row.id)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Delete, VideoPlay } from '@element-plus/icons-vue'
import { uiTestApi } from '../api/uiTest'
import { testModuleApi } from '../api/testModule'
import { projectApi } from '../api/project'

const router = useRouter()

const loading = ref(false)
const searchText = ref('')
const selectedProjectId = ref(null)
const selectedModuleId = ref(null)
const cases = ref([])
const modules = ref([])
const projects = ref([])

const loadProjects = async () => {
  try {
    projects.value = await projectApi.getAll()
  } catch (error) {
    ElMessage.error('Failed to load projects')
  }
}

const loadModules = async () => {
  try {
    modules.value = await testModuleApi.getAll()
  } catch (error) {
    ElMessage.error('Failed to load modules')
  }
}

const filteredModules = computed(() => {
  if (!selectedProjectId.value) return modules.value
  return modules.value.filter(m => m.project && m.project.id === selectedProjectId.value)
})

const onProjectChange = () => {
  selectedModuleId.value = null
  loadCases()
}

const loadCases = async () => {
  loading.value = true
  try {
    let res
    if (selectedModuleId.value) {
      res = await uiTestApi.getCasesByModule(selectedModuleId.value)
    } else if (selectedProjectId.value) {
      res = await uiTestApi.getCasesByProject(selectedProjectId.value)
    } else {
      res = await uiTestApi.getAllCases()
    }
    cases.value = res
  } catch (error) {
    ElMessage.error('Failed to load UI test cases')
  } finally {
    loading.value = false
  }
}

const filteredCases = computed(() => {
  if (!searchText.value) return cases.value
  return cases.value.filter(c =>
    c.name.toLowerCase().includes(searchText.value.toLowerCase())
  )
})

const handleNewCase = () => {
  router.push('/ui-testing/cases/new')
}

const executeCase = async (id) => {
  try {
    ElMessage.info('Starting execution...')
    const record = await uiTestApi.executeCase(id)
    if (record.status === 'FAILURE') {
        ElMessageBox.alert(record.errorMessage || 'Unknown error', 'Execution Failed', {
            confirmButtonText: 'OK',
            type: 'error'
        })
    } else {
        ElMessage.success('Execution successful')
        router.push(`/ui-testing/reports/${record.id}`)
    }
  } catch (error) {
    ElMessage.error('Failed to execute UI test case: ' + (error.message || 'Server error'))
  }
}

const deleteCase = async (id) => {
  try {
    await ElMessageBox.confirm('Are you sure to delete this UI test case?', 'Warning', {
      type: 'warning'
    })
    await uiTestApi.deleteCase(id)
    ElMessage.success('UI test case deleted')
    loadCases()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to delete UI test case')
    }
  }
}

onMounted(() => {
  loadProjects()
  loadModules()
  loadCases()
})
</script>

<style scoped>
.ui-test-case-list {
  width: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filter-section {
  margin-bottom: 20px;
}
</style>
