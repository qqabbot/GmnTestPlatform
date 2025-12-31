<template>
  <div class="test-case-list">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>Test Cases</span>
          <el-button type="primary" @click="handleNewCase">
            <el-icon><Plus /></el-icon>
            New Case
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
          placeholder="Search cases..."
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
        <el-table-column prop="caseName" label="Case Name" min-width="200" />
        <el-table-column label="Method" width="100">
          <template #default="{ row }">
            <el-tag :type="getMethodType(row.method)" size="small">
              {{ row.method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="url" label="URL" min-width="250" show-overflow-tooltip />
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.isActive" />
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              text
              @click="router.push(`/testing/cases/${row.id}/edit`)"
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

      <!-- Pagination -->
      <div class="pagination-container" style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalCases"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="pageSize = $event; currentPage = 1"
        />
      </div>
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog
      v-model="showDialog"
      :title="currentCase.id ? 'Edit Test Case' : 'New Test Case'"
      width="800px"
      @close="resetForm"
    >
      <el-form :model="currentCase" label-width="140px">
        <el-form-item label="Case Name">
          <el-input v-model="currentCase.caseName" />
        </el-form-item>

        <el-form-item label="Project">
          <el-select v-model="currentCase.projectId" placeholder="Select Project" @change="onDialogProjectChange">
            <el-option
              v-for="proj in projects"
              :key="proj.id"
              :label="proj.projectName"
              :value="proj.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Module">
          <el-select v-model="currentCase.moduleId" placeholder="Select Module" :disabled="!currentCase.projectId">
            <el-option
              v-for="item in dialogModules"
              :key="item.id"
              :label="item.moduleName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="HTTP Method">
          <el-radio-group v-model="currentCase.method">
            <el-radio label="GET">GET</el-radio>
            <el-radio label="POST">POST</el-radio>
            <el-radio label="PUT">PUT</el-radio>
            <el-radio label="DELETE">DELETE</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="Request URL">
          <el-input v-model="currentCase.url" />
        </el-form-item>

        <el-form-item label="Request Body">
          <el-input
            v-model="currentCase.body"
            type="textarea"
            :rows="4"
            placeholder='{"key": "value"}'
          />
        </el-form-item>

        <el-form-item label="Assertion Script">
          <el-input
            v-model="currentCase.assertionScript"
            type="textarea"
            :rows="3"
            placeholder="status_code == 200"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">Cancel</el-button>
        <el-button type="primary" @click="saveCase">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Delete } from '@element-plus/icons-vue'
import { testCaseApi } from '../api/testCase'
import { testModuleApi } from '../api/testModule'
import { projectApi } from '../api/project'

const router = useRouter()

const loading = ref(false)
const showDialog = ref(false)
const searchText = ref('')
const selectedProjectId = ref(null)
const selectedModuleId = ref(null)
const cases = ref([])
const modules = ref([])
const projects = ref([])

// Pagination
const currentPage = ref(1)
const pageSize = ref(20)
const totalCases = ref(0)
const currentCase = ref({
  caseName: '',
  projectId: null,
  moduleId: null,
  method: 'GET',
  url: '',
  body: '',
  assertionScript: 'status_code == 200',
  isActive: true
})

const dialogModules = computed(() => {
  if (!currentCase.value.projectId) return []
  return modules.value.filter(m => m.project && m.project.id === currentCase.value.projectId)
})

const filteredCases = computed(() => {
  let filtered = cases.value
  if (searchText.value) {
    filtered = filtered.filter(c =>
    c.caseName.toLowerCase().includes(searchText.value.toLowerCase())
  )
  }
  totalCases.value = filtered.length
  // Apply pagination
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filtered.slice(start, end)
})

const handlePageChange = (page) => {
  currentPage.value = page
}

const filteredModules = computed(() => {
  if (!selectedProjectId.value) return modules.value
  return modules.value.filter(m => m.project && m.project.id === selectedProjectId.value)
})

const getMethodType = (method) => {
  const types = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger'
  }
  return types[method] || 'info'
}



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

const onProjectChange = () => {
  selectedModuleId.value = null
  loadCases()
}

const onDialogProjectChange = () => {
  currentCase.value.moduleId = null
}

const loadCases = async () => {
  loading.value = true
  try {
    if (selectedModuleId.value) {
      const allCases = await testCaseApi.getAll()
      cases.value = allCases.filter(c => c.module && c.module.id === selectedModuleId.value)
    } else {
      cases.value = await testCaseApi.getAll()
    }
  } catch (error) {
    ElMessage.error('Failed to load test cases')
  } finally {
    loading.value = false
  }
}

const editCase = (row) => {
  currentCase.value = { 
    ...row,
    projectId: row.module && row.module.project ? row.module.project.id : null,
    moduleId: row.module ? row.module.id : null
  }
  showDialog.value = true
}

const saveCase = async () => {
  try {
    if (!currentCase.value.moduleId) {
      ElMessage.warning('Please select a module')
      return
    }

    const payload = {
      ...currentCase.value,
      module: { id: currentCase.value.moduleId }
    }
    
    if (currentCase.value.id) {
      await testCaseApi.update(currentCase.value.id, payload)
      ElMessage.success('Test case updated')
    } else {
      await testCaseApi.create(payload)
      ElMessage.success('Test case created')
    }
    showDialog.value = false
    loadCases()
  } catch (error) {
    ElMessage.error('Failed to save test case')
  }
}

const deleteCase = async (id) => {
  try {
    await ElMessageBox.confirm('Are you sure to delete this test case?', 'Warning', {
      confirmButtonText: 'OK',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await testCaseApi.delete(id)
    ElMessage.success('Test case deleted')
    loadCases()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to delete test case')
    }
  }
}

const handleNewCase = () => {
  router.push('/testing/cases/new')
}

const resetForm = () => {
  currentCase.value = {
    caseName: '',
    projectId: null,
    moduleId: null,
    method: 'GET',
    url: '',
    body: '',
    assertionScript: 'status_code == 200',
    isActive: true
  }
}

watch([searchText, selectedProjectId, selectedModuleId], () => {
  currentPage.value = 1 // Reset to first page when filter changes
})

onMounted(() => {
  loadProjects()
  loadModules()
  loadCases()
})
</script>

<style scoped>
.test-case-list {
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
