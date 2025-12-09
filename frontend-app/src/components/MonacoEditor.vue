<template>
  <div class="monaco-editor-container" :style="{ height: height }">
    <vue-monaco-editor
      v-model:value="content"
      :language="language"
      :theme="computedTheme"
      :options="editorOptions"
      @mount="handleMount"
    />
  </div>
</template>

<script setup>
import { ref, watch, computed, onBeforeUnmount } from 'vue'
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
  }
})

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
  theme: props.theme
}))

let completionDisposable = null

const handleMount = async (editor, monaco) => {
  // 1. Configure Custom Theme for Variable Highlighting
  monaco.editor.defineTheme('gmn-dark', {
    base: 'vs-dark',
    inherit: true,
    rules: [
      { token: 'variable', foreground: '4ec9b0', fontStyle: 'bold' }
    ],
    colors: {}
  })

  // 2. Set Tokenizer for Highlighting ${variable}
  // Note: This is global for the language. 
  // We apply it typically once. To be safe, checking if language already has our tokens is hard.
  // Using setMonarchTokensProvider works but might conflict if library does it.
  // For JSON, we extend it.
  // Simplest way for 'variable' tokenization without full language redef:
  // We can't easily extend JSON tokenizer in Monaco without redefining it.
  // Skipping custom tokenization for now to avoid breaking JSON syntax check.
  // Instead, rely on Generic Autocomplete.

  // 3. Register Completion Provider
  // We register it for 'json' and 'groovy'.
  
  if (!window.__gmn_completion_registered) {
    window.__gmn_completion_registered = true
    
    // Fetch variables once
    let variables = []
    try {
      const envs = await environmentApi.getAll()
       // Add built-in variables
      variables.push({ label: 'base_url', insertText: '${base_url}', detail: 'Global Variable' })
      variables.push({ label: 'timestamp', insertText: '${timestamp}', detail: 'Built-in' })
      
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
    } catch (e) {
      console.warn('Failed to fetch variables for Monaco', e)
    }

    const providerRel = (model, position) => {
        const word = model.getWordUntilPosition(position);
        const range = {
            startLineNumber: position.lineNumber,
            endLineNumber: position.lineNumber,
            startColumn: word.startColumn,
            endColumn: word.endColumn
        };
        
        // Trigger on $
        // Actually monaco triggers on typing. We want to check if previous char is $
        // Or if we just typed $.
        const lineContent = model.getLineContent(position.lineNumber)
        const textUntilPosition = lineContent.substring(0, position.column - 1)
        
        // If simply typing, show all variables?
        // Or only if $ is typed.
        // If user typed '$', textUntilPosition ends with '$'.
        // If user typed '$v', textUntilPosition ends with '$v'.
        
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
}
</style>
