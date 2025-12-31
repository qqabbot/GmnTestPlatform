<template>
  <div class="ui-test-report-list">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>UI Test Execution Reports</span>
        </div>
      </template>

      <!-- Filter Section -->
      <div class="filter-section">
        <el-select v-model="selectedProjectId" placeholder="Select Project" style="width: 200px; margin-right: 10px" clearable @change="onProjectChange">
          <el-option v-for="proj in projects" :key="proj.id" :label="proj.projectName" :value="proj.id" />
        </el-select>
        <el-select v-model="selectedCaseId" placeholder="Select Test Case" style="width: 250px; margin-right: 10px" clearable @change="loadReports">
          <el-option v-for="c in filteredCases" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="loadReports">
          <el-icon><Refresh /></el-icon> Refresh
        </el-button>
      </div>

      <!-- Table -->
      <el-table
        :data="reports"
        style="width: 100%; margin-top: 20px"
        v-loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="caseName" label="Test Case" min-width="150" show-overflow-tooltip />
        <el-table-column label="Status" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Execution Time" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.executedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="Duration (ms)" width="120" />
        <el-table-column label="Actions" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              text
              @click="viewDetail(row.id)"
            >
              <el-icon><View /></el-icon> View Detail
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, View } from '@element-plus/icons-vue'
import { uiTestApi } from '../api/uiTest'
import { projectApi } from '../api/project'
import dayjs from 'dayjs'

const router = useRouter()

const loading = ref(false)
const selectedProjectId = ref(null)
const selectedCaseId = ref(null)
const reports = ref([])
const projects = ref([])
const cases = ref([])

const loadProjects = async () => {
  try {
    projects.value = await projectApi.getAll()
  } catch (error) {
    ElMessage.error('Failed to load projects')
  }
}

const onProjectChange = async () => {
  selectedCaseId.value = null
  if (selectedProjectId.value) {
    try {
      cases.value = await uiTestApi.getCasesByProject(selectedProjectId.value)
    } catch (error) {
       ElMessage.error('Failed to load cases')
    }
  } else {
    cases.value = []
  }
  loadReports()
}

const filteredCases = computed(() => cases.value)

const loadReports = async () => {
  loading.value = true
  try {
    let res
    if (selectedCaseId.value) {
      res = await uiTestApi.getRecordsByCase(selectedCaseId.value)
    } else if (selectedProjectId.value) {
      res = await uiTestApi.getRecordsByProject(selectedProjectId.value)
    } else {
      res = await uiTestApi.getAllRecords()
    }
    reports.value = res
  } catch (error) {
    ElMessage.error('Failed to load reports')
  } finally {
    loading.value = false
  }
}

const viewDetail = (id) => {
  router.push(`/ui-testing/reports/${id}`)
}


const getStatusType = (status) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILURE') return 'danger'
  return 'info'
}

const formatTime = (time) => {
  if (!time) return 'N/A'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(() => {
  loadProjects()
  loadReports()
})
</script>

<style scoped>
.ui-test-report-list {
  width: 100%;
}
.card-header {
  font-size: 18px;
  font-weight: bold;
}
.filter-section {
  margin-bottom: 20px;
}
</style>
