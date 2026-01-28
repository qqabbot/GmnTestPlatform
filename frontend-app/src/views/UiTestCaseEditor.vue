<template>
  <div class="ui-test-case-editor">
    <div class="editor-header">
      <div class="header-left">
        <el-button @click="router.back()" link>
          <el-icon><ArrowLeft /></el-icon> Back
        </el-button>
        <span class="page-title">{{ isEditMode ? 'Edit UI Test Case' : 'New UI Test Case' }}</span>
      </div>
      <div class="header-actions">
        <el-button type="success" @click="handleExecute" v-if="isEditMode">
          <el-icon><VideoPlay /></el-icon> Execute
        </el-button>
        <el-button type="primary" @click="handleSave">
          <el-icon><Check /></el-icon> Save
        </el-button>
      </div>
    </div>

    <div class="editor-content">
      <el-row :gutter="20">
        <!-- Settings -->
        <el-col :span="8">
          <el-card header="Case Settings">
            <el-form :model="uiCase" label-position="top">
              <el-form-item label="Case Name" required>
                <el-input v-model="uiCase.name" placeholder="Enter case name" />
              </el-form-item>
              <el-form-item label="Project" required>
                <el-select v-model="uiCase.projectId" style="width: 100%" @change="onProjectChange">
                  <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="Module" required>
                <el-select v-model="uiCase.moduleId" style="width: 100%" :disabled="!uiCase.projectId">
                  <el-option v-for="m in filteredModules" :key="m.id" :label="m.moduleName" :value="m.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="Start URL">
                <el-input v-model="uiCase.startUrl" placeholder="https://example.com" />
              </el-form-item>
              <el-form-item label="Browser" required>
                <el-select v-model="uiCase.browserType" style="width: 100%">
                  <el-option label="Chromium (Chrome/Edge)" value="chromium" />
                  <el-option label="Firefox" value="firefox" />
                  <el-option label="WebKit (Safari)" value="webkit" />
                </el-select>
              </el-form-item>
              <el-form-item label="Execution Mode">
                <el-radio-group v-model="executionMode">
                  <el-radio label="server">Server Execution</el-radio>
                  <el-radio label="local">Local Execution</el-radio>
                </el-radio-group>
                <div style="margin-top: 5px; color: #909399; font-size: 12px;">
                  <span v-if="executionMode === 'server'">Test runs on server (headless mode)</span>
                  <span v-else>Test runs locally in your browser (requires Playwright installation)</span>
                </div>
              </el-form-item>
              <el-form-item label="Browser Window" v-if="executionMode === 'server'">
                <el-radio-group v-model="uiCase.headless">
                  <el-radio :label="true">Headless (Background)</el-radio>
                  <el-radio :label="false">Headed (Visible Window)</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="Viewport Size">
                <div style="display: flex; gap: 10px;">
                  <el-input-number v-model="uiCase.viewportWidth" :min="320" :max="3840" />
                  <span>x</span>
                  <el-input-number v-model="uiCase.viewportHeight" :min="240" :max="2160" />
                </div>
              </el-form-item>
              <el-form-item label="Description">
                <el-input v-model="uiCase.description" type="textarea" :rows="2" />
              </el-form-item>
              <el-form-item label="Custom Headers">
                <el-input 
                  v-model="uiCase.customHeaders" 
                  type="textarea" 
                  :rows="3" 
                  placeholder='{"x-token": "your-token", "Authorization": "Bearer xxx"}'
                />
                <div class="form-tip">JSON format. These headers will be added to all HTTP requests.</div>
              </el-form-item>
              <el-form-item label="Custom Cookies">
                <el-input 
                  v-model="uiCase.customCookies" 
                  type="textarea" 
                  :rows="3" 
                  placeholder='[{"name": "session", "value": "xyz", "domain": "example.com", "path": "/"}]'
                />
                <div class="form-tip">JSON array. Injected into browser context. Requires domain/path.</div>
              </el-form-item>
              <el-form-item label="Auto-Dismiss Dialogs">
                <el-switch v-model="uiCase.autoDismissDialogs" />
                <span style="margin-left: 10px; color: #909399; font-size: 12px;">
                  Automatically close alert/confirm/prompt popups
                </span>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <!-- Steps -->
        <el-col :span="16">
          <el-card>
            <template #header>
              <div class="step-header">
                <span>Test Steps</span>
                <div class="step-actions">
                  <el-button-group>
                    <el-button 
                      type="warning" 
                      size="small" 
                      @click="handleStartRecording" 
                      :disabled="isRecording"
                    >
                      <el-icon><VideoCamera /></el-icon> {{ isRecording ? 'Recording...' : 'Start Recording' }}
                    </el-button>
                    <el-button 
                      type="danger" 
                      size="small" 
                      @click="handleStopRecording" 
                      :disabled="!isRecording"
                    >
                      <el-icon><VideoPause /></el-icon> Stop Recording
                    </el-button>
                  </el-button-group>
                  <el-button type="info" size="small" @click="showImportDialog = true">
                    <el-icon><Upload /></el-icon> Import Code
                  </el-button>
                  <el-button type="primary" size="small" @click="addStep">
                    <el-icon><Plus /></el-icon> Add Step
                  </el-button>
                </div>
              </div>
            </template>
            
            <el-table :data="steps" row-key="tempId" default-expand-all :tree-props="{ children: 'children', hasChildren: 'hasChildren' }" style="width: 100%">
              <el-table-column label="#" width="50">
                <template #default="{ $index }">{{ $index + 1 }}</template>
              </el-table-column>
              <el-table-column label="Action" width="180">
                <template #default="{ row }">
                  <el-select v-model="row.actionType" size="small" style="width: 100%">
                    <el-option label="Navigate" value="NAVIGATE" />
                    <el-option label="Click" value="CLICK" />
                    <el-option label="Double Click" value="DBL_CLICK" />
                    <el-option label="Right Click" value="RIGHT_CLICK" />
                    <el-option label="Fill/Type" value="FILL" />
                    <el-option label="Press Key" value="PRESS_KEY" />
                    <el-option label="Select Option" value="SELECT_OPTION" />
                    <el-option label="Hover" value="HOVER" />
                    <el-option label="Drag & Drop" value="DRAG_AND_DROP" />
                    <el-option label="Scroll To" value="SCROLL_TO" />
                    <el-option label="Wait For Selector" value="WAIT_FOR_SELECTOR" />
                    <el-option label="Wait Load State" value="WAIT_FOR_LOAD_STATE" />
                    <el-option label="Wait Load State" value="WAIT_FOR_LOAD_STATE" />
                    <el-option label="Assert Visible" value="ASSERT_VISIBLE" />
                    <el-option label="Assert Not Visible" value="ASSERT_NOT_VISIBLE" />
                    <el-option label="Assert Text" value="ASSERT_TEXT" />
                    <el-option label="IF Condition" value="IF" />
                    <el-option label="FOR Loop" value="FOR" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="Selector/Target" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.selector" size="small" v-if="!['WAIT_FOR_LOAD_STATE', 'IF', 'FOR'].includes(row.actionType)" placeholder="Selector or URL" />
                  <el-input v-model="row.conditionExpression" size="small" v-if="row.actionType === 'IF'" placeholder="Condition (e.g. !#error)" />
                  <el-input v-model="row.loopSource" size="small" v-if="row.actionType === 'FOR'" placeholder="Count (e.g. 3) or Selector" />
                </template>
              </el-table-column>
              <el-table-column label="Value/Key" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.value" size="small" v-if="['NAVIGATE', 'FILL', 'ASSERT_TEXT', 'PRESS_KEY', 'SELECT_OPTION', 'DRAG_AND_DROP'].includes(row.actionType)" placeholder="Value or Target Selector" />
                </template>
              </el-table-column>
              <el-table-column label="Actions" width="120">
                <template #default="{ row }">
                  <el-button type="primary" size="small" circle @click="insertStep(row)" title="Insert Step Below">
                    <el-icon><Plus /></el-icon>
                  </el-button>
                  <el-button type="success" size="small" circle @click="addChildStep(row)" v-if="['IF', 'FOR'].includes(row.actionType)" title="Add Child Step">
                    <el-icon><Plus /></el-icon>
                  </el-button>
                  <el-button type="danger" size="small" circle @click="removeStep(row)" title="Delete Step">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- Import Dialog -->
    <el-dialog v-model="showImportDialog" title="Import from Playwright Code" width="600px">
      <p style="margin-bottom: 10px; color: #666;">Paste code generated by Playwright Codegen (Java/JS/TS) here:</p>
      <el-input v-model="importCode" type="textarea" :rows="10" placeholder="page.navigate('...');&#10;page.locator('#submit').click();" />
      <template #footer>
        <el-button @click="showImportDialog = false">Cancel</el-button>
        <el-button type="primary" @click="handleImportCode">Import</el-button>
      </template>
    </el-dialog>

    <!-- Recording Dialog -->
    <el-dialog v-model="showRecordingDialog" title="Start Recording" width="500px">
      <el-form :model="recordingForm" label-position="top">
        <el-form-item label="Target URL" required>
          <el-input 
            v-model="recordingForm.targetUrl" 
            placeholder="https://example.com"
            :disabled="isRecording"
          />
          <div style="margin-top: 5px; color: #909399; font-size: 12px;">
            The browser will open this URL and record your interactions
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRecordingDialog = false" :disabled="isRecording">Cancel</el-button>
        <el-button type="primary" @click="confirmStartRecording" :disabled="isRecording || !recordingForm.targetUrl">
          Start Recording
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, VideoPlay, Check, Plus, Delete, Upload, VideoCamera, VideoPause } from '@element-plus/icons-vue'
import { uiTestApi } from '../api/uiTest'
import { projectApi } from '../api/project'
import { testModuleApi } from '../api/testModule'

