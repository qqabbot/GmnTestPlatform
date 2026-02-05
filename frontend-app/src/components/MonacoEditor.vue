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

  // 2. Register Completion Provider
  if (!window.__gmn_completion_registered) {
    window.__gmn_completion_registered = true
    
    let variables = []
    // Fetch system/env variables
    try {
      environmentApi.getAll().then(envs => {
        if (!isMounted.value) return
        
        // Add built-in system variables
        variables.push({ 
          label: 'base_url', 
          insertText: 'base_url}', 
          detail: 'Global Server URL', 
          kind: monaco.languages.CompletionItemKind.Variable 
        })
        variables.push({ 
          label: 'timestamp', 
          insertText: 'timestamp}', 
          detail: 'Current Unix Timestamp', 
          kind: monaco.languages.CompletionItemKind.Variable 
        })
        
        envs.forEach(env => {
          if (env.variables) {
            try {
              const vars = JSON.parse(env.variables)
              Object.keys(vars).forEach(key => {
                if (!variables.find(v => v.label === key)) {
                  variables.push({
                    label: key,
                    insertText: key + '}',
                    detail: `Env: ${env.envName}`,
                    kind: monaco.languages.CompletionItemKind.Variable
                  })
                }
              })
            } catch (e) {}
          }
        })
      })
    } catch (e) {
      console.warn('Failed to fetch variables for Monaco', e)
    }

    // Groovy Built-in Objects and Methods for IntelliSense (Optimization 1)
    const groovyBuiltins = [
      // Common Objects
      { label: 'vars', kind: monaco.languages.CompletionItemKind.Variable, detail: 'Variable Map', insertText: 'vars', documentation: 'Used to get/put variables: vars.get("key"), vars.put("key", "value")' },
      { label: 'response', kind: monaco.languages.CompletionItemKind.Variable, detail: 'Response Body', insertText: 'response', documentation: 'The raw response body as a string.' },
      { label: 'response_body', kind: monaco.languages.CompletionItemKind.Variable, detail: 'Response Body String', insertText: 'response_body', documentation: 'The raw response body as a string (shorthand).' },
      { label: 'status_code', kind: monaco.languages.CompletionItemKind.Variable, detail: 'HTTP Status', insertText: 'status_code', documentation: 'The HTTP status code (integer).' },
      { label: 'status', kind: monaco.languages.CompletionItemKind.Variable, detail: 'HTTP Status (Alt)', insertText: 'status', documentation: 'Alias for status_code.' },
      { label: 'headers', kind: monaco.languages.CompletionItemKind.Variable, detail: 'Response Headers', insertText: 'headers', documentation: 'Map of response headers.' },
      
      // Core Functions
      { label: 'jsonPath', kind: monaco.languages.CompletionItemKind.Function, detail: 'JSON Utility', insertText: 'jsonPath(response, "${1:$.path}")', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'Extract values from JSON using JSONPath.' },
      { label: 'regex', kind: monaco.languages.CompletionItemKind.Function, detail: 'Regex Utility', insertText: 'regex(response, "${1:pattern}")', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'Extract values using Regular Expression. Returns the first captured group.' },
      { label: 'log', kind: monaco.languages.CompletionItemKind.Variable, detail: 'Logger', insertText: 'log.info("${1:message}")', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'Log messages to execution console.' },
      
      // vars methods (for member access)
      { label: 'vars.put', kind: monaco.languages.CompletionItemKind.Method, detail: 'Put Variable', insertText: 'put("${1:key}", ${2:value})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'Store a variable in the runtime context.' },
      { label: 'vars.get', kind: monaco.languages.CompletionItemKind.Method, detail: 'Get Variable', insertText: 'get("${1:key}")', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'Retrieve a variable value from the context.' },
      { label: 'vars.containsKey', kind: monaco.languages.CompletionItemKind.Method, detail: 'Check Variable', insertText: 'containsKey("${1:key}")', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'Check if a variable exists in the context.' },
      { label: 'vars.remove', kind: monaco.languages.CompletionItemKind.Method, detail: 'Remove Variable', insertText: 'remove("${1:key}")', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'Remove a variable from the context.' },
      
      // Common Assertions
      { label: 'assert_status', kind: monaco.languages.CompletionItemKind.Snippet, detail: 'Snippet', insertText: 'assert status_code == 200', documentation: 'Check if status code is 200.' },
      { label: 'assert_json', kind: monaco.languages.CompletionItemKind.Snippet, detail: 'Snippet', insertText: 'assert jsonPath(response, "${1:$.code}") == "${2:200}"', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'Check a JSON path value.' }
    ]

    const variableProvider = {
      triggerCharacters: ['$'],
      provideCompletionItems: (model, position) => {
        const lineContent = model.getLineContent(position.lineNumber)
        const textBefore = lineContent.substring(0, position.column - 1)
        
        // Check if we are inside ${ or just after $
        const isBraceTrigger = textBefore.endsWith('${')
        const isDollarTrigger = textBefore.endsWith('$') && !isBraceTrigger

        if (!isBraceTrigger && !isDollarTrigger) return { suggestions: [] }

        const word = model.getWordUntilPosition(position)
        const range = {
          startLineNumber: position.lineNumber,
          endLineNumber: position.lineNumber,
          startColumn: word.startColumn,
          endColumn: word.endColumn
        }

        return {
          suggestions: variables.map(v => ({
            ...v,
            range,
            // If triggered by $, prepend { to the insert text if it's missing
            insertText: isDollarTrigger ? '{' + v.insertText : v.insertText
          }))
        }
      }
    }

    const groovyIntellisenseProvider = {
      triggerCharacters: ['.'],
      provideCompletionItems: (model, position) => {
        const lineContent = model.getLineContent(position.lineNumber)
        const word = model.getWordUntilPosition(position)
        // Get the text before the word start to check for triggers like "vars."
        const textBeforeWord = lineContent.substring(0, word.startColumn - 1)
        
        // Don't trigger built-ins if we are typing a variable placeholder
        if (textBeforeWord.endsWith('$') || textBeforeWord.endsWith('${')) return { suggestions: [] }

        const range = {
          startLineNumber: position.lineNumber,
          endLineNumber: position.lineNumber,
          startColumn: word.startColumn,
          endColumn: word.endColumn
        }

        // Case 1: vars.member - suggest vars methods
        if (textBeforeWord.endsWith('vars.')) {
          return {
            suggestions: groovyBuiltins
              .filter(b => b.label.startsWith('vars.'))
              .map(b => ({
                ...b,
                label: b.label.replace('vars.', ''),
                range
              }))
          }
        }

        // Case 2: log.member - suggest log methods
        if (textBeforeWord.endsWith('log.')) {
           return {
            suggestions: [
              { label: 'info', kind: monaco.languages.CompletionItemKind.Method, detail: 'Log Info', insertText: 'info("${1:message}")', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, range },
              { label: 'error', kind: monaco.languages.CompletionItemKind.Method, detail: 'Log Error', insertText: 'error("${1:message}")', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, range },
              { label: 'warn', kind: monaco.languages.CompletionItemKind.Method, detail: 'Log Warn', insertText: 'warn("${1:message}")', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, range }
            ]
          }
        }

        // Case 3: Top-level objects and snippets
        return {
          suggestions: groovyBuiltins
            .filter(b => !b.label.includes('.'))
            .map(b => ({
              ...b,
              range
            }))
        }
      }
    }

    monaco.languages.registerCompletionItemProvider('json', variableProvider)
    monaco.languages.registerCompletionItemProvider('groovy', variableProvider)
    monaco.languages.registerCompletionItemProvider('groovy', groovyIntellisenseProvider)
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
