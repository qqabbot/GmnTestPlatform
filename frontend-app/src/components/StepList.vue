<template>
  <div class="step-list">
    <div class="list-header">
      <span>Steps</span>
      <el-button type="primary" size="small" @click="$emit('add')">
        <el-icon><Plus /></el-icon> Add
      </el-button>
      <el-button type="info" size="small" @click="$emit('open-library')" style="margin-left: 5px;" plain>
        <el-icon><Collection /></el-icon> Library
      </el-button>
    </div>
    
    <draggable 
      :modelValue="steps" 
      @update:modelValue="handleUpdate"
      :item-key="(item) => item.id || item.stepOrder"
      handle=".drag-handle"
      class="list-container"
      :clone="(item) => ({ ...item })"
    >
      <template #item="{ element, index }">
        <div 
          class="step-item" 
          :class="{ active: selectedIndex === index, 'disabled-step': element.enabled === false }"
          @click="$emit('select', index)"
        >
          <div class="drag-handle">
            <el-icon><Rank /></el-icon>
          </div>
          <div class="step-content">
            <div class="step-title">
              <el-tag size="small" :type="getMethodType(element.method)" effect="plain" style="width: 50px; text-align: center;">{{ element.method }}</el-tag>
              <span class="step-name">{{ element.stepName || 'New Step' }}</span>
            </div>
            <div class="step-url">{{ element.url || '/' }}</div>
          </div>
          <div class="step-actions">
            <!-- Stop propagation to prevent selecting the step when toggling -->
            <el-switch 
              v-model="element.enabled" 
              size="small" 
              @click.stop
              @change="(val) => $emit('update-step', index, { enabled: val })"
            />
            <el-button type="danger" link size="small" @click.stop="$emit('remove', index)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </template>
    </draggable>

    <div v-if="!steps || steps.length === 0" class="empty-state">
      No steps added. Click "Add" to start.
    </div>
  </div>
</template>

<script setup>
import draggable from 'vuedraggable'

const props = defineProps({
  steps: {
    type: Array,
    required: true
  },
  selectedIndex: {
    type: Number,
    default: -1
  }
})

const emit = defineEmits(['update:steps', 'select', 'add', 'remove', 'update-step', 'open-library'])

const handleUpdate = (newSteps) => {
  // 更新stepOrder
  const updatedSteps = newSteps.map((step, index) => ({
    ...step,
    stepOrder: index + 1
  }))
  emit('update:steps', updatedSteps)
}

const getMethodType = (method) => {
  const types = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger'
  }
  return types[method] || 'info'
}
</script>

<style scoped>
.step-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #dcdfe6;
  background-color: #fff;
}

.list-header {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  background-color: #f5f7fa;
  font-weight: 600;
  color: #606266;
}

.list-container {
  flex: 1;
  overflow-y: auto;
}

.step-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.step-item:hover {
  background-color: #f5f7fa;
}

.step-item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
  padding-left: 13px; /* Compensate for border */
}

.step-item.disabled-step {
  opacity: 0.6;
  background-color: #fafafa;
}

.drag-handle {
  cursor: grab;
  margin-right: 12px;
  color: #c0c4cc;
  display: flex;
  align-items: center;
}

.drag-handle:hover {
  color: #909399;
}

.step-content {
  flex: 1;
  overflow: hidden;
  margin-right: 10px;
}

.step-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.step-name {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #303133;
}

.step-url {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.step-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.step-item:hover .step-actions,
.step-item.active .step-actions {
  opacity: 1;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
</style>
