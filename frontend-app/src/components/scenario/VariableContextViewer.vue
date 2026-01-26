<template>
  <div class="variable-viewer">
    <div class="viewer-header">
      <el-icon><Menu /></el-icon>
      <span class="title">Runtime Variables</span>
    </div>
    <div class="viewer-content">
      <el-table :data="variableList" size="small" style="width: 100%" height="100%">
        <el-table-column prop="key" label="Key" show-overflow-tooltip />
        <el-table-column prop="value" label="Value" show-overflow-tooltip>
          <template #default="scope">
            <span class="var-value">{{ formatValue(scope.row.value) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="variableList.length === 0" class="empty-state">
        No variables in current context
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Menu } from '@element-plus/icons-vue'

const props = defineProps({
  variables: {
    type: Object,
    default: () => ({})
  }
})

const variableList = computed(() => {
  return Object.entries(props.variables).map(([key, value]) => ({
    key,
    value
  }))
})

const formatValue = (val) => {
  if (val === null || val === undefined) return 'null'
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}
</script>

<style scoped>
.variable-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-left: 1px solid #ebeef5;
}

.viewer-header {
  height: 48px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  font-size: 14px;
  background: #f8f9fa;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
}

.viewer-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #909399;
  font-size: 12px;
}

.var-value {
  color: #e67e22;
  font-family: inherit;
  font-size: 12px;
}
</style>
