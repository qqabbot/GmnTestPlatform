<template>
  <div class="environment-list">
    <el-row :gutter="20">
      <!-- Left: Environment List -->
      <el-col :span="8">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>Environments</span>
              <el-button type="primary" size="small" @click="showEnvDialog = true; editEnv = { id: null, envName: '', domain: '', description: '' }">
                <el-icon><Plus /></el-icon>
              </el-button>
            </div>
          </template>
          <el-table :data="environments" highlight-current-row @current-change="handleEnvSelect" style="width: 100%" v-loading="loading">
            <el-table-column prop="envName" label="Name" />
            <el-table-column label="Actions" width="150" align="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" circle text @click.stop="openEditEnvDialog(row)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button type="danger" size="small" circle text @click.stop="deleteEnvironment(row.id)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- Right: Variables -->
      <el-col :span="16">
        <el-card class="box-card" v-if="selectedEnv">
          <template #header>
            <div class="card-header">
              <span>Variables for {{ selectedEnv.envName }}</span>
              <el-button type="primary" size="small" @click="showVarDialog = true">
                <el-icon><Plus /></el-icon>
                Add Variable
              </el-button>
            </div>
          </template>
          <el-table :data="variables" style="width: 100%" v-loading="varLoading">
            <el-table-column prop="keyName" label="Key" width="180" />
            <el-table-column prop="valueContent" label="Value" />
            <el-table-column label="Actions" width="120" align="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" circle text @click="openEditVarDialog(row)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button type="danger" size="small" circle text @click="deleteVariable(row.id)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        <el-empty v-else description="Select an environment to view variables" />
      </el-col>
    </el-row>

    <!-- Environment Dialog -->
    <el-dialog v-model="showEnvDialog" :title="editEnv.id ? 'Edit Environment' : 'New Environment'" width="400px" @close="resetEnvForm">
      <el-form :model="currentEnvForm" label-width="80px">
        <el-form-item label="Name" required>
          <el-input v-model="currentEnvForm.envName" placeholder="e.g., QA, Prod" />
        </el-form-item>
        <el-form-item label="Domain">
          <el-input v-model="currentEnvForm.domain" placeholder="e.g., http://api.example.com" />
        </el-form-item>
        <el-form-item label="Desc">
          <el-input v-model="currentEnvForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEnvDialog = false">Cancel</el-button>
        <el-button type="primary" @click="editEnv.id ? updateEnvironment() : saveEnvironment()">Save</el-button>
      </template>
    </el-dialog>

    <!-- Variable Dialog -->
    <el-dialog v-model="showVarDialog" title="New Variable" width="400px" @close="resetVarForm">
      <el-form :model="newVar" label-width="80px">
        <el-form-item label="Key" required>
          <el-input v-model="newVar.keyName" placeholder="e.g., base_url" />
        </el-form-item>
        <el-form-item label="Value" required>
          <el-input v-model="newVar.valueContent" placeholder="Variable value" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showVarDialog = false">Cancel</el-button>
        <el-button type="primary" @click="saveVariable">Save</el-button>
      </template>
    </el-dialog>

    <!-- Edit Variable Dialog -->
    <el-dialog v-model="showEditVarDialog" title="Edit Variable" width="400px" @close="resetEditVarForm">
      <el-form :model="editVar" label-width="80px">
        <el-form-item label="Key" required>
          <el-input v-model="editVar.keyName" placeholder="e.g., base_url" />
        </el-form-item>
        <el-form-item label="Value" required>
          <el-input v-model="editVar.valueContent" placeholder="Variable value" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditVarDialog = false">Cancel</el-button>
        <el-button type="primary" @click="updateVariable">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { environmentApi } from '../api/environment'
import { variableApi } from '../api/variable'

const loading = ref(false)
const varLoading = ref(false)
const environments = ref([])
const variables = ref([])
const selectedEnv = ref(null)

const showEnvDialog = ref(false)
const showVarDialog = ref(false)
const showEditVarDialog = ref(false)

const newEnv = ref({ envName: '', domain: '', description: '' })
const editEnv = ref({ id: null, envName: '', domain: '', description: '' })
const newVar = ref({ keyName: '', valueContent: '' })
const editVar = ref({ id: null, keyName: '', valueContent: '' })

