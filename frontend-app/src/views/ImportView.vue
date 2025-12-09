<template>
  <div class="import-view">
    <h2>Import API Definitions</h2>
    <el-tabs v-model="activeTab" type="border-card">
      
      <!-- Swagger / OpenAPI -->
      <el-tab-pane label="Swagger / OpenAPI" name="swagger">
        <el-form label-width="120px">
          <el-form-item label="Target Project" required>
            <el-select v-model="form.projectId" placeholder="Select Project">
              <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="Import Type">
             <el-radio-group v-model="swaggerType">
               <el-radio label="url">URL</el-radio>
               <el-radio label="json">JSON Content</el-radio>
             </el-radio-group>
          </el-form-item>
          <el-form-item label="Swagger URL" v-if="swaggerType === 'url'">
             <el-input v-model="form.url" placeholder="http://petstore.swagger.io/v2/swagger.json" />
          </el-form-item>
          <el-form-item label="JSON Content" v-if="swaggerType === 'json'">
             <monaco-editor v-model="form.content" language="json" height="300px" />
          </el-form-item>
          
          <el-form-item>
             <el-button type="primary" @click="handleImportSwagger" :loading="importing">Import Swagger</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { importApi } from '../api/import'
import { projectApi } from '../api/project'
import { ElMessage } from 'element-plus'
import MonacoEditor from '../components/MonacoEditor.vue'

const activeTab = ref('swagger')
const swaggerType = ref('url')
const importing = ref(false)
const projects = ref([])

const form = reactive({
    projectId: null,
    url: '',
    content: ''
})

onMounted(async () => {
    try {
        projects.value = await projectApi.getAll()
    } catch (e) {
        ElMessage.error('Failed to load projects')
    }
})

const handleImportSwagger = async () => {
    if (!form.projectId) {
        ElMessage.warning('Select a project')
        return
    }
    if (swaggerType.value === 'url' && !form.url) {
        ElMessage.warning('Enter URL')
        return
    }
    if (swaggerType.value === 'json' && !form.content) {
        ElMessage.warning('Enter JSON content')
        return
    }
    
    importing.value = true
    try {
        const res = await importApi.importSwagger(
            form.projectId, 
            swaggerType.value === 'url' ? form.url : null,
            swaggerType.value === 'json' ? form.content : null
        )
        ElMessage.success(res.message)
    } catch (e) {
        ElMessage.error('Import Failed: ' + (e.response?.data?.error || e.message))
    } finally {
        importing.value = false
    }
}

</script>

<style scoped>
.import-view {
  padding: 20px;
}
</style>
