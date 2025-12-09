<template>
  <div class="test-report">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>Test Reports</span>
          <el-button @click="loadReports">
            <el-icon><Refresh /></el-icon>
            Refresh
          </el-button>
        </div>
      </template>

      <!-- Filter -->
      <div class="filter-section">
        <el-select v-model="filterType" placeholder="Filter By" style="width: 150px; margin-right: 10px">
          <el-option label="All" value="all" />
          <el-option label="By Project" value="project" />
          <el-option label="By Module" value="module" />
        </el-select>
        <el-select v-if="filterType === 'project'" v-model="selectedProjectId" placeholder="Select Project" style="width: 200px; margin-right: 10px" @change="loadReports">
          <el-option v-for="proj in projects" :key="proj.id" :label="proj.projectName" :value="proj.id" />
        </el-select>
        <el-select v-if="filterType === 'module'" v-model="selectedModuleId" placeholder="Select Module" style="width: 200px; margin-right: 10px" @change="loadReports">
          <el-option v-for="mod in modules" :key="mod.id" :label="mod.moduleName" :value="mod.id" />
        </el-select>
      </div>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" class="report-tabs">
        <el-tab-pane label="Dashboard" name="dashboard">
          <!-- Summary Cards -->
          <div class="summary-cards">
            <el-card class="stat-card pass">
              <div class="stat-value">{{ passCount }}</div>
              <div class="stat-label">Passed</div>
            </el-card>
            <el-card class="stat-card fail">
              <div class="stat-value">{{ failCount }}</div>
              <div class="stat-label">Failed</div>
            </el-card>
            <el-card class="stat-card total">
              <div class="stat-value">{{ totalCount }}</div>
              <div class="stat-label">Total</div>
            </el-card>
            <el-card class="stat-card rate">
              <div class="stat-value">{{ passRate }}%</div>
              <div class="stat-label">Pass Rate</div>
            </el-card>
          </div>

          <!-- Charts -->
          <el-row :gutter="20" style="margin-top: 20px;">
            <el-col :span="12">
              <el-card header="Pass/Fail Distribution">
                <v-chart class="chart" :option="pieOption" autoresize />
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card header="Execution Trend (Last 7 Days)">
                 <!-- Placeholder for trend data, using dummy data for now as backend might not support historical aggregation yet -->
                <v-chart class="chart" :option="lineOption" autoresize />
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="Execution History" name="list">
          <!-- Results Table -->
          <el-table :data="reports" style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="caseName" label="Case Name" min-width="200" />
            <el-table-column label="Status" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'PASS' ? 'success' : 'danger'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="detail" label="Detail" min-width="250" show-overflow-tooltip />
            <el-table-column prop="duration" label="Duration (ms)" width="120" />
            <el-table-column prop="executedAt" label="Executed At" width="180" />
            <el-table-column label="Actions" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewDetail(row)">View</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <report-detail
      v-model:visible="showDetail"
      :report="currentReport"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { reportApi } from '../api/report'
import { projectApi } from '../api/project'
import { testModuleApi } from '../api/testModule'
import ReportDetail from '../components/ReportDetail.vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  PieChart,
  LineChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const loading = ref(false)
const filterType = ref('all')
const selectedProjectId = ref(null)
const selectedModuleId = ref(null)
const activeTab = ref('dashboard')
const showDetail = ref(false)
const currentReport = ref(null)
const reports = ref([])
const projects = ref([])
const modules = ref([])

const totalCount = computed(() => reports.value.length)
const passCount = computed(() => reports.value.filter(r => r.status === 'PASS').length)
const failCount = computed(() => reports.value.filter(r => r.status !== 'PASS').length)
const passRate = computed(() => totalCount.value > 0 ? Math.round((passCount.value / totalCount.value) * 100) : 0)

const pieOption = computed(() => ({
  tooltip: {
    trigger: 'item'
  },
  legend: {
    top: '5%',
    left: 'center'
  },
  series: [
    {
      name: 'Status',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 20,
          fontWeight: 'bold'
        }
      },
      labelLine: {
        show: false
      },
      data: [
        { value: passCount.value, name: 'Passed', itemStyle: { color: '#67c23a' } },
        { value: failCount.value, name: 'Failed', itemStyle: { color: '#f56c6c' } }
      ]
    }
  ]
}))

const lineOption = computed(() => ({
  xAxis: {
    type: 'category',
    data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      data: [820, 932, 901, 934, 1290, 1330, 1320], // Dummy data
      type: 'line',
      smooth: true
    }
  ]
}))

const viewDetail = (row) => {
  currentReport.value = row
  showDetail.value = true
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

const loadReports = async () => {
  loading.value = true
  try {
    if (filterType.value === 'project' && selectedProjectId.value) {
      reports.value = await reportApi.getByProject(selectedProjectId.value)
    } else if (filterType.value === 'module' && selectedModuleId.value) {
      reports.value = await reportApi.getByModule(selectedModuleId.value)
    } else {
      reports.value = await reportApi.getAll()
    }
  } catch (error) {
    ElMessage.error('Failed to load reports')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProjects()
  loadModules()
  loadReports()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-section {
  margin-bottom: 20px;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.stat-card.pass .stat-value {
  color: #67c23a;
}

.stat-card.fail .stat-value {
  color: #f56c6c;
}

.stat-card.total .stat-value {
  color: #409eff;
}

.stat-card.rate .stat-value {
  color: #e6a23c;
}

.chart {
  height: 300px;
}

</style>
