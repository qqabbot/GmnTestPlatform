<template>
  <div class="test-execution">
    <el-card class="box-card">
      <template #header>
        <span>Test Execution</span>
      </template>

      <!-- Configuration Panel -->
      <el-form :model="config" label-width="120px">
        <el-form-item label="Scope">
          <el-radio-group v-model="config.scope" @change="onScopeChange">
            <el-radio label="project">By Project</el-radio>
            <el-radio label="module">By Module</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="Project" v-if="config.scope === 'project'">
          <el-select v-model="config.projectId" placeholder="Select Project">
            <el-option v-for="proj in projects" :key="proj.id" :label="proj.projectName" :value="proj.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="Module" v-if="config.scope === 'module'">
          <el-select v-model="config.moduleId" placeholder="Select Module">
            <el-option v-for="mod in modules" :key="mod.id" :label="mod.moduleName" :value="mod.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="Environment">
          <el-select v-model="config.envKey" placeholder="Select environment">
            <el-option v-for="env in environments" :key="env.id" :label="env.envName" :value="env.envName" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            @click="startExecution"
            :loading="executing"
          >
            <el-icon><VideoPlay /></el-icon>
            Start Execution
          </el-button>
        </el-form-item>
      </el-form>

      <!-- Statistics Cards -->
      <el-row :gutter="20" v-if="results.length > 0">
        <el-col :span="6">
          <el-statistic title="Total Cases" :value="stats.total" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="Passed" :value="stats.passed">
            <template #suffix>
              <el-icon style="color: #67c23a"><SuccessFilled /></el-icon>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="Failed" :value="stats.failed">
            <template #suffix>
              <el-icon style="color: #f56c6c"><CircleCloseFilled /></el-icon>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="Skipped" :value="stats.skipped">
            <template #suffix>
              <el-icon style="color: #909399"><WarningFilled /></el-icon>
            </template>
          </el-statistic>
        </el-col>
      </el-row>

      <!-- Results Table -->
      <!-- Results List -->
      <div v-if="results.length > 0" style="margin-top: 20px;">
        <el-collapse>
          <el-collapse-item v-for="(res, index) in results" :key="index" :name="index">
            <template #title>
              <div style="display: flex; align-items: center; width: 100%; justify-content: space-between; padding-right: 10px;">
                <span style="font-weight: bold;">{{ res.caseName }}</span>
                <div>
                  <el-tag :type="getStatusType(res.status)" size="small" style="margin-right: 10px;">{{ res.status }}</el-tag>
                  <span style="color: #909399; font-size: 12px;">{{ res.duration }} ms</span>
                </div>
              </div>
            </template>
            
            <div class="result-detail">
              <p><strong>Detail:</strong> {{ res.detail }}</p>
              
              <!-- We can add more details here if the backend returns them, e.g. request/response -->
              <!-- Currently the execute endpoint might only return summary. 
                   If we want full details, we need to ensure backend returns them or we fetch logs. -->
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { testCaseApi } from '../api/testCase'
import { projectApi } from '../api/project'
import { testModuleApi } from '../api/testModule'
import { environmentApi } from '../api/environment'
import MonacoEditor from '../components/MonacoEditor.vue'

const executing = ref(false)
const config = ref({
  scope: 'project',
  projectId: null,
  moduleId: null,
  envKey: ''
})
const results = ref([])
const projects = ref([])
const modules = ref([])
const environments = ref([])

const stats = computed(() => {
  return {
    total: results.value.length,
    passed: results.value.filter(r => r.status === 'PASS').length,
    failed: results.value.filter(r => r.status === 'FAIL').length,
    skipped: results.value.filter(r => r.status === 'SKIP').length
  }
})

const getStatusType = (status) => {
  const types = {
    PASS: 'success',
    FAIL: 'danger',
    SKIP: 'info'
  }
  return types[status] || 'info'
}

const onScopeChange = () => {
  config.value.projectId = null
  config.value.moduleId = null
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

const loadEnvironments = async () => {
  try {
    environments.value = await environmentApi.getAll()
    if (environments.value.length > 0) {
      config.value.envKey = environments.value[0].envName
    }
  } catch (error) {
    ElMessage.error('Failed to load environments')
  }
}

const startExecution = async () => {
  if (config.value.scope === 'project' && !config.value.projectId) {
    ElMessage.warning('Please select a project')
    return
  }
  if (config.value.scope === 'module' && !config.value.moduleId) {
    ElMessage.warning('Please select a module')
    return
  }

  executing.value = true
  results.value = []
  
  try {
    const params = { envKey: config.value.envKey }
    if (config.value.scope === 'project') {
      params.projectId = config.value.projectId
    } else {
      params.moduleId = config.value.moduleId
    }
    
    const data = await testCaseApi.execute(params)
    results.value = data
    
    const passRate = ((stats.value.passed / stats.value.total) * 100).toFixed(1)
    ElMessage.success(`Execution completed! Pass rate: ${passRate}%`)
  } catch (error) {
    ElMessage.error('Execution failed')
  } finally {
    executing.value = false
  }
}

onMounted(() => {
  loadProjects()
  loadModules()
  loadEnvironments()
})
</script>

<style scoped>
.test-execution {
  width: 100%;
}

.el-statistic {
  text-align: center;
}
</style>