const route = useRoute()
const router = useRouter()

const isEditMode = computed(() => !!route.params.id)
const projects = ref([])
const modules = ref([])
const showImportDialog = ref(false)
const importCode = ref('')
const isRecording = ref(false)
const showRecordingDialog = ref(false)
const recordingForm = ref({
  targetUrl: ''
})
let recordingWebSocket = null
const executionMode = ref('server')
const playwrightInstalled = ref(false)
const checkingPlaywright = ref(false)

const uiCase = ref({
  name: '',
  projectId: null,
  moduleId: null,
  startUrl: '',
  browserType: 'chromium',
  headless: true,
  viewportWidth: 1280,
  viewportHeight: 720,
  description: '',
  customHeaders: '',
  customCookies: '',
  autoDismissDialogs: false
})

const steps = ref([])

const loadData = async () => {
  try {
    projects.value = await projectApi.getAll()
    modules.value = await testModuleApi.getAll()

    if (isEditMode.value) {
      const res = await uiTestApi.getCase(route.params.id)
      uiCase.value = res
       const stepsRes = await uiTestApi.getSteps(route.params.id)
       // Convert flat list to tree
       steps.value = buildStepTree(stepsRes)
    }
  } catch (err) {
    ElMessage.error('Failed to load data')
  }
}

