<template>
  <div class="environment-container">
    <!-- Sidebar: Environment List -->
    <div class="env-sidebar">
      <div class="sidebar-header">
        <h2>Environments</h2>
        <el-button 
          type="primary" 
          circle 
          class="add-btn"
          @click="openAddDialog"
        >
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>

      <div class="env-list" v-loading="loading">
        <div 
          v-for="env in environments" 
          :key="env.id"
          class="env-item"
          :class="{ active: selectedEnv?.id === env.id }"
          @click="handleEnvSelect(env)"
        >
          <div class="env-status" :style="{ backgroundColor: getEnvColor(env.envName) }"></div>
          <div class="env-info">
            <div class="env-name">{{ env.envName }}</div>
            <div class="env-domain">{{ env.domain || 'No domain' }}</div>
          </div>
          <div class="env-actions">
            <el-dropdown trigger="click" @click.stop>
              <el-button link class="more-btn">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #header></template>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="openEditEnvDialog(env)">
                    <el-icon><Edit /></el-icon> Edit
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="deleteEnvironment(env.id)" style="color: #f56c6c">
                    <el-icon><Delete /></el-icon> Delete
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content: Variables -->
    <div class="variable-workspace">
      <div v-if="selectedEnv" class="workspace-content">
        <header class="content-header">
          <div class="header-left">
            <div class="env-badge" :style="{ backgroundColor: getEnvColor(selectedEnv.envName) + '20', color: getEnvColor(selectedEnv.envName) }">
              {{ selectedEnv.envName }}
            </div>
            <h1>Variables</h1>
          </div>
          <div class="header-right">
            <el-radio-group v-model="editMode" size="small" style="margin-right: 16px;">
              <el-radio-button label="list">List View</el-radio-button>
              <el-radio-button label="json">JSON Mode</el-radio-button>
            </el-radio-group>
            <el-button type="primary" size="small" @click="showVarDialog = true">
              <el-icon><Plus /></el-icon> Add Variable
            </el-button>
          </div>
        </header>

        <div class="workspace-body" v-loading="varLoading">
          <!-- List View -->
          <div v-if="editMode === 'list'" class="list-view">
            <el-table :data="variables" style="width: 100%" class="modern-table">
              <el-table-column prop="keyName" label="KEY" width="240">
                <template #default="{ row }">
                  <code class="key-code">{{ row.keyName }}</code>
                </template>
              </el-table-column>
              <el-table-column label="VALUE">
                <template #default="{ row }">
                  <div class="inline-edit-cell">
                    <el-input 
                      v-if="row.isEditing"
                      v-model="row.tempValue"
                      size="small"
                      @blur="saveInlineEdit(row)"
                      @keyup.enter="saveInlineEdit(row)"
                      ref="inlineInput"
                    />
                    <div v-else class="value-display" @dblclick="startInlineEdit(row)">
                      {{ (row.keyName.toLowerCase().includes('secret') || row.keyName.toLowerCase().includes('password')) && !row.showRaw ? '••••••••' : row.valueContent }}
                      <el-icon 
                        v-if="row.keyName.toLowerCase().includes('secret') || row.keyName.toLowerCase().includes('password')" 
                        class="toggle-visibility"
                        @click.stop="row.showRaw = !row.showRaw"
                      >
                        <View v-if="!row.showRaw" />
                        <Hide v-else />
                      </el-icon>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="ACTIONS" width="100" align="right">
                <template #default="{ row }">
                  <el-button link type="danger" @click="deleteVariable(row.id)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- JSON Mode (Bulk Editor) -->
          <div v-else class="json-view">
            <div class="json-editor-container">
              <MonacoEditor
                v-model="jsonContent"
                language="json"
                height="100%"
              />
              <div class="json-actions">
                <el-button type="primary" @click="saveBulkJson" :loading="saving">Sync Changes</el-button>
                <el-button @click="resetJson">Reset</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="Choose an environment to manage its variables" class="fancy-empty" />
    </div>

    <!-- Environment Dialog -->
    <el-dialog 
      v-model="showEnvDialog" 
      :title="editEnv.id ? 'Edit Environment' : 'Create Environment'" 
      width="440px"
      custom-class="modern-dialog"
    >
      <el-form :model="currentEnvForm" label-position="top">
        <el-form-item label="Environment Name" required>
          <el-input v-model="currentEnvForm.envName" placeholder="e.g. Production, Staging" />
        </el-form-item>
        <el-form-item label="Base URL / Domain">
          <el-input v-model="currentEnvForm.domain" placeholder="https://api.example.com" />
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="currentEnvForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEnvDialog = false">Cancel</el-button>
          <el-button type="primary" @click="editEnv.id ? updateEnvironment() : saveEnvironment()" :loading="saving">
            {{ editEnv.id ? 'Update' : 'Create' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Variable Dialog (Form add) -->
    <el-dialog v-model="showVarDialog" title="Add Variable" width="400px">
      <el-form :model="newVar" label-position="top">
        <el-form-item label="Key" required>
          <el-input v-model="newVar.keyName" placeholder="API_KEY" />
        </el-form-item>
        <el-form-item label="Value" required>
          <el-input v-model="newVar.valueContent" placeholder="your-value" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showVarDialog = false">Cancel</el-button>
        <el-button type="primary" @click="saveVariable">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, MoreFilled, View, Hide } from '@element-plus/icons-vue'
import { environmentApi } from '../api/environment'
import { variableApi } from '../api/variable'
import MonacoEditor from '../components/MonacoEditor.vue'

const loading = ref(false)
const varLoading = ref(false)
const saving = ref(false)
const environments = ref([])
const variables = ref([])
const selectedEnv = ref(null)
const editMode = ref('list') // 'list' or 'json'
const jsonContent = ref('')

const showEnvDialog = ref(false)
const showVarDialog = ref(false)

const newEnv = ref({ envName: '', domain: '', description: '' })
const editEnv = ref({ id: null, envName: '', domain: '', description: '' })
const newVar = ref({ keyName: '', valueContent: '' })

// Inline editing refs
const inlineInput = ref(null)

const currentEnvForm = computed(() => editEnv.value.id ? editEnv.value : newEnv.value)

const loadEnvironments = async () => {
  loading.value = true
  try {
    environments.value = await environmentApi.getAll()
    if (environments.value.length > 0 && !selectedEnv.value) {
      handleEnvSelect(environments.value[0])
    }
  } catch (error) {
    ElMessage.error('Failed to load environments')
  } finally {
    loading.value = false
  }
}

const handleEnvSelect = async (row) => {
  selectedEnv.value = row
  loadVariables(row.id)
}

const loadVariables = async (envId) => {
  varLoading.value = true
  try {
    const res = await variableApi.getByEnvironment(envId)
    variables.value = res.map(v => ({
      ...v,
      isEditing: false,
      tempValue: v.valueContent,
      showRaw: false
    }))
    updateJsonFromList()
  } catch (error) {
    ElMessage.error('Failed to load variables')
  } finally {
    varLoading.value = false
  }
}

const updateJsonFromList = () => {
  const obj = {}
  variables.value.forEach(v => {
    obj[v.keyName] = v.valueContent
  })
  jsonContent.value = JSON.stringify(obj, null, 2)
}

const startInlineEdit = (row) => {
  row.isEditing = true
  row.tempValue = row.valueContent
  nextTick(() => {
    if (inlineInput.value) inlineInput.value.focus()
  })
}

const saveInlineEdit = async (row) => {
  if (!row.isEditing) return
  if (row.tempValue === row.valueContent) {
    row.isEditing = false
    return
  }
  
  try {
    await variableApi.update(row.id, {
      keyName: row.keyName,
      valueContent: row.tempValue,
      environment: { id: selectedEnv.value.id }
    })
    row.valueContent = row.tempValue
    row.isEditing = false
    updateJsonFromList()
    ElMessage.success('Variable updated')
  } catch (error) {
    ElMessage.error('Failed to update variable')
    row.tempValue = row.valueContent
    row.isEditing = false
  }
}

const saveBulkJson = async () => {
  try {
    const newObj = JSON.parse(jsonContent.value)
    saving.value = true
    await variableApi.sync(selectedEnv.value.id, newObj)
    ElMessage.success('Environment variables synced')
    loadVariables(selectedEnv.value.id)
  } catch (e) {
    if (e instanceof SyntaxError) {
      ElMessage.error('Invalid JSON format')
    } else {
      ElMessage.error('Failed to sync changes')
    }
  } finally {
    saving.value = false
  }
}

const resetJson = () => {
  updateJsonFromList()
}

const getEnvColor = (name) => {
  const n = name.toLowerCase()
  if (n.includes('prod')) return '#ff4757'
  if (n.includes('staging') || n.includes('pre')) return '#ffa502'
  if (n.includes('qa') || n.includes('test')) return '#2e86de'
  if (n.includes('dev')) return '#2ed573'
  return '#747d8c'
}

const saveEnvironment = async () => {
  if (!newEnv.value.envName) return ElMessage.warning('Name is required')
  saving.value = true
  try {
    await environmentApi.create(newEnv.value)
    ElMessage.success('Environment created')
    showEnvDialog.value = false
    loadEnvironments()
  } catch (error) {
    ElMessage.error('Failed to create environment')
  } finally {
    saving.value = false
  }
}

const updateEnvironment = async () => {
  if (!editEnv.value.envName) return ElMessage.warning('Name is required')
  saving.value = true
  try {
    await environmentApi.update(editEnv.value.id, editEnv.value)
    ElMessage.success('Environment updated')
    showEnvDialog.value = false
    loadEnvironments()
  } catch (error) {
    ElMessage.error('Failed to update')
  } finally {
    saving.value = false
  }
}

const deleteEnvironment = async (id) => {
  try {
    await ElMessageBox.confirm('Delete this environment and all its variables?', 'Critical Action', { 
      type: 'error',
      confirmButtonText: 'Delete Forever',
      confirmButtonClass: 'el-button--danger'
    })
    await environmentApi.delete(id)
    ElMessage.success('Environment purged')
    if (selectedEnv.value?.id === id) selectedEnv.value = null
    loadEnvironments()
  } catch (e) {}
}

const saveVariable = async () => {
  if (!newVar.value.keyName || !newVar.value.valueContent) return ElMessage.warning('Required fields missing')
  try {
    await variableApi.create({
      ...newVar.value,
      environment: { id: selectedEnv.value.id }
    })
    ElMessage.success('Variable added')
    showVarDialog.value = false
    newVar.value = { keyName: '', valueContent: '' }
    loadVariables(selectedEnv.value.id)
  } catch (e) {
    ElMessage.error('Failed to add variable')
  }
}

const deleteVariable = async (id) => {
  try {
    await variableApi.delete(id)
    loadVariables(selectedEnv.value.id)
  } catch (e) {}
}

const openAddDialog = () => {
  editEnv.value = { id: null, envName: '', domain: '', description: '' }
  showEnvDialog.value = true
}

const openEditEnvDialog = (row) => {
  editEnv.value = { ...row }
  showEnvDialog.value = true
}

onMounted(loadEnvironments)
</script>

<style scoped>
.environment-container {
  display: flex;
  height: calc(100vh - 100px);
  background: #fcfcfc;
  overflow: hidden;
  margin: -20px; /* Offset parent padding */
}

/* Sidebar Styling */
.env-sidebar {
  width: 300px;
  background: #ffffff;
  border-right: 1px solid #edf2f7;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f7fafc;
}

.sidebar-header h2 {
  font-size: 18px;
  font-weight: 700;
  color: #1a202c;
  margin: 0;
}

.env-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.env-item {
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  position: relative;
  border: 1px solid transparent;
}

.env-item:hover {
  background: #f7fafc;
}

.env-item.active {
  background: #ecf2ff;
  border-color: #d0e1fd;
}

.env-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 12px;
  flex-shrink: 0;
}

