<template>
  <div class="variable-input-wrapper" ref="wrapperRef">
    <el-input
      v-bind="$attrs"
      v-model="internalValue"
      @input="handleInput"
      ref="inputRef"
    >
      <template v-for="(_, name) in $slots" #[name]="slotData">
        <slot :name="name" v-bind="slotData" />
      </template>
    </el-input>

    <el-popover
      v-model:visible="showSuggestions"
      :virtual-ref="inputRef"
      trigger="manual"
      placement="bottom-start"
      width="300"
      popper-class="variable-popover"
    >
      <div v-if="suggestions.length > 0" class="suggestion-list">
        <div 
          v-for="(item, index) in suggestions" 
          :key="index"
          class="suggestion-item"
          :class="{ active: activeIndex === index }"
          @click="selectVariable(item)"
          @mouseover="activeIndex = index"
        >
          <span class="var-name">{{ item.name }}</span>
          <span class="var-value">{{ item.value }}</span>
        </div>
      </div>
      <div v-else class="empty-suggestions">No variables found</div>
    </el-popover>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { environmentApi } from '../api/environment'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const internalValue = ref(props.modelValue)
const showSuggestions = ref(false)
const inputRef = ref(null)
const wrapperRef = ref(null)
const suggestions = ref([])
const activeIndex = ref(0)
const variables = ref([])

watch(() => props.modelValue, (val) => {
  internalValue.value = val
})

watch(internalValue, (val) => {
  emit('update:modelValue', val)
})

onMounted(async () => {
  try {
    // Fetch environments to get variables
    // Optimization: Cache this in a store instead of fetching on every mount
    const envs = await environmentApi.getAll()
    const allVars = []
    
    // Add built-in variables
    allVars.push({ name: 'base_url', value: 'Base URL' })
    allVars.push({ name: 'timestamp', value: 'Current Timestamp' })
    
    // Flatten environment variables
    envs.forEach(env => {
      if (env.variables) {
        try {
          const vars = JSON.parse(env.variables)
          Object.keys(vars).forEach(key => {
            if (!allVars.find(v => v.name === key)) {
              allVars.push({ name: key, value: vars[key] || '', source: env.envName })
            }
          })
        } catch (e) {
          // ignore parse error
        }
      }
    })
    
    variables.value = allVars
  } catch (e) {
    console.error('Failed to load variables for autocomplete', e)
  }
})

const handleInput = (val) => {
  if (val && val.endsWith('$')) {
    // Trigger suggestions
    suggestions.value = variables.value
    showSuggestions.value = true
    activeIndex.value = 0
  } else if (showSuggestions.value) {
    // Determine if we are still typing a variable? 
    // Creating a robust parser is hard, for now simple trigger on $
    // If user types more, we could filter.
    // Let's hide if space is typed or empty
    const lastChar = val.slice(-1)
    if (lastChar === ' ' || lastChar === '}') {
      showSuggestions.value = false
    }
  }
}

const selectVariable = (item) => {
  // Replace the trailing $ with ${name}
  // This is a naive implementation, assumes cursor is at end.
  // For better quality, we need cursor position. But el-input exposes native input.
  
  const val = internalValue.value
  if (val.endsWith('$')) {
    internalValue.value = val.substring(0, val.length - 1) + '${' + item.name + '}'
  } else {
    // Append
    internalValue.value += '${' + item.name + '}'
  }
  showSuggestions.value = false
  
  // Refocus input
  // inputRef.value.focus() 
}

import { onBeforeUnmount } from 'vue'
onBeforeUnmount(() => {
  // Hard cleanup only. Reseting reactive variables here can trigger re-renders 
  // and secondary unmounts during destruction, causing parentNode errors.
})

</script>

<style scoped>
.variable-input-wrapper {
  width: 100%;
  min-width: 500px;
}

.suggestion-list {
  max-height: 200px;
  overflow-y: auto;
}

.suggestion-item {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid #f0f2f5;
}

.suggestion-item:last-child {
  border-bottom: none;
}

.suggestion-item.active {
  background-color: #ecf5ff;
}

.var-name {
  font-weight: 500;
  color: #409eff;
}

.var-value {
  color: #909399;
  font-size: 12px;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-suggestions {
  padding: 10px;
  text-align: center;
  color: #909399;
}
</style>
