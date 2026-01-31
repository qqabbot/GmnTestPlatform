<template>
  <div class="dashboard-nav">
    <!-- Hero header -->
    <header class="hero">
      <div class="hero-bg" />
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="hero-title-text">快捷入口</span>
          <span class="hero-subtitle">环境与常用地址，一键直达</span>
        </h1>
        <el-button type="primary" class="hero-cta" @click="openEnvDialog()">
          <el-icon><Plus /></el-icon>
          新增环境
        </el-button>
      </div>
    </header>

    <!-- Content -->
    <div v-loading="loading" class="nav-content">
      <!-- Empty state -->
      <div v-if="!loading && tree.length === 0" class="empty-state">
        <div class="empty-icon-wrap">
          <el-icon :size="80" class="empty-icon"><FolderOpened /></el-icon>
        </div>
        <p class="empty-title">暂无环境</p>
        <p class="empty-desc">点击上方「新增环境」添加第一个环境，再添加常用地址</p>
        <el-button type="primary" size="large" @click="openEnvDialog()">
          <el-icon><Plus /></el-icon>
          新增环境
        </el-button>
      </div>

      <!-- Environment cards grid -->
      <div v-else class="env-grid">
        <div
          v-for="(env, idx) in tree"
          :key="env.id"
          class="env-card"
          :style="{ animationDelay: `${idx * 0.08}s` }"
        >
          <div class="env-card-header">
            <div class="env-card-title-wrap">
              <div class="env-icon-wrap">
                <el-icon :size="24"><Folder /></el-icon>
              </div>
              <div>
                <h3 class="env-name">{{ env.name }}</h3>
                <p v-if="env.description" class="env-desc">{{ env.description }}</p>
              </div>
            </div>
            <div class="env-card-actions">
              <el-tooltip content="新增地址" placement="top">
                <el-button type="primary" circle size="small" @click="openAddrDialog(env)">
                  <el-icon><Plus /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="编辑环境" placement="top">
                <el-button type="info" circle size="small" link @click="openEnvDialog(env)">
                  <el-icon><Edit /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="删除环境" placement="top">
                <el-button type="danger" circle size="small" link @click="deleteEnv(env)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </div>
          <ul class="address-list">
            <li
              v-for="addr in (env.addresses || [])"
              :key="addr.id"
              class="address-item"
            >
              <a
                v-if="addr.url"
                :href="addr.url"
                target="_blank"
                rel="noopener noreferrer"
                class="address-link"
              >
                <span class="addr-icon"><el-icon><Link /></el-icon></span>
                <span class="short-name-badge">{{ addr.shortName }}</span>
                <span class="label">{{ addr.label }}</span>
                <span v-if="addr.url" class="url-preview">{{ addr.url }}</span>
                <el-icon class="open-icon"><TopRight /></el-icon>
              </a>
              <div v-else class="address-link no-url">
                <span class="addr-icon"><el-icon><Link /></el-icon></span>
                <span class="short-name-badge">{{ addr.shortName }}</span>
                <span class="label">{{ addr.label }}</span>
              </div>
              <div class="address-actions">
                <el-button type="primary" size="small" link @click="openAddrDialog(env, addr)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button type="danger" size="small" link @click="deleteAddr(addr)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </li>
            <li v-if="!(env.addresses && env.addresses.length)" class="address-empty">
              <el-icon><Link /></el-icon>
              <span>暂无地址，点击右上角 + 添加</span>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 环境 新增/编辑 -->
    <el-dialog
      v-model="envDialogVisible"
      :title="editingEnv?.id ? '编辑环境' : '新增环境'"
      width="480px"
      @close="editingEnv = null"
    >
      <el-form :model="envForm" label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="envForm.name" placeholder="如 A、B" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="envForm.description" placeholder="可选" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="envForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="envDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEnv">保存</el-button>
      </template>
    </el-dialog>

    <!-- 地址 新增/编辑 -->
    <el-dialog
      v-model="addrDialogVisible"
      :title="editingAddr?.id ? '编辑地址' : '新增地址'"
      width="520px"
      @close="editingAddr = null; addrFormEnv = null"
    >
      <el-form :model="addrForm" label-position="top">
        <el-form-item label="短名称" required>
          <el-input v-model="addrForm.shortName" placeholder="如 A1、A2" />
        </el-form-item>
        <el-form-item label="标签" required>
          <el-input v-model="addrForm.label" placeholder="如 my、th" />
        </el-form-item>
        <el-form-item label="URL">
          <el-input v-model="addrForm.url" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addrForm.remark" type="textarea" placeholder="可选" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="addrForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addrDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAddr">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, FolderOpened, Folder, Link, Edit, Delete, TopRight } from '@element-plus/icons-vue'
import { dashboardNavApi } from '../api/dashboardNav'

const loading = ref(false)
const tree = ref([])

const envDialogVisible = ref(false)
const editingEnv = ref(null)
const envForm = ref({ name: '', description: '', sortOrder: 0 })

const addrDialogVisible = ref(false)
const addrFormEnv = ref(null)
const editingAddr = ref(null)
const addrForm = ref({ shortName: '', label: '', url: '', remark: '', sortOrder: 0 })

async function loadTree() {
  loading.value = true
  try {
    tree.value = await dashboardNavApi.getTree()
  } catch (e) {
    ElMessage.error('加载导航失败')
  } finally {
    loading.value = false
  }
}

function openEnvDialog(env = null) {
  editingEnv.value = env
  if (env) {
    envForm.value = { name: env.name, description: env.description || '', sortOrder: env.sortOrder ?? 0 }
  } else {
    envForm.value = { name: '', description: '', sortOrder: tree.value.length }
  }
  envDialogVisible.value = true
}

