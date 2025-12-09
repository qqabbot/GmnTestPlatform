<template>
  <div class="module-list">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>Test Modules</span>
          <el-button type="primary" @click="showDialog = true">
            <el-icon><Plus /></el-icon>
            New Module
          </el-button>
        </div>
      </template>

      <!-- Filter Section -->
      <div class="filter-section">
        <el-select v-model="selectedProjectId" placeholder="Filter by Project" style="width: 200px; margin-right: 10px" clearable @change="loadModules">
          <el-option v-for="proj in projects" :key="proj.id" :label="proj.projectName" :value="proj.id" />
        </el-select>
        <el-input
          v-model="searchText"
          placeholder="Search modules..."
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
        :data="filteredModules"
        style="width: 100%; margin-top: 20px"
        v-loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="moduleName" label="Module Name" min-width="200" />
        <el-table-column label="Project" min-width="200">
          <template #default="{ row }">
            {{ row.project ? row.project.projectName : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Created At" width="180" />
        <el-table-column label="Actions" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              text
              @click="editModule(row)"
            >
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button
              type="danger"
              size="small"
              text
              @click="deleteModule(row.id)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog
      v-model="showDialog"
      :title="currentModule.id ? 'Edit Module' : 'New Module'"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="currentModule" label-width="120px">
        <el-form-item label="Module Name">
          <el-input v-model="currentModule.moduleName" />
        </el-form-item>

        <el-form-item label="Project">
          <el-select v-model="currentModule.projectId" placeholder="Select Project" style="width: 100%">
            <el-option
              v-for="proj in projects"
              :key="proj.id"
              :label="proj.projectName"
              :value="proj.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">Cancel</el-button>
        <el-button type="primary" @click="saveModule">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { testModuleApi } from '../api/testModule'
import { projectApi } from '../api/project'

const loading = ref(false)
const showDialog = ref(false)
const searchText = ref('')
const selectedProjectId = ref(null)
const modules = ref([])
const projects = ref([])
const currentModule = ref({
  moduleName: '',
  projectId: null
})

const filteredModules = computed(() => {
  let result = modules.value
  
  if (selectedProjectId.value) {
    result = result.filter(m => m.project && m.project.id === selectedProjectId.value)
  }
  
  if (searchText.value) {
    result = result.filter(m =>
      m.moduleName.toLowerCase().includes(searchText.value.toLowerCase())
    )
  }
  
  return result
})

const loadProjects = async () => {
  try {
    projects.value = await projectApi.getAll()
  } catch (error) {
    ElMessage.error('Failed to load projects')
  }
}

const loadModules = async () => {
  loading.value = true
  try {
    modules.value = await testModuleApi.getAll()
  } catch (error) {
    ElMessage.error('Failed to load modules')
  } finally {
    loading.value = false
  }
}

const editModule = (row) => {
  currentModule.value = {
    ...row,
    projectId: row.project ? row.project.id : null
  }
  showDialog.value = true
}

const saveModule = async () => {
  try {
    if (!currentModule.value.moduleName) {
      ElMessage.warning('Please enter module name')
      return
    }
    if (!currentModule.value.projectId) {
      ElMessage.warning('Please select a project')
      return
    }

    const payload = {
      ...currentModule.value,
      project: { id: currentModule.value.projectId }
    }
    
    if (currentModule.value.id) {
      await testModuleApi.update(currentModule.value.id, payload)
      ElMessage.success('Module updated')
    } else {
      await testModuleApi.create(payload)
      ElMessage.success('Module created')
    }
    showDialog.value = false
    loadModules()
  } catch (error) {
    ElMessage.error('Failed to save module')
  }
}

const deleteModule = async (id) => {
  try {
    await ElMessageBox.confirm('Are you sure to delete this module?', 'Warning', {
      confirmButtonText: 'OK',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await testModuleApi.delete(id)
    ElMessage.success('Module deleted')
    loadModules()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to delete module')
    }
  }
}

const resetForm = () => {
  currentModule.value = {
    moduleName: '',
    projectId: null
  }
}

onMounted(() => {
  loadProjects()
  loadModules()
})
</script>

<style scoped>
.module-list {
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
