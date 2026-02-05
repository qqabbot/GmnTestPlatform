<template>
  <div class="dashboard-env-container">
    <!-- Sidebar: Environment List -->
    <div class="env-sidebar">
      <div class="sidebar-header">
        <h2>Environments</h2>
        <el-button 
          type="primary" 
          circle 
          class="add-btn"
          @click="openEnvDialog()"
        >
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>

      <div class="env-list" v-loading="loading">
        <div 
          v-for="env in tree" 
          :key="env.id"
          class="env-item"
          :class="{ active: activeEnvId === env.id }"
          @click="selectEnv(env.id)"
        >
          <div class="env-status" :style="{ backgroundColor: getEnvColor(env.name) }"></div>
          <div class="env-info">
            <div class="env-name">{{ env.name }}</div>
            <div class="env-domain">{{ env.description}}</div>
          </div>
          <div class="env-actions">
            <el-dropdown trigger="click" @command="(cmd) => handleEnvCommand(cmd, env)">
              <el-button link class="more-btn" @click.stop>
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">
                    <el-icon><Edit /></el-icon> 编辑
                  </el-dropdown-item>
                  <el-dropdown-item divided command="delete" style="color: #f56c6c">
                    <el-icon><Delete /></el-icon> 删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content: Addresses -->
    <div class="variable-workspace">
      <div v-if="currentEnv" class="workspace-content">
        <header class="content-header">
          <div class="header-left">
            
            <h1>Navigations</h1>
          </div>
          <div class="header-right">
            <el-button type="primary" size="small" @click="openAddrDialog(currentEnv)">
              <el-icon><Plus /></el-icon> 新增地址
            </el-button>
          </div>
        </header>

        <div class="filter-section" v-if="availableLabels.length > 0">
          <div class="filter-pills">
            <div
              v-for="label in availableLabels"
              :key="label"
              class="filter-pill"
              :class="{ active: activeLabel === label }"
              @click="activeLabel = label"
            >
              {{ label }}
            </div>
          </div>
        </div>

        <div class="workspace-body">
          <div v-if="displayAddresses.length === 0" class="empty-content">
            <el-empty description="当前分类下暂无地址" />
          </div>
          
          <div v-else class="address-grid">
            <div 
              v-for="addr in displayAddresses" 
              :key="addr.id" 
              class="address-card"
              :class="{ 'is-clickable': addr.url }"
              @click="addr.url && window.open(addr.url, '_blank')"
            >
              <div class="card-actions">
                <el-button link size="small" @click.stop="openAddrDialog(currentEnv, addr)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button link type="danger" size="small" @click.stop="deleteAddr(addr)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>

              <div class="card-badges">
                <span class="short-name-badge">{{ addr.shortName }}</span>
              </div>
              
            
              
              <div class="addr-url" v-if="addr.url">
                <span class="url-text">{{ addr.url }}</span>
              </div>
              <div class="addr-remark" v-if="addr.remark">{{ addr.remark }}</div>

            
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="请从左侧选择一个环境" class="fancy-empty" />
    </div>

    <!-- Dialogs -->
    <el-dialog
      v-model="envDialogVisible"
      :title="editingEnv?.id ? '编辑环境' : '新增环境'"
      width="440px"
      custom-class="modern-dialog"
    >
      <el-form :model="envForm" label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="envForm.name" placeholder="如 Production, Staging" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="envForm.description" placeholder="可选描述" type="textarea" :rows="1" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="envForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="envDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEnv">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="addrDialogVisible"
      :title="editingAddr?.id ? '编辑地址' : '新增地址'"
      width="500px"
      custom-class="modern-dialog"
    >
      <el-form :model="addrForm" label-position="top" class="addr-form">
        <div class="form-row">
          <el-form-item label="国家" required class="col-6">
            <el-autocomplete
              v-model="addrForm.label"
              :fetch-suggestions="queryLabelSearch"
              placeholder=""
              clearable
            />
          </el-form-item>
          <el-form-item label="标题" required class="col-6">
            <el-input v-model="addrForm.shortName" placeholder="" />
          </el-form-item>
        </div>
        
        <el-form-item label="地址">
          <el-input v-model="addrForm.url" placeholder="https://..." />
        </el-form-item>
        
        <el-form-item label="备注">
          <el-input v-model="addrForm.remark" type="textarea" :rows="2" />
        </el-form-item>
        
        <el-form-item label="排序">
          <el-input-number v-model="addrForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="addrDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveAddr">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, MoreFilled, Edit, Delete, TopRight 
} from '@element-plus/icons-vue'
import { dashboardNavApi } from '../api/dashboardNav'