async function saveEnv() {
  if (!envForm.value.name?.trim()) {
    ElMessage.warning('请输入环境名称')
    return
  }
  try {
    if (editingEnv.value?.id) {
      await dashboardNavApi.updateEnvironment(editingEnv.value.id, envForm.value)
      ElMessage.success('已更新')
    } else {
      await dashboardNavApi.createEnvironment(envForm.value)
      ElMessage.success('已添加')
    }
    envDialogVisible.value = false
    await loadTree()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function deleteEnv(env) {
  try {
    await ElMessageBox.confirm(
      `删除环境「${env.name}」将同时删除其下所有地址，确定删除？`,
      '确认删除',
      { type: 'warning' }
    )
    await dashboardNavApi.deleteEnvironment(env.id)
    ElMessage.success('已删除')
    await loadTree()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function openAddrDialog(env, addr = null) {
  addrFormEnv.value = env
  editingAddr.value = addr
  if (addr) {
    addrForm.value = {
      shortName: addr.shortName,
      label: addr.label,
      url: addr.url || '',
      remark: addr.remark || '',
      sortOrder: addr.sortOrder ?? 0
    }
  } else {
    addrForm.value = { shortName: '', label: '', url: '', remark: '', sortOrder: (env.addresses || []).length }
  }
  addrDialogVisible.value = true
}

async function saveAddr() {
  if (!addrForm.value.shortName?.trim() || !addrForm.value.label?.trim()) {
    ElMessage.warning('请输入短名称和标签')
    return
  }
  const envId = addrFormEnv.value?.id
  if (!envId) {
    return
  }
  try {
    if (editingAddr.value?.id) {
      await dashboardNavApi.updateAddress(editingAddr.value.id, addrForm.value)
      ElMessage.success('已更新')
    } else {
      await dashboardNavApi.createAddress(envId, addrForm.value)
      ElMessage.success('已添加')
    }
    addrDialogVisible.value = false
    await loadTree()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function deleteAddr(addr) {
  try {
    await ElMessageBox.confirm('确定删除该地址？', '确认删除', { type: 'warning' })
    await dashboardNavApi.deleteAddress(addr.id)
    ElMessage.success('已删除')
    await loadTree()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadTree()
})
</script>

<style scoped>
.dashboard-nav {
  min-height: 100vh;
  background: linear-gradient(160deg, #f0f4ff 0%, #e8eeff 40%, #f5f7fa 100%);
}

/* Hero header */
.hero {
  position: relative;
  padding: 40px 32px 36px;
  margin-bottom: 28px;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #5b6bc0 100%);
  opacity: 0.92;
}

.hero-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.06'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  opacity: 0.5;
}

.hero-content {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
}

.hero-title {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hero-title-text {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.02em;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.hero-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.88);
  font-weight: 400;
}

.hero-cta {
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.45);
  font-weight: 500;
  padding: 10px 20px;
}

.hero-cta:hover {
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.55);
  transform: translateY(-1px);
}

/* Content */
.nav-content {
  padding: 0 24px 40px;
  min-height: 320px;
}

/* Empty state */
.empty-state {
  text-align: center;
  padding: 80px 24px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(102, 126, 234, 0.08);
}

.empty-icon-wrap {
  width: 120px;
  height: 120px;
  margin: 0 auto 24px;
  background: linear-gradient(145deg, #e8ecff 0%, #f0f4ff 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
  opacity: 0.9;
}

.empty-icon {
  color: #667eea;
}

.empty-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.empty-desc {
  margin: 0 0 24px;
  font-size: 14px;
  color: #909399;
}

/* Environment grid */
.env-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.env-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  animation: cardIn 0.5s ease backwards;
}

.env-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(102, 126, 234, 0.12);
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.env-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 20px 16px;
  background: linear-gradient(180deg, #fafbff 0%, #fff 100%);
  border-bottom: 1px solid #f0f2f5;
}

.env-card-title-wrap {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.env-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(145deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.env-name {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.env-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.4;
}

.env-card-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

/* Address list */
.address-list {
  list-style: none;
  margin: 0;
  padding: 12px 16px 16px;
}

.address-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 10px;
  margin-bottom: 6px;
  transition: background 0.2s ease;
}

.address-item:last-child {
  margin-bottom: 0;
}

.address-item:hover {
  background: #f5f7fa;
}

.address-link {
  flex: 1;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  color: #409eff;
  font-size: 14px;
  transition: color 0.2s ease;
}

.address-link:hover {
  color: #66b1ff;
}

.address-link .open-icon {
  flex-shrink: 0;
  margin-left: 4px;
  opacity: 0.7;
  font-size: 14px;
}

.address-link.no-url {
  color: #606266;
  cursor: default;
}

.addr-icon {
  color: #909399;
  flex-shrink: 0;
  font-size: 16px;
}

.short-name-badge {
  flex-shrink: 0;
  min-width: 40px;
  padding: 2px 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, #e8ecff 0%, #f0f4ff 100%);
  color: #5b6bc0;
  font-size: 12px;
  font-weight: 600;
}

.label {
  flex-shrink: 0;
  color: #303133;
  font-weight: 500;
}

.url-preview {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.address-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
  opacity: 0.7;
}

.address-item:hover .address-actions {
  opacity: 1;
}

.address-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  color: #909399;
  font-size: 13px;
  border: 1px dashed #dcdfe6;
  border-radius: 10px;
  background: #fafbfc;
}

.address-empty .el-icon {
  font-size: 18px;
  color: #c0c4cc;
}
</style>
