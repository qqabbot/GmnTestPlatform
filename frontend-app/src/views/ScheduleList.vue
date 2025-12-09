<template>
  <div class="schedule-list">
    <div class="header">
      <h2>Scheduled Tasks</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon> New Schedule
      </el-button>
    </div>

    <el-table :data="tasks" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="Task Name" />
      <el-table-column prop="cronExpression" label="Cron" width="150" />
      <el-table-column prop="planId" label="Plan ID" width="100" />
      <el-table-column prop="envKey" label="Env" width="100" />
      <el-table-column prop="status" label="Status" width="100">
        <template #default="{ row }">
           <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'warning'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="nextRunTime" label="Next Run" width="180">
        <template #default="{ row }">
           {{ formatDate(row.nextRunTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="lastRunTime" label="Last Run" width="180">
        <template #default="{ row }">
           {{ formatDate(row.lastRunTime) }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="250" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'ACTIVE'" type="warning" size="small" @click="pauseTask(row)">Pause</el-button>
          <el-button v-else type="success" size="small" @click="resumeTask(row)">Resume</el-button>
          
          <el-button type="primary" size="small" @click="openEditDialog(row)">Edit</el-button>
          <el-button type="danger" size="small" @click="deleteTask(row)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? 'Edit Schedule' : 'New Schedule'" width="500px">
      <el-form label-width="120px">
        <el-form-item label="Task Name" required>
           <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Test Plan" required>
           <el-select v-model="form.planId" placeholder="Select Plan" style="width: 100%">
             <el-option v-for="p in plans" :key="p.id" :label="p.name + ' (ID:' + p.id + ')'" :value="p.id" />
           </el-select>
        </el-form-item>
        <el-form-item label="Environment" required>
           <el-select v-model="form.envKey" placeholder="Select Env" style="width: 100%">
             <el-option v-for="env in environments" :key="env.id" :label="env.envName" :value="env.envName" />
           </el-select>
        </el-form-item>
        <el-form-item label="Cron Expression" required>
           <el-input v-model="form.cronExpression" placeholder="e.g. 0 0/10 * * * ?" />
           <div class="help-text">
             Examples: <br>
             Every 30s: 0/30 * * * * ? <br>
             Every day 9am: 0 0 9 * * ?
           </div>
        </el-form-item>
      </el-form>
      <template #footer>
         <el-button @click="dialogVisible = false">Cancel</el-button>
         <el-button type="primary" @click="saveTask">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { scheduleApi } from '../api/schedule'
import { testPlanApi } from '../api/testPlan'
import { environmentApi } from '../api/environment'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tasks = ref([])
const plans = ref([])
const environments = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)

const form = reactive({
    id: null,
    name: '',
    planId: null,
    envKey: null,
    cronExpression: ''
})

onMounted(async () => {
    loadData()
    // Load options
    try {
       plans.value = await testPlanApi.getAll()
       environments.value = await environmentApi.getAll()
    } catch (e) {}
})

const loadData = async () => {
    loading.value = true
    try {
        tasks.value = await scheduleApi.getAll()
    } catch (e) {
        ElMessage.error('Failed to load schedules')
    } finally {
        loading.value = false
    }
}

const formatDate = (dateStr) => {
    if(!dateStr) return '-'
    // Basic formatting or use moment.js if available. 
    // Backend sends ISO/Timestamp array? Usually LocalDateTime is Serialization dependent.
    // If array: new Date(y, m-1, ...). If string ISO: new Date(str).
    // Let's assume ISO string for now.
    return new Date(dateStr).toLocaleString()
}

const openCreateDialog = () => {
    isEdit.value = false
    Object.assign(form, { id: null, name: '', planId: null, envKey: null, cronExpression: '' })
    dialogVisible.value = true
}

const openEditDialog = (row) => {
    isEdit.value = true
    Object.assign(form, row)
    dialogVisible.value = true
}

const saveTask = async () => {
    if (!form.name || !form.planId || !form.envKey || !form.cronExpression) {
        ElMessage.warning('All fields are required')
        return
    }
    
    try {
        if(isEdit.value) {
            await scheduleApi.update(form.id, form)
        } else {
            await scheduleApi.create(form)
        }
        ElMessage.success('Saved')
        dialogVisible.value = false
        loadData()
    } catch(e) {
        ElMessage.error('Failed to save')
    }
}

const pauseTask = async (row) => {
    try {
        await scheduleApi.pause(row.id)
        ElMessage.success('Paused')
        loadData()
    } catch(e) { ElMessage.error('Failed') }
}

const resumeTask = async (row) => {
    try {
        await scheduleApi.resume(row.id)
        ElMessage.success('Resumed')
        loadData()
    } catch(e) { ElMessage.error('Failed') }
}

const deleteTask = async (row) => {
    try {
        await ElMessageBox.confirm('Confirm delete?')
        await scheduleApi.delete(row.id)
        ElMessage.success('Deleted')
        loadData()
    } catch(e) {}
}
</script>

<style scoped>
.schedule-list {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.5;
}
</style>