// Computed property for current form (edit or new)
const currentEnvForm = computed(() => {
  return editEnv.value.id ? editEnv.value : newEnv.value
})

const loadEnvironments = async () => {
  loading.value = true
  try {
    environments.value = await environmentApi.getAll()
  } catch (error) {
    ElMessage.error('Failed to load environments')
  } finally {
    loading.value = false
  }
}

const handleEnvSelect = async (row) => {
  if (!row) return
  selectedEnv.value = row
  loadVariables(row.id)
}

const loadVariables = async (envId) => {
  varLoading.value = true
  try {
    variables.value = await variableApi.getByEnvironment(envId)
  } catch (error) {
    ElMessage.error('Failed to load variables')
  } finally {
    varLoading.value = false
  }
}

const saveEnvironment = async () => {
  if (!newEnv.value.envName) return ElMessage.warning('Name is required')
  try {
    await environmentApi.create(newEnv.value)
    ElMessage.success('Environment created')
    showEnvDialog.value = false
    loadEnvironments()
  } catch (error) {
    ElMessage.error('Failed to create environment')
  }
}

const deleteEnvironment = async (id) => {
  try {
    await ElMessageBox.confirm('Delete this environment?', 'Warning', { type: 'warning' })
    await environmentApi.delete(id)
    ElMessage.success('Environment deleted')
    if (selectedEnv.value && selectedEnv.value.id === id) {
      selectedEnv.value = null
      variables.value = []
    }
    loadEnvironments()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('Failed to delete environment')
  }
}

const saveVariable = async () => {
  if (!newVar.value.keyName || !newVar.value.valueContent) return ElMessage.warning('Key and Value are required')
  try {
    await variableApi.create({
      ...newVar.value,
      environment: { id: selectedEnv.value.id }
    })
    ElMessage.success('Variable created')
    showVarDialog.value = false
    loadVariables(selectedEnv.value.id)
  } catch (error) {
    ElMessage.error('Failed to create variable')
  }
}

const deleteVariable = async (id) => {
  try {
    await variableApi.delete(id)
    ElMessage.success('Variable deleted')
    loadVariables(selectedEnv.value.id)
  } catch (error) {
    ElMessage.error('Failed to delete variable')
  }
}

const openEditEnvDialog = (row) => {
  editEnv.value = { id: row.id, envName: row.envName, domain: row.domain || '', description: row.description || '' }
  showEnvDialog.value = true
}

const updateEnvironment = async () => {
  if (!editEnv.value.envName) return ElMessage.warning('Name is required')
  try {
    await environmentApi.update(editEnv.value.id, {
      envName: editEnv.value.envName,
      domain: editEnv.value.domain,
      description: editEnv.value.description
    })
    ElMessage.success('Environment updated')
    showEnvDialog.value = false
    loadEnvironments()
    // If updated environment is selected, refresh variables
    if (selectedEnv.value && selectedEnv.value.id === editEnv.value.id) {
      selectedEnv.value = { ...selectedEnv.value, ...editEnv.value }
    }
  } catch (error) {
    ElMessage.error('Failed to update environment')
  }
}

const resetEnvForm = () => {
  newEnv.value = { envName: '', domain: '', description: '' }
  editEnv.value = { id: null, envName: '', domain: '', description: '' }
}

const resetVarForm = () => {
  newVar.value = { keyName: '', valueContent: '' }
}

const openEditVarDialog = (row) => {
  editVar.value = { id: row.id, keyName: row.keyName, valueContent: row.valueContent }
  showEditVarDialog.value = true
}

const resetEditVarForm = () => {
  editVar.value = { id: null, keyName: '', valueContent: '' }
}

const updateVariable = async () => {
  if (!editVar.value.keyName || !editVar.value.valueContent) {
    return ElMessage.warning('Key and Value are required')
  }
  try {
    await variableApi.update(editVar.value.id, {
      keyName: editVar.value.keyName,
      valueContent: editVar.value.valueContent,
      environment: { id: selectedEnv.value.id }
    })
    ElMessage.success('Variable updated')
    showEditVarDialog.value = false
    loadVariables(selectedEnv.value.id)
  } catch (error) {
    ElMessage.error('Failed to update variable')
  }
}

onMounted(() => {
  loadEnvironments()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
