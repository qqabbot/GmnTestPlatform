<template>
  <div class="monaco-editor-container" :style="{ height: isFullscreen ? '100vh' : height }">
    <div class="editor-toolbar" v-if="showToolbar">
      <el-button-group>
        <el-button size="small" @click="formatCode" :disabled="readOnly || language !== 'json'">
          <el-icon><Document /></el-icon> Format
        </el-button>
        <el-button size="small" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon> {{ isFullscreen ? 'Exit' : 'Fullscreen' }}
        </el-button>
      </el-button-group>
    </div>
    <div :class="['editor-wrapper', { 'fullscreen': isFullscreen }]">
      <vue-monaco-editor
        v-model:value="content"
        :language="language"
        :theme="computedTheme"
        :options="editorOptions"
        @mount="handleMount"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onBeforeUnmount, onMounted, onUnmounted } from 'vue'
import { Document, FullScreen } from '@element-plus/icons-vue'
import { environmentApi } from '../api/environment'
import { VueMonacoEditor } from '@guolao/vue-monaco-editor'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: 'json'
  },
  height: {
    type: String,
    default: '300px'
  },
  readOnly: {
    type: Boolean,
    default: false
  },
  theme: {
    type: String,
    default: 'vs-dark' // Changed default to Dark as per Phase 3.2 requirements
  },
  showToolbar: {
    type: Boolean,
    default: true
  }
})

const isFullscreen = ref(false)
let editorInstance = null
let monacoInstance = null
let layoutTimer = null
const isMounted = ref(false)

const emit = defineEmits(['update:modelValue', 'change'])

const content = computed({
  get: () => props.modelValue,
  set: (val) => {
    emit('update:modelValue', val)
    emit('change', val)
  }
})

const computedTheme = computed(() => props.theme) // allow overriding

const editorOptions = computed(() => ({
  automaticLayout: true,
  formatOnType: true,
  formatOnPaste: true,
  readOnly: props.readOnly,
  minimap: { enabled: false },
  scrollBeyondLastLine: false,
  fontSize: 14,
  tabSize: 2,
  theme: props.theme,
  wordWrap: 'on', // Enable word wrap to prevent horizontal scrolling
  wrappingIndent: 'indent',
  lineNumbers: 'on',
  renderLineHighlight: 'all'
}))

let completionDisposable = null

const formatCode = () => {
  if (editorInstance && monacoInstance && props.language === 'json') {
    try {
      editorInstance.getAction('editor.action.formatDocument').run()
    } catch (e) {
      console.error('Format failed:', e)
    }
  }
}

const safeLayout = () => {
  if (editorInstance) {
    try {
      editorInstance.layout()
    } catch (e) {
      // Ignore layout errors during unmount/resize
    }
  }
}

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
  if (editorInstance) {
    if (layoutTimer) clearTimeout(layoutTimer)
    layoutTimer = setTimeout(() => {
      safeLayout()
      layoutTimer = null
    }, 100)
  }
}

const handleEscape = (e) => {
  if (e.key === 'Escape' && isFullscreen.value) {
    isFullscreen.value = false
    if (editorInstance) {
      if (layoutTimer) clearTimeout(layoutTimer)
      layoutTimer = setTimeout(() => {
        safeLayout()
        layoutTimer = null
      }, 100)
    }
  }
}

onMounted(() => {
  isMounted.value = true
  window.addEventListener('keydown', handleEscape)
})

onBeforeUnmount(() => {
  isMounted.value = false
  window.removeEventListener('keydown', handleEscape)
  if (layoutTimer) {
    clearTimeout(layoutTimer)
    layoutTimer = null
  }
  // Dispose editor instance to prevent memory leaks and DOM access errors
  if (editorInstance) {
    try {
      editorInstance.dispose()
    } catch (e) {
      // Ignore errors during disposal
    }
    editorInstance = null
  }
  monacoInstance = null
})

const handleMount = async (editor, monaco) => {
  editorInstance = editor
  monacoInstance = monaco
  // 1. Configure Custom Theme for Variable Highlighting
  // Guard to prevent re-definition (Monaco is global)
  if (!window.__gmn_theme_registered) {
    window.__gmn_theme_registered = true
    monaco.editor.defineTheme('gmn-dark', {
      base: 'vs-dark',
      inherit: true,
      rules: [
        { token: 'variable', foreground: '4ec9b0', fontStyle: 'bold' }
      ],
      colors: {}
    })
  }

  // 2. Set Tokenizer for Highlighting ${variable}
  // ... skipped ...

  // 3. Register Completion Provider
  // We register it for 'json' and 'groovy'.
  
  if (!window.__gmn_completion_registered) {
    window.__gmn_completion_registered = true // Set immediately to prevent race condition
    
    // Fetch variables once
    let variables = []
    try {
      // Use a timeout or background fetch to not delay editor init? 
      // Actually we just start the fetch, the provider will use 'variables' array which fills up later.
      environmentApi.getAll().then(envs => {
        // Check if component is still mounted before processing
        if (!isMounted.value || !editorInstance) return
         // Add built-in variables
        variables.push({ label: 'base_url', insertText: '${base_url}', detail: 'Global Variable', kind: monaco.languages.CompletionItemKind.Variable })
        variables.push({ label: 'timestamp', insertText: '${timestamp}', detail: 'Built-in', kind: monaco.languages.CompletionItemKind.Variable })
        
        envs.forEach(env => {
          if (env.variables) {
            try {
              const vars = JSON.parse(env.variables)
              Object.keys(vars).forEach(key => {
                if (!variables.find(v => v.label === key)) {
                  variables.push({
                    label: key,
                    insertText: '${' + key + '}',
                    detail: `Env: ${env.envName}`,
                    kind: monaco.languages.CompletionItemKind.Variable
                  })
                }
              })
            } catch (e) {}
          }
        })
      }).catch(e => {
        console.warn('Failed to fetch variables for Monaco', e)
      })
    } catch (e) {
      console.warn('Failed to initiate variable fetch', e)
    }

    const providerRel = (model, position) => {
        // ... (rest of provider logic)
        const word = model.getWordUntilPosition(position);
        const range = {
            startLineNumber: position.lineNumber,
            endLineNumber: position.lineNumber,
            startColumn: word.startColumn,
            endColumn: word.endColumn
        };
        
        return {
            suggestions: variables.map(v => ({
              ...v,
              range: range
            }))
        };
    }

    monaco.languages.registerCompletionItemProvider('json', {
        triggerCharacters: ['$'],
        provideCompletionItems: providerRel
    });
    
    monaco.languages.registerCompletionItemProvider('groovy', {
        triggerCharacters: ['$'],
        provideCompletionItems: providerRel
    });
  }
}

</script>

<style scoped>
.monaco-editor-container {
  width: 100%;
  border: 1px solid #4c4d4f; /* Darker border for dark theme */
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.editor-toolbar {
  padding: 8px;
  background-color: #1e1e1e;
  border-bottom: 1px solid #4c4d4f;
  display: flex;
  justify-content: flex-end;
}

.editor-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.editor-wrapper.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  background-color: #1e1e1e;
}

.monaco-editor-container {
  display: flex;
  flex-direction: column;
}

.monaco-editor-container :deep(.vue-monaco-editor) {
  flex: 1;
  min-height: 0;
}

.editor-wrapper.fullscreen .monaco-editor-container {
  height: 100vh;
  border: none;
  border-radius: 0;
}
</style>