const filteredModules = computed(() => {
  if (!uiCase.value.projectId) return []
  return modules.value.filter(m => (m.project?.id || m.project) === uiCase.value.projectId)
})

const onProjectChange = () => {
  uiCase.value.moduleId = null
}

const buildStepTree = (flatSteps) => {
  if (!flatSteps || flatSteps.length === 0) return []
  
  const nodeMap = {}
  const roots = []
  
  // First pass: Create nodes and assign tempId if missing
  flatSteps.forEach(step => {
    step.children = []
    step.tempId = step.id || `temp_${Date.now()}_${Math.random()}`
    nodeMap[step.id] = step
  })
  
  // Second pass: Link children
  flatSteps.forEach(step => {
    if (step.parentId && nodeMap[step.parentId]) {
      nodeMap[step.parentId].children.push(step)
    } else {
      roots.push(step)
    }
  })
  
  return roots
}

const flattenSteps = (treeNodes, parentId = null) => {
  let result = []
  treeNodes.forEach((node, index) => {
    const flatNode = { ...node }
    delete flatNode.children
    delete flatNode.tempId // Remove temp ID
    flatNode.parentId = parentId
    flatNode.stepOrder = index + 1
    result.push(flatNode)
    
    if (node.children && node.children.length > 0) {
      result = result.concat(flattenSteps(node.children, node.id || node.tempId)) // If new node, parentId is problematic... 
      // WAIT: If parent is new, it has no ID. Backend needs to save parent first?
      // Actually backend usually handles list save. But FK relationship requires ID.
      // If I send list, backend logic needs to handle ID assignment.
      // Current backend insertCase/Steps deletes all and re-inserts?
      // Let's check UiTestService.
      // If it re-inserts, it generates new IDs. But parent_id refers to ID.
      // If parent is newly inserted, we don't know it yet.
      // SOLUTION: Backend must handle recursion or topological sort save.
      // OR: I rely on `stepOrder`? No, schema uses parent_id.
      // Workaround: If I save a tree, backend should handle it. But backend expects flat list.
      // Complex. 
      // Maybe I assume for phase 7.5 that user saves parent first? No.
      // If I use a "batch save" that returns IDs map?
      // Or I change backend to accept Tree?
    }
  })
  return result
}