const loading = ref(false)
const tree = ref([])
const activeEnvId = ref(null)
const activeLabel = ref(null)

// Dialog states
const envDialogVisible = ref(false)
const editingEnv = ref(null)
const envForm = ref({ name: '', description: '', sortOrder: 0 })

const addrDialogVisible = ref(false)
const editingAddr = ref(null)
const addrFormEnv = ref(null)
const addrForm = ref({ shortName: '', label: '', url: '', remark: '', sortOrder: 0 })

// Computed
const currentEnv = computed(() => {
  return tree.value.find(e => e.id === activeEnvId.value)
})

const availableLabels = computed(() => {
  if (!currentEnv.value || !currentEnv.value.addresses) return []
  const labels = new Set(
    currentEnv.value.addresses
      .map(a => a.label || 'General')
      .filter(l => l)
  )
  return Array.from(labels).sort()
})

const displayAddresses = computed(() => {
  if (!currentEnv.value) return []
  let list = currentEnv.value.addresses || []
  
  if (activeLabel.value) {
    list = list.filter(a => (a.label || 'General') === activeLabel.value)
  }
  return list
})

// UI Helpers
const getEnvColor = (name) => {
  if (!name) return '#747d8c'
  const n = name.toLowerCase()
  if (n.includes('prod')) return '#ff4757'
  if (n.includes('staging') || n.includes('pre')) return '#ffa502'
  if (n.includes('qa') || n.includes('test')) return '#2e86de'
  if (n.includes('dev')) return '#2ed573'
  return '#747d8c'
}

// Methods
const loadTree = async () => {
  loading.value = true
  try {
    tree.value = await dashboardNavApi.getTree()
    if (!activeEnvId.value && tree.value.length > 0) {
      activeEnvId.value = tree.value[0].id
      if (availableLabels.value.length > 0) {
        activeLabel.value = availableLabels.value[0]
      }
    }
  } catch (e) {
    ElMessage.error('加载导航失败')
  } finally {
    loading.value = false
  }
}

const selectEnv = (id) => {
  activeEnvId.value = id
  nextTick(() => {
    if (availableLabels.value.length > 0) {
      activeLabel.value = availableLabels.value[0]
    } else {
      activeLabel.value = null
    }
  })
}

// Env Actions
const openEnvDialog = (env = null) => {
  editingEnv.value = env
  if (env) {
    envForm.value = { ...env }
  } else {
    envForm.value = { name: '', description: '', sortOrder: tree.value.length * 10 }
  }
  envDialogVisible.value = true
}

const handleEnvCommand = (cmd, env) => {
  if (cmd === 'edit') openEnvDialog(env)
  if (cmd === 'delete') deleteEnv(env)
}