.env-info {
  flex: 1;
  min-width: 0;
}

.env-name {
  font-weight: 600;
  font-size: 14px;
  color: #2d3748;
  margin-bottom: 2px;
}

.env-domain {
  font-size: 12px;
  color: #718096;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.env-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.env-item:hover .env-actions,
.env-item.active .env-actions {
  opacity: 1;
}

/* Main Workspace */
.variable-workspace {
  flex: 1;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  min-width: 0; /* Important: allow flex item to shrink below content size */
}

.workspace-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.content-header {
  padding: 20px 32px;
  background: #fff;
  border-bottom: 1px solid #edf2f7;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.env-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.content-header h1 {
  font-size: 20px;
  font-weight: 700;
  color: #1a202c;
  margin: 0;
}

.workspace-body {
  flex: 1;
  padding: 32px;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* Changed from overflow-y: auto to allow inner scroll */
  position: relative;
  min-width: 0;
}

.list-view {
  flex: 1;
  overflow-y: auto;
  overflow-x: auto;
  min-width: 0;
}

/* Modern Table */
.modern-table {
  background: transparent !important;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.key-code {
  background: #f1f5f9;
  color: #475569;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 13px;
}

.inline-edit-cell {
  min-height: 32px;
  display: flex;
  align-items: center;
}

.value-display {
  width: 100%;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: text;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background 0.2s;
  color: #4a5568;
}

.value-display:hover {
  background: #edf2f7;
}

.toggle-visibility {
  cursor: pointer;
  color: #a0aec0;
  margin-left: 8px;
}

.toggle-visibility:hover {
  color: #4a5568;
}

/* JSON Mode */
.json-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.json-editor-container {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #edf2f7;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.json-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.fancy-empty {
  height: 100%;
}

/* Dialog Styles */
:deep(.modern-dialog) {
  border-radius: 16px;
}

:deep(.el-radio-button__inner) {
  padding: 8px 20px;
}
</style>
