<template>
  <div class="resource-library">
    <div class="panel-header">
      <h3>Library</h3>
      <el-input v-model="search" placeholder="Search cases..." prefix-icon="Search" size="small" />
    </div>

    <div class="library-content">
      <!-- Logic Nodes Section -->
      <div class="section-title">Logic Nodes</div>
      <draggable 
        class="node-list logic-list" 
        :list="logicNodes" 
        :group="{ name: 'steps', pull: 'clone', put: false }" 
        :sort="false"
        item-key="type"
        :clone="cloneLogicNode"
      >
        <template #item="{ element }">
          <div class="node-item logic-item">
            <el-icon><component :is="element.icon" /></el-icon>
            <span>{{ element.label }}</span>
          </div>
        </template>
      </draggable>

      <!-- Test Cases Section -->
      <div class="section-title mt-4">Test Cases</div>
      <div v-loading="loading" class="case-list-wrapper">
         <draggable 
            class="node-list case-list" 
            :list="filteredCases" 
            :group="{ name: 'steps', pull: 'clone', put: false }" 
            :sort="false"
            item-key="id"
            :clone="cloneTestCase"
         >
           <template #item="{ element }">
             <div class="node-item case-item">
               <el-tag size="small" :type="getMethodColor(element.method)">{{ element.method }}</el-tag>
               <span class="case-name" :title="element.caseName">{{ element.caseName }}</span>
             </div>
           </template>
         </draggable>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import draggable from 'vuedraggable'
import { Search, Folder, Refresh, Operation, Timer } from '@element-plus/icons-vue'
import { testCaseApi } from '../../api/testCase'

const props = defineProps({
  projectId: {
    type: Number,
    default: null
  }
})

const search = ref('')
const loading = ref(false)
const testCases = ref([])

const logicNodes = [
  { type: 'GROUP', label: 'Group', icon: 'Folder' },
  { type: 'LOOP', label: 'Loop', icon: 'Refresh' },
  { type: 'IF', label: 'If Condition', icon: 'Operation' },
  { type: 'WAIT', label: 'Wait', icon: 'Timer' }
]

onMounted(async () => {
  if (props.projectId) {
    loadTestCases()
  }
})

watch(() => props.projectId, (newVal) => {
  if (newVal) {
    loadTestCases()
  } else {
    testCases.value = []
  }
})

const loadTestCases = async () => {
  if (!props.projectId) return
  
  loading.value = true
  try {
    const res = await testCaseApi.getByProject(props.projectId, 0, 100, '')
    testCases.value = res.cases || []
  } catch (e) {
    console.error(e)
    testCases.value = []
  } finally {
    loading.value = false
  }
}

const filteredCases = computed(() => {
  if (!search.value) return testCases.value
  return testCases.value.filter(c => c.caseName.toLowerCase().includes(search.value.toLowerCase()))
})

const getMethodColor = (method) => {
  const map = { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }
  return map[method] || 'info'
}

// Cloning logic
const cloneTestCase = (original) => {
  // Pre-populate dataOverrides with original Test Case configuration
  const initialOverrides = {}
  
  // Include original params if they exist
  if (original.params) {
    try {
      initialOverrides.params = typeof original.params === 'string' 
        ? JSON.parse(original.params) 
        : original.params
    } catch (e) {
      console.warn('Failed to parse original params:', e)
    }
  }
  
  // Include original headers if they exist
  if (original.headers) {
    try {
      initialOverrides.headers = typeof original.headers === 'string' 
        ? JSON.parse(original.headers) 
        : original.headers
    } catch (e) {
      console.warn('Failed to parse original headers:', e)
    }
  }
  
  // Include original body if it exists
  if (original.body) {
    try {
      initialOverrides.body = typeof original.body === 'string' 
        ? JSON.parse(original.body) 
        : original.body
    } catch (e) {
      console.warn('Failed to parse original body:', e)
    }
  }
  
  // Include Pre-request Script (setupScript) if it exists
  if (original.setupScript) {
    initialOverrides.setupScript = original.setupScript
  }
  
  // Include Global Assertion (assertionScript) if it exists
  if (original.assertionScript) {
    initialOverrides.assertionScript = original.assertionScript
  }
  
  return {
    isNew: true,
    type: 'CASE',
    name: original.caseName,
    referenceCaseId: original.id,
    children: [],
    controlLogic: {},
    dataOverrides: initialOverrides,
    tempId: Date.now()
  }
}

const cloneLogicNode = (original) => {
  return {
    isNew: true,
    type: original.type,
    name: original.label,
    referenceCaseId: null,
    children: [],
    controlLogic: {},
    dataOverrides: {},
    tempId: Date.now()
  }
}
</script>

<style scoped>
.resource-library {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #dcdfe6;
}
.panel-header {
  padding: 10px 15px;
  border-bottom: 1px solid #f0f2f5;
}
.library-content {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}
.section-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  font-weight: 600;
  text-transform: uppercase;
}
.mt-4 { margin-top: 16px; }
.node-item {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: grab;
  transition: all 0.2s;
  font-size: 13px;
}
.node-item:hover {
  background: #ecf5ff;
  border-color: #c6e2ff;
  color: #409eff;
}
.logic-item {
  gap: 8px;
}
.case-item {
  gap: 6px;
}
.case-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}
</style>
