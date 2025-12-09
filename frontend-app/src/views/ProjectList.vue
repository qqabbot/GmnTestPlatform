<template>
  <div class="project-list">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>Projects</span>
          <el-button type="primary" @click="showDialog = true">
            <el-icon><Plus /></el-icon>
            New Project
          </el-button>
        </div>
      </template>

      <el-table :data="projects" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="projectName" label="Project Name" min-width="200" />
        <el-table-column prop="description" label="Description" min-width="300" />
        <el-table-column prop="createdAt" label="Created At" width="180" />
        <el-table-column label="Actions" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="editProject(row)">
              Edit
            </el-button>
            <el-button type="danger" size="small" text @click="deleteProject(row.id)">
              Delete
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Dialog -->
    <el-dialog
      v-model="showDialog"
      :title="currentProject.id ? 'Edit Project' : 'New Project'"
      width="500px"
      @close="resetForm"
    >
      <el-form :model="currentProject" label-width="100px">
        <el-form-item label="Name" required>
          <el-input v-model="currentProject.projectName" />
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="currentProject.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">Cancel</el-button>
        <el-button type="primary" @click="saveProject">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { projectApi } from '../api/project'

const loading = ref(false)
const showDialog = ref(false)
const projects = ref([])
const currentProject = ref({
  projectName: '',
  description: ''
})

const loadProjects = async () => {
  loading.value = true
  try {
    projects.value = await projectApi.getAll()
  } catch (error) {
    ElMessage.error('Failed to load projects')
  } finally {
    loading.value = false
  }
}

const editProject = (row) => {
  currentProject.value = { ...row }
  showDialog.value = true
}

const saveProject = async () => {
  try {
    if (currentProject.value.id) {
      await projectApi.update(currentProject.value.id, currentProject.value)
      ElMessage.success('Project updated')
    } else {
      await projectApi.create(currentProject.value)
      ElMessage.success('Project created')
    }
    showDialog.value = false
    loadProjects()
  } catch (error) {
    ElMessage.error('Failed to save project')
  }
}

const deleteProject = async (id) => {
  try {
    await ElMessageBox.confirm('Are you sure to delete this project?', 'Warning', {
      type: 'warning'
    })
    await projectApi.delete(id)
    ElMessage.success('Project deleted')
    loadProjects()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('Failed to delete project')
  }
}

const resetForm = () => {
  currentProject.value = {
    projectName: '',
    description: ''
  }
}

onMounted(() => {
  loadProjects()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
