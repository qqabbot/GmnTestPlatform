<template>
  <div 
    class="step-node" 
    :class="{ 'is-selected': isSelected, 'is-container': isContainer }"
    @click.stop="$emit('select', step)"
  >
    <div class="node-header">
        <div class="node-icon">
            <el-icon v-if="step.type === 'CASE'"><Document /></el-icon>
            <el-icon v-else-if="step.type === 'GROUP'"><Folder /></el-icon>
            <el-icon v-else-if="step.type === 'LOOP'"><Refresh /></el-icon>
            <el-icon v-else-if="step.type === 'IF'"><Operation /></el-icon>
             <el-icon v-else-if="step.type === 'WAIT'"><Timer /></el-icon>
        </div>
        <div class="node-info">
            <span class="node-type">{{ step.type }}</span>
            <span class="node-name">{{ step.name || step.type }}</span>
        </div>
        <div class="node-actions">
            <div v-if="step.status" class="status-icon" :class="step.status.toLowerCase()">
                <el-icon v-if="step.status === 'RUNNING'" class="is-loading"><Loading /></el-icon>
                <el-icon v-else-if="step.status === 'PASS'"><Check /></el-icon>
                <el-icon v-else-if="step.status === 'FAIL' || step.status === 'ERROR'"><Close /></el-icon>
            </div>
            <el-button 
                v-if="isStepDisabled" 
                link 
                type="warning" 
                size="small" 
                icon="VideoPause" 
                title="Step is disabled"
            />
            <el-dropdown trigger="click" @command="handleAction">
                <el-button link type="primary" size="small" icon="MoreFilled" />
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item command="copy" icon="DocumentCopy">Copy</el-dropdown-item>
                        <el-dropdown-item command="paste" icon="DocumentAdd" :disabled="!canPaste">Paste</el-dropdown-item>
                        <el-dropdown-item command="toggleEnabled" :icon="isStepDisabled ? 'VideoPlay' : 'VideoPause'">
                            {{ isStepDisabled ? 'Enable' : 'Disable' }}
                        </el-dropdown-item>
                        <el-dropdown-item divided command="remove" icon="Delete" type="danger">Delete</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
    </div>
    
    <!-- Recursive Children for Containers -->
    <div v-if="isContainer" class="node-children">
        <draggable 
            class="nested-list"
            :list="step.children" 
            group="steps" 
            item-key="tempId"
            ghost-class="ghost-node"
        >
            <template #item="{ element, index }">
                <step-node 
                    :step="element" 
                    :steps="step.children"
                    :index="index"
                    :selected-id="selectedId"
                    @select="(s) => $emit('select', s)"
                    @remove="handleRemoveChild"
                    @copy="(s) => $emit('copy', s)"
                    @paste="(copied, target, idx) => $emit('paste', copied, target, idx)"
                    @toggleEnabled="(s) => $emit('toggleEnabled', s)"
                />
            </template>
        </draggable>
        <div v-if="!step.children || step.children.length === 0" class="empty-children">
            Drop items here
        </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, inject } from 'vue'
import draggable from 'vuedraggable'
import { Document, Folder, Refresh, Operation, Timer, Close, Loading, Check, CircleClose, MoreFilled, DocumentCopy, DocumentAdd, VideoPlay, VideoPause, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
    step: Object,
    index: Number,
    selectedId: [String, Number],
    steps: Array // Parent's list
})

const emit = defineEmits(['select', 'remove', 'copy', 'paste', 'toggleEnabled'])

// Clipboard for copy/paste
const clipboard = inject('stepClipboard', ref(null))
const canPaste = computed(() => clipboard.value !== null)

const isSelected = computed(() => {
    // robust check, use tempId if id missing (new node)
    const myId = props.step.id || props.step.tempId
    return props.selectedId && props.selectedId === myId
})

const isContainer = computed(() => {
    return ['GROUP', 'LOOP', 'IF'].includes(props.step.type)
})

const handleRemoveChild = (childStep) => {
    const idx = props.step.children.indexOf(childStep)
    if (idx > -1) {
        props.step.children.splice(idx, 1)
    }
}

const isStepDisabled = computed(() => {
    if (!props.step.dataOverrides) return false
    return props.step.dataOverrides.enabled === false
})

const handleAction = (command) => {
    switch (command) {
        case 'copy':
            clipboard.value = JSON.parse(JSON.stringify(props.step)) // Deep copy
            ElMessage.success('Step copied')
            emit('copy', props.step)
            break
        case 'paste':
            if (clipboard.value) {
                const copied = JSON.parse(JSON.stringify(clipboard.value))
                // Generate new tempId for pasted step
                copied.tempId = 'temp_' + Date.now() + '_' + Math.random()
                copied.id = null // Clear ID so it's treated as new
                // Clear children IDs as well
                if (copied.children) {
                    copied.children.forEach(child => {
                        child.tempId = 'temp_' + Date.now() + '_' + Math.random()
                        child.id = null
                    })
                }
                emit('paste', copied, props.step, props.index)
                ElMessage.success('Step pasted')
            }
            break
        case 'toggleEnabled':
            if (!props.step.dataOverrides) {
                props.step.dataOverrides = {}
            }
            props.step.dataOverrides.enabled = !isStepDisabled.value
            emit('toggleEnabled', props.step)
            ElMessage.success(isStepDisabled.value ? 'Step disabled' : 'Step enabled')
            break
        case 'remove':
            emit('remove', props.step)
            break
    }
}
</script>

<style scoped>
.step-node {
    background: #fff;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    margin-bottom: 8px;
    position: relative;
    cursor: pointer;
    overflow: hidden;
}
.step-node.is-selected {
    border-color: #409eff;
    box-shadow: 0 0 0 2px rgba(64,158,255, 0.2);
}
.step-node:has(.step-disabled) {
    opacity: 0.6;
    background: #f5f7fa;
}
.node-header {
    display: flex;
    align-items: center;
    padding: 8px 12px;
    background: #fff;
}
.is-container > .node-header {
    background: #f5f7fa;
    border-bottom: 1px solid #ebeef5;
}

.node-icon {
    margin-right: 10px;
    color: #606266;
    display: flex;
    align-items: center;
}
.node-info {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
}
.node-type {
    font-size: 10px;
    background: #f0f2f5;
    padding: 2px 4px;
    border-radius: 2px;
    color: #909399;
}
.node-name {
    font-size: 13px;
    font-weight: 500;
}
.node-children {
    padding: 10px 10px 10px 20px; /* Indent */
    background: #fcfcfc;
    min-height: 40px;
}
.empty-children {
    font-size: 12px;
    color: #dcdfe6;
    padding: 10px;
    text-align: center;
    border: 1px dashed #ebeef5;
    border-radius: 4px;
}
.node-actions {
    display: flex;
    align-items: center;
    gap: 8px;
}
.status-icon {
    display: flex;
    align-items: center;
    font-size: 14px;
}
.status-icon.running { color: #eb9e05; }
.status-icon.pass { color: #67c23a; }
.status-icon.fail, .status-icon.error { color: #f56c6c; }

.nested-list {
    min-height: 20px;
}
</style>
