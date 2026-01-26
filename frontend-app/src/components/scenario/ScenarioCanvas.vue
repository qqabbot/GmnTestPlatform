<template>
  <div class="scenario-canvas">
    <div v-if="!scenarioData.id" class="canvas-empty">
      <el-empty description="Select or create a scenario to start editing" />
    </div>
    <div v-else class="canvas-content">
        <div class="canvas-header">
            <h3>{{ scenarioData.name }}</h3>
            <div class="actions">
                <el-button type="info" size="small" icon="Timer" @click="$emit('history')">History</el-button>
                <el-button type="primary" size="small" icon="VideoPlay" @click="$emit('run')">Run</el-button>
                <el-button type="success" size="small" icon="Check" @click="$emit('save')">Save</el-button>
            </div>
        </div>
        
        <div class="tree-container">
             <!-- Root List -->
            <draggable 
                class="step-list root-list"
                :list="localSteps" 
                group="steps" 
                item-key="tempId"
                ghost-class="ghost-node"
                @change="handleChange"
            >
                <template #item="{ element, index }">
                    <step-node 
                        :step="element" 
                        :index="index"
                        :selected-id="selectedStepId"
                        @select="handleSelect"
                        @remove="handleRemove"
                        @copy="handleCopy"
                        @paste="handlePaste"
                        @toggleEnabled="handleToggleEnabled"
                    />
                </template>
            </draggable>

            <div v-if="localSteps.length === 0" class="drop-placeholder">
                Drag items here from the library
            </div>
        </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, provide, defineAsyncComponent } from 'vue'
import draggable from 'vuedraggable'
import { VideoPlay, Check } from '@element-plus/icons-vue'

// Avoid circular dependency in recursive component by registering async
const StepNode = defineAsyncComponent(() => import('./StepNode.vue'))

const props = defineProps({
    scenarioData: {
        type: Object,
        default: () => ({})
    },
    steps: {
        type: Array,
        default: () => []
    },
    selectedStepId: [String, Number]
})

const emit = defineEmits(['update:steps', 'select', 'save', 'run', 'history'])

const localSteps = ref([])

// Clipboard for copy/paste (provide to child components)
const stepClipboard = ref(null)
provide('stepClipboard', stepClipboard)

watch(() => props.steps, (newVal) => {
    localSteps.value = newVal || []
}, { immediate: true, deep: true })

const handleChange = () => {
    emit('update:steps', localSteps.value)
}

const handleSelect = (step) => {
    emit('select', step)
}

const handleRemove = (step) => {
    removeStepRecursively(localSteps.value, step)
    handleChange()
}

const handleCopy = (step) => {
    // Clipboard is handled in StepNode
}

const handlePaste = (copiedStep, targetStep, targetIndex) => {
    if (targetStep) {
        // Paste into target step's children (if it's a container)
        if (['GROUP', 'LOOP', 'IF'].includes(targetStep.type)) {
            if (!targetStep.children) {
                targetStep.children = []
            }
            targetStep.children.push(copiedStep)
        } else {
            // Paste after target step
            const parentList = findStepParentList(localSteps.value, targetStep)
            if (parentList) {
                const idx = parentList.indexOf(targetStep)
                parentList.splice(idx + 1, 0, copiedStep)
            }
        }
    } else {
        // Paste at root level after targetIndex
        localSteps.value.splice(targetIndex + 1, 0, copiedStep)
    }
    handleChange()
}

const handleToggleEnabled = (step) => {
    // Step enabled state is already updated in StepNode
    handleChange()
}

const removeStepRecursively = (list, step) => {
    const idx = list.indexOf(step)
    if (idx > -1) {
        list.splice(idx, 1)
        return true
    }
    // Search in children
    for (const item of list) {
        if (item.children) {
            if (removeStepRecursively(item.children, step)) {
                return true
            }
        }
    }
    return false
}

const findStepParentList = (list, targetStep) => {
    if (list.includes(targetStep)) {
        return list
    }
    for (const item of list) {
        if (item.children) {
            const found = findStepParentList(item.children, targetStep)
            if (found) return found
        }
    }
    return null
}
</script>

<style scoped>
.scenario-canvas {
    height: 100%;
    background-color: #f0f2f5;
    display: flex;
    flex-direction: column;
}
.canvas-header {
    background: #fff;
    padding: 10px 20px;
    border-bottom: 1px solid #dcdfe6;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.tree-container {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
}
.step-list {
    min-height: 200px;
    padding-bottom: 50px;
}
.drop-placeholder {
    margin-top: -150px;
    text-align: center;
    color: #c0c4cc;
    pointer-events: none;
    padding: 40px;
    border: 2px dashed #e4e7ed;
    border-radius: 8px;
}
.ghost-node {
    opacity: 0.5;
    background: #e6f7ff;
    border: 1px dashed #1890ff;
}
</style>