const saveEnv = async () => {
  if (!envForm.value.name?.trim()) return ElMessage.warning('请输入名称')
  try {
    if (editingEnv.value) {
      await dashboardNavApi.updateEnvironment(editingEnv.value.id, envForm.value)
    } else {
      await dashboardNavApi.createEnvironment(envForm.value)
    }
    envDialogVisible.value = false
    loadTree()
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const deleteEnv = async (env) => {
  try {
    await ElMessageBox.confirm(`确定删除环境 ${env.name} 及其所有地址吗？`, '警告', {
      confirmButtonText: '确定删除',
      confirmButtonClass: 'el-button--danger',
      type: 'error'
    })
    await dashboardNavApi.deleteEnvironment(env.id)
    if (activeEnvId.value === env.id) activeEnvId.value = null
    loadTree()
  } catch(e) {}
}

// Addr Actions
const openAddrDialog = (env, addr = null) => {
  addrFormEnv.value = env
  editingAddr.value = addr
  if (addr) {
    addrForm.value = { ...addr }
  } else {
    addrForm.value = { 
      label: activeLabel.value || '',
      shortName: '', 
      url: '', 
      remark: '', 
      sortOrder: (env.addresses || []).length * 10 
    }
  }
  addrDialogVisible.value = true
}

const saveAddr = async () => {
  if (!addrForm.value.label?.trim()) return ElMessage.warning('请输入国家/地区')
  try {
    const payload = { ...addrForm.value }

    if (editingAddr.value) {
      await dashboardNavApi.updateAddress(editingAddr.value.id, payload)
    } else {
      await dashboardNavApi.createAddress(addrFormEnv.value.id, payload)
    }
    addrDialogVisible.value = false
    loadTree()
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const deleteAddr = async (addr) => {
  try {
    await ElMessageBox.confirm('确定删除该地址？')
    await dashboardNavApi.deleteAddress(addr.id)
    loadTree()
  } catch(e) {}
}

const queryLabelSearch = (queryString, cb) => {
  const commons = [
    { value: 'MY' },
    { value: 'ID' },
    { value: 'TH' }
  ]
  if (currentEnv.value) {
    currentEnv.value.addresses?.forEach(a => {
      if (a.label && !commons.some(c => c.value === a.label)) {
        commons.push({ value: a.label })
      }
    })
  }
  
  const results = queryString
    ? commons.filter(item => item.value.toLowerCase().includes(queryString.toLowerCase()))
    : commons
  cb(results)
}

const window = globalThis

onMounted(() => {
  loadTree()
})
</script>

<style scoped>
.dashboard-env-container {
  display: flex;
  height: calc(100vh - 100px);
  background: #fcfcfc;
  overflow: hidden;
  margin: -20px;
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
  min-width: 0;
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

/* Filter Section Styling */
.filter-section {
  background: #fff;
  padding: 16px 32px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #edf2f7;
}

.filter-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-pill {
  padding: 8px 20px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #edf2f7;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 90px;
}

.filter-pill:hover {
  border-color: #409eff;
  color: #409eff;
  background: #f0f7ff;
}

.filter-pill.active {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.3);
  transform: translateY(-1px);
}

.workspace-body {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
}

/* Address Card Design - Kept but refined */
.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.address-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #edf2f7;
  transition: all 0.25s;
  display: flex;
  flex-direction: column;
  position: relative;
}

.address-card.is-clickable {
  cursor: pointer;
}

.address-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.06);
  border-color: #d0e1fd;
}

.card-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  opacity: 0;
  transition: opacity 0.2s;
  display: flex;
  gap: 4px;
}

.address-card:hover .card-actions {
  opacity: 1;
}

.card-badges {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.short-name-badge {
  background-color: #f7fafc;
  color: #718096;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  text-transform: uppercase;
}

.addr-label {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
}

.addr-url {
  font-size: 12px;
  color: #718096;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: 'JetBrains Mono', monospace;
}

.addr-remark {
  font-size: 13px;
  color: #4a5568;
  margin-bottom: 16px;
  flex: 1;
  line-height: 1.5;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f7fafc;
}

.visit-link {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 4px;
}

.no-link-text {
  font-size: 12px;
  color: #a0aec0;
}

.fancy-empty {
  height: 100%;
}

/* Dialog Styles */
:deep(.modern-dialog) {
  border-radius: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.form-row {
  display: flex;
  gap: 16px;
}
.col-6 {
  flex: 1;
}
</style>