// FIX: Backend logic update required for saving hierarchy.
// Current logic: deleteStepsByCaseId -> insertStep loop.
// If I use LAST_INSERT_ID(), it works for one.
// If I insert parent, get ID, then insert children.
// My backend `UiTestService.saveCase` calls `saveSteps`.
// I need, in Frontend, to NOT flatten blindly if IDs are missing.
// Actually, simplest is: Save Parent, get IDs, Save Children. But that's too many requests.
// Better: Send flat list. But ParentId must be valid.
// If Parent has no ID (new step), I can't send valid ParentId.
// PROPOSAL: Use negative IDs (temp IDs) for parent_id reference? And Backend resolves it?
// That's standard practice.
// I'll update frontend to use `tempId` for `parentId` if `id` is missing.
// AND Update backend to resolve temp IDs.

const insertStep = (row) => {
  // Find parent list and index
  const findAndInsert = (list) => {
    const idx = list.indexOf(row)
    if (idx > -1) {
      list.splice(idx + 1, 0, {
        actionType: 'CLICK',
        selector: '',
        value: '',
        children: [],
        tempId: `temp_${Date.now()}_${Math.random()}`,
        type: 'step',
        screenshotOnFailure: true,
        stepOrder: 0 // Will be handled on save
      })
      return true
    }
    for (const item of list) {
        if (item.children && findAndInsert(item.children)) return true
    }
    return false
  }
  
  findAndInsert(steps.value)
}

const addStep = () => {
  steps.value.push({
    actionType: 'NAVIGATE',
    selector: '',
    value: '',
    children: [], // For tree
    tempId: `temp_${Date.now()}_${Math.random()}`,
    type: 'step',
    screenshotOnFailure: true
  })
}

const addChildStep = (parentRow) => {
  if (!parentRow.children) parentRow.children = []
  parentRow.children.push({
    actionType: 'CLICK',
    selector: '',
    value: '',
    children: [],
    tempId: `temp_${Date.now()}_${Math.random()}`,
    type: 'step', // or leaf
    screenshotOnFailure: true
  })
}

const removeStep = (row) => {
  // Recursive find and delete
  const removeFromList = (list) => {
    const idx = list.indexOf(row)
    if (idx > -1) {
      list.splice(idx, 1)
      return true
    }
    for (const item of list) {
        if (item.children && removeFromList(item.children)) return true
    }
    return false
  }
  removeFromList(steps.value)
}

const handleSave = async () => {
  try {
    if (!uiCase.value.name || !uiCase.value.moduleId) {
      ElMessage.warning('Please fill in required fields')
      return
    }

    // Attach steps to the case object for combined saving
    // Flatten the tree structure to a list, backend expects flat list
    const flatSteps = flattenSteps(steps.value)
    
    // Map temp IDs to parentTempId for backend resolution
    const payloadSteps = flatSteps.map(s => {
       const stepParams = { ...s }
       // If parentId is a temp string, move it to parentTempId and nullify parentId
       if (typeof s.parentId === 'string' && s.parentId.startsWith('temp_')) {
           stepParams.parentTempId = s.parentId
           stepParams.parentId = null 
       }
       // Ensure tempId is sent if it's a new step
       if (s.tempId && s.tempId.startsWith('temp_')) {
           stepParams.tempId = s.tempId
       }
       return stepParams
    })

    const payload = {
      ...uiCase.value,
      steps: payloadSteps
    }

    // Save Case (and steps)
    await uiTestApi.saveCase(payload)
    
    ElMessage.success('UI Test Case saved')
    router.push('/ui-testing/cases')
  } catch (err) {
    console.error(err)
    ElMessage.error('Failed to save UI Test Case')
  }
}

