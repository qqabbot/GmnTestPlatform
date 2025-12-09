<template>
  <div class="step-template-list">
    <div class="header-actions">
      <h2>Step Library</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon> New Template
      </el-button>
    </div>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="Name" />
      <el-table-column prop="method" label="Method" width="100">
        <template #default="{ row }">
          <el-tag :type="row.method === 'GET' ? 'success' : 'primary'">{{ row.method }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="url" label="URL" show-overflow-tooltip />
      <el-table-column label="Actions" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">Edit</el-button>
          <el-button link type="danger" @click="handleDelete(row)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Template Editor Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? 'Edit Template' : 'New Template'"
      width="60%"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="Name">
          <el-input v-model="form.name" placeholder="e.g. Login Step" />
        </el-form-item>
        <el-form-item label="Method">
          <el-radio-group v-model="form.method">
            <el-radio label="GET">GET</el-radio>
            <el-radio label="POST">POST</el-radio>
            <el-radio label="PUT">PUT</el-radio>
            <el-radio label="DELETE">DELETE</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="URL">
           <variable-input v-model="form.url" placeholder="URL" />
        </el-form-item>
        <el-tabs>
          <el-tab-pane label="Body">
             <monaco-editor v-model="form.body" language="json" height="200px" />
          </el-tab-pane>
          <el-tab-pane label="Headers">
             <monaco-editor v-model="form.headers" language="json" height="200px" />
          </el-tab-pane>
          <el-tab-pane label="Combined Script">
             <monaco-editor v-model="form.assertionScript" language="groovy" height="200px" />
             <div class="help-text">Contains assertions and extractors logic (Groovy)</div>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">Cancel</el-button>
          <el-button type="primary" @click="saveTemplate">Save</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { stepTemplateApi } from '../api/stepTemplate'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MonacoEditor from '../components/MonacoEditor.vue'
import VariableInput from '../components/VariableInput.vue'

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)

const form = reactive({
  id: null,
  name: '',
  method: 'GET',
  url: '',
  headers: '{}',
  body: '',
  assertionScript: '',
  projectId: null // Optional, logic to set this from context usually needed
})

const fetchTemplates = async () => {
  loading.value = true
  try {
    tableData.value = await stepTemplateApi.getAll() // Fetch All for now
  } catch (e) {
    ElMessage.error('Failed to load templates')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    name: '',
    method: 'GET',
    url: '',
    headers: '{}',
    body: '',
    assertionScript: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm('Are you sure you want to delete this template?', 'Warning', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning'
  }).then(async () => {
    try {
      await stepTemplateApi.delete(row.id)
      ElMessage.success('Deleted successfully')
      fetchTemplates()
    } catch (e) {
      ElMessage.error('Failed to delete')
    }
  })
}

const saveTemplate = async () => {
  try {
    if (isEdit.value) {
      await stepTemplateApi.update(form.id, form)
    } else {
      await stepTemplateApi.create(form)
    }
    ElMessage.success('Saved successfully')
    dialogVisible.value = false
    fetchTemplates()
  } catch (e) {
    ElMessage.error('Failed to save')
  }
}

onMounted(() => {
  fetchTemplates()
})
</script>

<style scoped>
.step-template-list {
  padding: 20px;
}
.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