const handleExecute = async () => {
    try {
        if (!uiCase.value.name || !uiCase.value.moduleId) {
            ElMessage.warning('Please fill in required fields')
            return
        }

        // Check Playwright installation for local mode
        if (executionMode.value === 'local') {
            const installed = await checkAndInstallPlaywright()
            if (!installed) {
                ElMessage.warning('Playwright installation is required for local execution')
                return
            }
        }

        // 1. Auto-save current state before execution to ensure backend gets latest settings (headless, etc.)
        const payload = {
            ...uiCase.value,
            steps: steps.value
        }
        const savedCase = await uiTestApi.saveCase(payload)
        uiCase.value.id = savedCase.id // Update ID if it was a new case

        // 2. Execute
        if (executionMode.value === 'local') {
            await handleLocalExecute()
        } else {
            await handleServerExecute()
        }
    } catch (e) {
        console.error('Execution Error:', e)
        ElMessage.error('Execution failed: ' + (e.response?.data?.message || e.message || 'Server error'))
    }
}

const handleServerExecute = async () => {
    ElMessage.info('Saving and starting execution...')
    const record = await uiTestApi.executeCase(uiCase.value.id, 'server')
    
    if (record.status === 'FAILURE') {
        ElMessageBox.alert(record.errorMessage || 'Unknown error', 'Execution Failed', {
            confirmButtonText: 'OK',
            type: 'error'
        })
    } else {
        ElMessage.success('Execution successful')
        router.push(`/ui-testing/reports/${record.id}`)
    }
}

const handleLocalExecute = async () => {
    try {
        ElMessage.info('Generating local execution script...')
        
        // Get local script
        const response = await uiTestApi.getLocalScript(uiCase.value.id)
        if (!response.success || !response.script) {
            ElMessage.error('Failed to generate local execution script')
            return
        }
        
        // Show script in a simple dialog
        ElMessageBox.prompt(
            `Local execution script generated.\n\nTo run locally:\n1. Save the script below as test.js\n2. Run: node test.js\n\nScript:\n\n${response.script}`,
            'Local Execution Script',
            {
                confirmButtonText: 'Copy Script',
                cancelButtonText: 'Close',
                inputType: 'textarea',
                inputValue: response.script,
                inputPlaceholder: 'Script will be copied to clipboard',
                inputAttributes: {
                    readonly: true,
                    rows: 20
                }
            }
        ).then(() => {
            // Copy to clipboard
            navigator.clipboard.writeText(response.script).then(() => {
                ElMessage.success('Script copied to clipboard')
            }).catch(() => {
                ElMessage.warning('Failed to copy to clipboard. Please copy manually.')
            })
        }).catch(() => {
            // User cancelled, do nothing
        })
    } catch (e) {
        if (e !== 'cancel') {
            console.error('Local execution error:', e)
            ElMessage.error('Failed to generate local script: ' + (e.response?.data?.error || e.message || 'Server error'))
        }
    }
}

const checkAndInstallPlaywright = async () => {
    if (playwrightInstalled.value) {
        return true
    }
    
    checkingPlaywright.value = true
    try {
        const response = await uiTestApi.checkPlaywright()
        if (response.installed) {
            playwrightInstalled.value = true
            checkingPlaywright.value = false
            return true
        }
        
        // Not installed, ask user to install
        await ElMessageBox.confirm(
            'Playwright browsers are not installed. Would you like to install them now?',
            'Playwright Installation Required',
            {
                type: 'warning',
                confirmButtonText: 'Install',
                cancelButtonText: 'Cancel'
            }
        )
        
        // Install
        ElMessage.info('Installing Playwright browsers... This may take a few minutes.')
        const installResponse = await uiTestApi.installPlaywright()
        
        if (installResponse.success) {
            playwrightInstalled.value = true
            ElMessage.success('Playwright browsers installation initiated. They will be downloaded automatically on first use.')
            return true
        } else {
            ElMessage.error('Failed to install Playwright: ' + (installResponse.error || 'Unknown error'))
            return false
        }
    } catch (e) {
        if (e === 'cancel') {
            return false
        }
        console.error('Check Playwright error:', e)
        ElMessage.error('Failed to check Playwright installation: ' + (e.response?.data?.error || e.message || 'Server error'))
        return false
    } finally {
        checkingPlaywright.value = false
    }
}

const handleImportCode = () => {
  if (!importCode.value) return

  const lines = importCode.value.split('\n')
  const newSteps = []
  
  lines.forEach(line => {
    line = line.trim().replace(/^await\s+/, '').replace(/;$/, '')
    if (!line.startsWith('page.')) return

    let step = null

    // 1. Check for Navigation
    const navMatch = line.match(/page\.(?:goto|navigate)\(['"](.+?)['"]\)/)
    if (navMatch) {
      step = { actionType: 'NAVIGATE', selector: '', value: navMatch[1] }
    } else {
      // 2. Parse Locator and Action
      // Typical format: page.locator('...').click() or page.getByRole(...).fill('...')
      const lastDotIndex = line.lastIndexOf('.')
      if (lastDotIndex === -1) return

      const locatorPart = line.substring(0, lastDotIndex)
      const actionPart = line.substring(lastDotIndex)

      let selector = ''
      
      // Locator patterns
      const roleMatch = locatorPart.match(/getByRole\(['"](.+?)['"](?:,\s*\{\s*name:\s*['"](.+?)['"]\s*\}\s*)?\)/)
      const textMatch = locatorPart.match(/getByText\(['"](.+?)['"]\)/)
      const placeholderMatch = locatorPart.match(/getByPlaceholder\(['"](.+?)['"]\)/)
      const labelMatch = locatorPart.match(/getByLabel\(['"](.+?)['"]\)/)
      const genericLocatorMatch = locatorPart.match(/locator\(['"](.+?)['"]\)/)

      if (roleMatch) {
        selector = `role=${roleMatch[1]}`
        if (roleMatch[2]) selector += `[name="${roleMatch[2]}"]`
      } else if (textMatch) {
        selector = `text="${textMatch[1]}"`
      } else if (placeholderMatch) {
        selector = `placeholder="${placeholderMatch[1]}"`
      } else if (labelMatch) {
        selector = `label="${labelMatch[1]}"`
      } else if (genericLocatorMatch) {
        selector = genericLocatorMatch[1]
      }

      // Handle .nth(n)
      const nthMatch = locatorPart.match(/\.nth\((\d+)\)/)
      if (nthMatch && selector) {
        selector += ` >> nth=${nthMatch[1]}`
      }

      if (selector) {
        // Action patterns
        const fillMatch = actionPart.match(/\.(?:fill|type)\(['"](.+?)['"]\)/)
        const pressMatch = actionPart.match(/\.press\(['"](.+?)['"]\)/)
        const selectMatch = actionPart.match(/\.selectOption\(['"](.+?)['"]\)/)

        if (fillMatch) {
          step = { actionType: 'FILL', selector, value: fillMatch[1] }
        } else if (pressMatch) {
          step = { actionType: 'PRESS_KEY', selector, value: pressMatch[1] }
        } else if (selectMatch) {
          step = { actionType: 'SELECT_OPTION', selector, value: selectMatch[1] }
        } else if (actionPart.includes('click()')) {
          step = { actionType: 'CLICK', selector, value: '' }
        } else if (actionPart.includes('dblclick()')) {
          step = { actionType: 'DBL_CLICK', selector, value: '' }
        } else if (actionPart.includes('hover()')) {
          step = { actionType: 'HOVER', selector, value: '' }
        }
      }
    }

    if (step) {
      step.stepOrder = steps.value.length + newSteps.length + 1
      step.screenshotOnFailure = true
      step.children = []
      step.tempId = `temp_${Date.now()}_${Math.random()}`
      newSteps.push(step)
    }
  })

  if (newSteps.length > 0) {
    steps.value.push(...newSteps)
    ElMessage.success(`Imported ${newSteps.length} steps`)
    showImportDialog.value = false
    importCode.value = ''
  } else {
    ElMessage.error('Failed to parse any steps. Please check the code format.')
  }
}

// Recording functions
const handleStartRecording = () => {
  if (!isEditMode.value && !uiCase.value.id) {
    ElMessage.warning('Please save the test case first before recording')
    return
  }
  
  // Use startUrl if available, otherwise show dialog
  if (uiCase.value.startUrl) {
    recordingForm.value.targetUrl = uiCase.value.startUrl
    confirmStartRecording()
  } else {
    showRecordingDialog.value = true
  }
}

const confirmStartRecording = async () => {
  if (!recordingForm.value.targetUrl || !recordingForm.value.targetUrl.trim()) {
    ElMessage.warning('Please enter a target URL')
    return
  }

  try {
    // Ensure case is saved first
    if (!uiCase.value.id) {
      const payload = {
        ...uiCase.value,
        steps: steps.value
      }
      const savedCase = await uiTestApi.saveCase(payload)
      uiCase.value.id = savedCase.id
    }

    // Start recording
    await uiTestApi.startRecording(uiCase.value.id, {
      targetUrl: recordingForm.value.targetUrl
    })

    // Connect WebSocket
    connectRecordingWebSocket()

    isRecording.value = true
    showRecordingDialog.value = false
    ElMessage.success('Recording started! Please interact with the browser window.')
  } catch (e) {
    console.error('Start recording error:', e)
    ElMessage.error('Failed to start recording: ' + (e.response?.data?.error || e.message || 'Server error'))
  }
}

const handleStopRecording = async () => {
  try {
    await uiTestApi.stopRecording(uiCase.value.id)

    // Close WebSocket
    if (recordingWebSocket) {
      recordingWebSocket.close()
      recordingWebSocket = null
    }

    // Get recorded code and import
    const response = await uiTestApi.getRecordingCode(uiCase.value.id)
    if (response.code && response.code.trim()) {
      importCode.value = response.code
      handleImportCode()
      ElMessage.success('Recording stopped and steps imported successfully')
    } else {
      ElMessage.warning('Recording stopped, but no code was generated')
    }

    isRecording.value = false
    recordingForm.value.targetUrl = ''
  } catch (e) {
    console.error('Stop recording error:', e)
    ElMessage.error('Failed to stop recording: ' + (e.response?.data?.error || e.message || 'Server error'))
    isRecording.value = false
  }
}

const connectRecordingWebSocket = () => {
  if (!uiCase.value.id) {
    return
  }

  // Determine WebSocket URL
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  const wsUrl = `${protocol}//${host}/api/ui-tests/recording/${uiCase.value.id}`

  try {
    recordingWebSocket = new WebSocket(wsUrl)

    recordingWebSocket.onopen = () => {
      console.log('Recording WebSocket connected')
    }

    recordingWebSocket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'code') {
          // Real-time code updates (optional - we'll get full code on stop)
          console.log('Received code line:', data.code)
        }
      } catch (e) {
        console.error('Failed to parse WebSocket message:', e)
      }
    }

    recordingWebSocket.onerror = (error) => {
      console.error('Recording WebSocket error:', error)
      ElMessage.warning('WebSocket connection error. Recording may still work.')
    }

    recordingWebSocket.onclose = () => {
      console.log('Recording WebSocket closed')
      recordingWebSocket = null
    }
  } catch (e) {
    console.error('Failed to create WebSocket connection:', e)
    ElMessage.warning('WebSocket connection failed. Recording may still work.')
  }
}

// Use route guard to ensure clean navigation
onBeforeRouteLeave((to, from, next) => {
  // Stop recording if active
  if (isRecording.value) {
    ElMessageBox.confirm('Recording is in progress. Stop recording before leaving?', 'Warning', {
      type: 'warning',
      confirmButtonText: 'Stop & Leave',
      cancelButtonText: 'Cancel'
    }).then(() => {
      handleStopRecording().finally(() => {
        next()
      })
    }).catch(() => {
      // User cancelled, stay on page
    })
    return
  }
  
  // Close any open dialogs before navigation
  if (showImportDialog.value) {
    showImportDialog.value = false
    nextTick(() => {
      next()
    })
  } else {
    next()
  }
})

onMounted(() => {
  loadData()
  // Check Playwright installation status if in local mode
  if (executionMode.value === 'local') {
    checkAndInstallPlaywright().catch(() => {
      // Silent fail on mount
    })
  }
})

onBeforeUnmount(() => {
  // Clean up WebSocket connection
  if (recordingWebSocket) {
    recordingWebSocket.close()
    recordingWebSocket = null
  }
  
  // Stop recording if active
  if (isRecording.value && uiCase.value.id) {
    uiTestApi.stopRecording(uiCase.value.id).catch(err => {
      console.error('Failed to stop recording on unmount:', err)
    })
  }
  
  // Don't modify reactive state during unmount as it can cause
  // Element Plus components (like menu) to access destroyed DOM elements
  // Vue will automatically handle component cleanup
})
</script>

<style scoped>
.ui-test-case-editor {
  padding: 20px;
}
.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}
.page-title {
  font-size: 18px;
  font-weight: bold;
}
.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.step-actions {
  display: flex;
  gap: 10px;
}
.editor-content {
  margin-top: 20px;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
