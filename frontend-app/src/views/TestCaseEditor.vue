<template>
  <div class="test-case-editor">
    <!-- Header -->
    <div class="editor-header">
      <div class="header-left">
        <el-button @click="router.back()" link>
          <el-icon><ArrowLeft /></el-icon> Back
        </el-button>
        <el-divider direction="vertical" />
        <span class="page-title">{{ isEditMode ? 'Edit Test Case' : 'New Test Case' }}</span>
      </div>
      <div class="header-actions">
        <el-button type="info" @click="handleDryRun" :loading="store.loading">
          <el-icon><View /></el-icon> Dry Run
        </el-button>
        <el-button type="success" @click="handleRun" :loading="store.loading">
          <el-icon><VideoPlay /></el-icon> Run
        </el-button>
        <el-button type="primary" @click="handleSave" :loading="store.loading">
          <el-icon><Check /></el-icon> Save
        </el-button>
      </div>
    </div>

    <!-- Main Content -->
    <div class="editor-main">
      <!-- Left Sidebar: Step List -->
      <div class="sidebar">
        <step-list
          v-if="isMounted"
          v-model:steps="store.currentCase.steps"
          :selected-index="selectedStepIndex"
          @select="handleStepSelect"
          @add="store.addStep"
          @remove="handleRemoveStep"
          @update-step="handleUpdateStep"
          @open-library="openLibrary"
        />
      </div>

      <!-- Center: Step Detail or Case Settings -->
      <div class="content-area">
        <el-tabs v-model="activeTab" type="border-card" class="main-tabs">
          <el-tab-pane label="Step Details" name="step">
            <step-detail v-model="currentStep" />
          </el-tab-pane>
          <el-tab-pane label="Case Settings" name="settings">
            <div class="settings-form">
              <el-form label-width="140px">
                <el-form-item label="Case Name" required>
                  <el-input v-model="store.currentCase.caseName" placeholder="Enter test case name" />
                </el-form-item>
                
                <el-form-item label="HTTP Method" required>
                  <el-radio-group v-model="store.currentCase.method">
                    <el-radio label="GET">GET</el-radio>
                    <el-radio label="POST">POST</el-radio>
                    <el-radio label="PUT">PUT</el-radio>
                    <el-radio label="DELETE">DELETE</el-radio>
                    <el-radio label="PATCH">PATCH</el-radio>
                  </el-radio-group>
                </el-form-item>
                
                <el-form-item label="Request URL" required>
                  <div style="display: flex; gap: 8px;">
                    <variable-input v-model="store.currentCase.url" placeholder="http://api.example.com/endpoint or ${base_url}/users" style="flex: 1;">
                      <template #prepend>
                        <el-icon><Link /></el-icon>
                      </template>
                    </variable-input>
                    <el-button @click="showCurlDialog = true" size="small" type="info">
                      <el-icon><DocumentCopy /></el-icon> Import cURL
                    </el-button>
                    <el-button @click="showAiDialog = true" size="small" type="warning">
                      <el-icon><MagicStick /></el-icon> AI Generate
                    </el-button>
                  </div>
                  <div class="help-text">Use ${variable_name} for variable substitution. Type $ to autocomplete. Or paste cURL command to auto-fill.</div>
                </el-form-item>

                <el-form-item label="Headers">
                  <monaco-editor v-if="isMounted" v-model="store.currentCase.headers" language="json" height="150px" :show-toolbar="true" />
                  <div class="help-text">JSON headers object</div>
                </el-form-item>
                
                <el-form-item label="Request Body">
                  <div class="section-header">
                    <span class="help-text">JSON request body (for POST/PUT/PATCH methods)</span>
                    <el-button 
                      type="warning" 
                      size="small" 
                      link 
                      @click="handleMockBody(-1)"
                      :loading="mockingBody === -1"
                    >
                      <el-icon><MagicStick /></el-icon> AI Mock Body
                    </el-button>
                  </div>
                  <monaco-editor v-if="isMounted" v-model="store.currentCase.body" language="json" height="200px" :show-toolbar="true" />
                </el-form-item>
                
                <el-divider />
                
                <el-form-item label="Project" required>
                  <el-select v-model="store.currentCase.projectId" placeholder="Select Project" @change="loadModules" style="width: 100%">
                    <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Module" required>
                  <el-select v-model="store.currentCase.moduleId" placeholder="Select Module" :disabled="!store.currentCase.projectId" style="width: 100%">
                    <el-option v-for="m in filteredModules" :key="m.id" :label="m.moduleName" :value="m.id" />
                  </el-select>
                </el-form-item>
                
                <el-divider />
                
                <el-divider />

                <el-form-item label="Pre-request Script">
                   <div style="margin-bottom: 5px; width: 100%;">
                    <el-alert type="info" :closable="false" show-icon>
                      <template #title>
                        Run before request. <b>Variables Fallback Pattern:</b>
                      </template>
                      <template #default>
                        <pre style="margin: 5px 0; font-family: monospace; background: #f4f4f5; padding: 5px; border-radius: 4px;">if (!vars.containsKey("userId")) {
    vars.put("userId", "123456") // Default value for standalone execution
}</pre>
                      </template>
                    </el-alert>
                  </div>
                  <monaco-editor v-model="store.currentCase.setupScript" language="groovy" height="200px" />
                </el-form-item>

                <el-form-item label="Global Assertion">
                  <div style="margin-bottom: 5px; width: 100%;">
                    <el-alert type="success" :closable="false" show-icon>
                      <template #title>
                        Run after request. <b>Groovy Examples:</b>
                      </template>
                      <template #default>
                        <pre style="margin: 5px 0; font-family: monospace; background: #f4f4f5; padding: 5px; border-radius: 4px;">assert status_code == 200
assert jsonPath(response, "$.code") == "200"
// Extract variable for next steps
vars.put("token", jsonPath(response, "$.data.token"))</pre>
                      </template>
                    </el-alert>
                  </div>
                  <monaco-editor v-model="store.currentCase.assertionScript" language="groovy" height="200px" />
                  <div class="help-text">Groovy script for assertions (e.g., status_code == 200)</div>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- Dry Run Dialog -->
    <el-dialog v-model="showDryRunDialog" title="Dry Run" width="600px">
      <el-form>
        <el-form-item label="Environment">
          <el-select v-model="dryRunEnv" placeholder="Select environment" style="width: 100%">
            <el-option
              v-for="env in environments"
              :key="env.id"
              :label="env.envName"
              :value="env.envName"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDryRunDialog = false">Cancel</el-button>
        <el-button type="primary" @click="executeDryRun" :loading="store.loading" :disabled="!dryRunEnv">Run</el-button>
      </template>
    </el-dialog>

    <!-- Execute Dialog -->
    <el-dialog v-model="showExecuteDialog" title="Execute Test Case" width="600px">
      <el-form>
        <el-form-item label="Environment">
          <el-select v-model="executeEnv" placeholder="Select environment" style="width: 100%">
            <el-option
              v-for="env in environments"
              :key="env.id"
              :label="env.envName"
              :value="env.envName"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showExecuteDialog = false">Cancel</el-button>
        <el-button type="success" @click="executeRun" :loading="store.loading" :disabled="!executeEnv">Execute</el-button>
      </template>
    </el-dialog>

    <!-- cURL Import Dialog -->
    <el-dialog v-model="showCurlDialog" title="Import from cURL" width="700px">
      <el-form>
        <el-form-item label="Paste cURL Command">
          <el-input
            v-model="curlCommand"
            type="textarea"
            :rows="8"
            :placeholder="curlPlaceholder"
          />
          <div class="help-text" style="margin-top: 8px;">
            Paste your cURL command here. It will automatically parse method, URL, headers, and body.
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCurlDialog = false">Cancel</el-button>
        <el-button type="primary" @click="handleImportCurl" :loading="importingCurl" :disabled="!curlCommand.trim()">
          Import
        </el-button>
      </template>
    </el-dialog>

    <!-- AI Generate Dialog -->
    <el-dialog v-model="showAiDialog" title="AI Generate Test Cases" width="900px" top="5vh">
      <div v-loading="generatingAi" style="min-height: 200px;">
        <div v-if="generatedCases.length > 0" class="ai-results">
          <el-alert type="success" :closable="false" style="margin-bottom: 15px;">
            AI has generated {{ generatedCases.length }} test scenarios. Select the ones you want to import.
          </el-alert>
          <el-table :data="generatedCases" @selection-change="handleAiSelection">
            <el-table-column type="selection" width="55" />
            <el-table-column prop="caseName" label="Case Name" />
            <el-table-column prop="method" label="Method" width="100" />
            <el-table-column prop="url" label="URL" show-overflow-tooltip />
            <el-table-column label="Actions" width="120">
              <template #default="{ row }">
                <el-button link type="primary" @click="previewAiCase(row)">Preview</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else-if="!generatingAi" class="ai-empty">
          <p>Click "Generate" to let AI create test scenarios based on your current URL and Body.</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="showAiDialog = false">Cancel</el-button>
        <el-button type="warning" @click="handleAiGenerate" :loading="generatingAi">
          {{ generatedCases.length > 0 ? 'Regenerate' : 'Generate' }}
        </el-button>
        <el-button type="primary" @click="importSelectedAiCases" :disabled="selectedAiCases.length === 0" :loading="importingAi">
          Import Selected ({{ selectedAiCases.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- AI Preview Dialog -->
    <el-dialog v-model="showAiPreview" title="Case Preview" width="700px" append-to-body>
      <el-form label-width="100px" v-if="previewingCase">
        <el-form-item label="Case Name">
          <el-input v-model="previewingCase.caseName" />
        </el-form-item>
        <el-form-item label="Request">
          <el-tag>{{ previewingCase.method }}</el-tag>
          <span style="margin-left: 10px;">{{ previewingCase.url }}</span>
        </el-form-item>
        <el-form-item label="Body">
          <monaco-editor :model-value="previewingCase.body" language="json" read-only height="150px" />
        </el-form-item>
        <el-form-item label="Assertion">
          <monaco-editor :model-value="previewingCase.assertionScript" language="groovy" read-only height="150px" />
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- Step Library Drawer -->
    <el-drawer v-model="showLibraryDrawer" title="Step Library" size="30%">
      <div class="library-content">
        <el-input v-model="templateSearch" placeholder="Search templates..." prefix-icon="Search" style="margin-bottom: 15px;" />
        
        <div class="template-list">
          <div v-for="tpl in stepTemplates" :key="tpl.id" class="template-item">
            <div class="tpl-info">
              <el-tag size="small" :type="tpl.method === 'GET' ? 'success' : 'primary'">{{ tpl.method }}</el-tag>
              <span class="tpl-name">{{ tpl.name }}</span>
            </div>
            <el-button type="primary" size="small" link @click="importTemplate(tpl)">Add</el-button>
          </div>
          <div v-if="stepTemplates.length === 0" class="empty-tpl">
             No templates found. Go to Library to create one.
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- Execution Result Drawer -->
    <el-drawer v-model="showResult" :title="resultTitle" size="50%">
      <div v-if="store.executionResult" class="result-content">
        <!-- Dry Run Result -->
        <div v-if="isDryRunResult">
          <h3>Resolved URL</h3>
          <pre>{{ store.executionResult.resolvedUrl }}</pre>
          
          <h3>Resolved Body</h3>
          <monaco-editor :model-value="store.executionResult.resolvedBody" language="json" read-only height="300px" />
          
          <h3>Variables</h3>
          <pre>{{ JSON.stringify(store.executionResult.variables, null, 2) }}</pre>
        </div>

        <!-- Actual Execution Result -->
        <div v-else>
          <div class="result-summary">
            <el-tag :type="store.executionResult.status === 'PASS' ? 'success' : 'danger'" size="large" style="margin-right: 20px;">
              {{ store.executionResult.status }}
            </el-tag>
            <span style="margin-right: 20px;"><strong>Duration:</strong> {{ store.executionResult.duration }} ms</span>
            <span v-if="store.executionResult.statusCode"><strong>Status Code:</strong> {{ store.executionResult.statusCode }}</span>
          </div>

          <el-divider />

          <div v-if="store.executionResult.message" class="result-message">
            <h3>Message</h3>
            <p>{{ store.executionResult.message }}</p>
          </div>

          <div v-if="store.executionResult.detail" class="result-detail">
            <h3>Detail</h3>
            <pre>{{ store.executionResult.detail }}</pre>
          </div>

          <h3>Step Execution Details</h3>
          <el-collapse v-if="store.executionResult.logs && store.executionResult.logs.length > 0" accordion>
            <el-collapse-item v-for="(log, index) in store.executionResult.logs" :key="index" :name="index">
              <template #title>
                <div class="log-title">
                  <el-tag size="small" :type="log.responseStatus >= 200 && log.responseStatus < 300 ? 'success' : (log.responseStatus === 0 ? 'warning' : 'danger')" style="margin-right: 10px;">
                    {{ log.responseStatus || 'ERR' }}
                  </el-tag>
                  <span style="font-weight: bold;">{{ log.stepName || `Step ${index + 1}` }}</span>
                  <span style="margin-left: 10px; color: #909399; font-size: 12px;">{{ log.requestUrl || 'N/A' }}</span>
                </div>
              </template>
              
              <el-tabs type="card">
                <el-tab-pane label="Request">
                  <div class="log-detail-item">
                     <h4>URL</h4>
                     <pre>{{ log.requestUrl || 'N/A' }}</pre>
                  </div>
                  <div class="log-detail-item">
                     <h4>Headers</h4>
                     <monaco-editor :model-value="formatJson(log.requestHeaders)" language="json" read-only height="150px" :show-toolbar="true" />
                  </div>
                  <div class="log-detail-item">
                     <h4>Body</h4>
                     <monaco-editor :model-value="log.requestBody || ''" language="json" read-only height="150px" :show-toolbar="true" />
                  </div>
                </el-tab-pane>
                <el-tab-pane label="Response">
                  <div class="log-detail-item">
                     <h4>Status Code</h4>
                     <el-tag :type="log.responseStatus >= 200 && log.responseStatus < 300 ? 'success' : (log.responseStatus === 0 ? 'warning' : 'danger')">
                       {{ log.responseStatus || 'N/A' }}
                     </el-tag>
                  </div>
                  <div class="log-detail-item">
                     <h4>Headers</h4>
                     <monaco-editor :model-value="formatJson(log.responseHeaders)" language="json" read-only height="150px" :show-toolbar="true" />
                  </div>
                  <div class="log-detail-item">
                     <div class="section-header">
                        <h4>Body</h4>
                        <el-button 
                          type="warning" 
                          size="small" 
                          link 
                          @click="handleSuggestAssertions(log.responseBody, index)"
                          :loading="suggestingAssertions === index"
                        >
                          <el-icon><MagicStick /></el-icon> Suggest Assertions
                        </el-button>
                     </div>
                     <monaco-editor :model-value="log.responseBody || ''" language="json" read-only height="200px" :show-toolbar="true" />
                  </div>
                </el-tab-pane>
                <el-tab-pane label="Extractors" v-if="log.extractors && log.extractors.length > 0">
                  <el-table :data="log.extractors" border>
                    <el-table-column prop="type" label="Type" width="100" />
                    <el-table-column prop="expression" label="Expression" />
                    <el-table-column prop="variableName" label="Variable" />
                    <el-table-column prop="value" label="Extracted Value" show-overflow-tooltip />
                  </el-table>
                </el-tab-pane>
                <el-tab-pane label="Assertions" v-if="log.assertions && log.assertions.length > 0">
                  <el-table :data="log.assertions" border>
                    <el-table-column prop="type" label="Type" width="100" />
                    <el-table-column prop="expression" label="Expression" />
                    <el-table-column prop="expectedValue" label="Expected" />
                    <el-table-column prop="actualValue" label="Actual" />
                    <el-table-column prop="passed" label="Result">
                      <template #default="{ row }">
                        <el-tag :type="row.passed ? 'success' : 'danger'">
                          {{ row.passed ? 'PASS' : 'FAIL' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-tab-pane>
              </el-tabs>
            </el-collapse-item>
          </el-collapse>
          <div v-else class="no-logs">
            <el-empty description="No step execution logs available" />
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, View, VideoPlay, Check, Link, DocumentCopy, MagicStick } from '@element-plus/icons-vue'
import { useTestCaseStore } from '../stores/testCaseStore'
import { projectApi } from '../api/project'
import { testModuleApi } from '../api/testModule'
import { environmentApi } from '../api/environment'
import { stepTemplateApi } from '../api/stepTemplate'
import { importApi } from '../api/import'
import { aiApi } from '../api/ai'
import StepList from '../components/StepList.vue'
import StepDetail from '../components/StepDetail.vue'
import MonacoEditor from '../components/MonacoEditor.vue'
import VariableInput from '../components/VariableInput.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onUnmounted } from 'vue'

const route = useRoute()
const router = useRouter()
const store = useTestCaseStore()

const isEditMode = computed(() => !!route.params.id)
const selectedStepIndex = ref(-1)
const activeTab = ref('settings')
const isMounted = ref(true)
const projects = ref([])
const modules = ref([])
const environments = ref([])
const showDryRunDialog = ref(false)
const showExecuteDialog = ref(false)
const dryRunEnv = ref('')
const executeEnv = ref('')
const showResult = ref(false)
const isDryRun = ref(false)

// Step Library State
const showLibraryDrawer = ref(false)
const stepTemplates = ref([])
const templateSearch = ref('')

// cURL Import State
const showCurlDialog = ref(false)
const curlCommand = ref('')
const importingCurl = ref(false)
const curlPlaceholder = "curl -X POST 'https://api.example.com/users' -H 'Content-Type: application/json' -d '{\"name\":\"test\"}'"

// AI State
const showAiDialog = ref(false)
const generatingAi = ref(false)
const generatedCases = ref([])
const selectedAiCases = ref([])
const importingAi = ref(false)
const showAiPreview = ref(false)
const previewingCase = ref(null)
const suggestingAssertions = ref(-1) // -1 or step index
const mockingBody = ref(-2) // -1 for case, >=0 for step, -2 for idle

const handleMockBody = async (index) => {
    const url = index === -1 ? store.currentCase.url : store.currentCase.steps[index].url
    const method = index === -1 ? store.currentCase.method : store.currentCase.steps[index].method
    const name = index === -1 ? store.currentCase.caseName : store.currentCase.steps[index].stepName
    
    if (!url) {
        ElMessage.warning('Please enter a URL first')
        return
    }

    mockingBody.value = index
    try {
        const result = await aiApi.generateMock({
            url,
            method,
            description: name
        })
        if (index === -1) {
            store.currentCase.body = result
        } else {
            const steps = [...store.currentCase.steps]
            steps[index].body = result
            store.currentCase.steps = steps
        }
        ElMessage.success('Mock body generated')
    } catch (error) {
        ElMessage.error('AI Mock Generation failed: ' + error.message)
    } finally {
        mockingBody.value = -2
    }
}

const handleAiGenerate = async () => {
    ElMessage.info('Coming soon')
    return
    
    /* Original implementation
    if (!store.currentCase.url) {
        ElMessage.warning('Please enter a URL first')
        return
    }
    generatingAi.value = true
    try {
        const result = await aiApi.generateCases({
            url: store.currentCase.url,
            method: store.currentCase.method,
            headers: store.currentCase.headers,
            body: store.currentCase.body,
            count: 5
        })
        generatedCases.value = result
    } catch (error) {
        ElMessage.error('AI Generation failed: ' + (error.response?.data?.error || error.message))
    } finally {
        generatingAi.value = false
    }
    */
}

const handleAiSelection = (selection) => {
    selectedAiCases.value = selection
}

const previewAiCase = (caseObj) => {
    previewingCase.value = caseObj
    showAiPreview.value = true
}

const importSelectedAiCases = async () => {
    if (!store.currentCase.projectId || !store.currentCase.moduleId) {
        ElMessage.warning('Please select Project and Module first')
        return
    }
    
    importingAi.value = true
    try {
        let successCount = 0
        for (const aiCase of selectedAiCases.value) {
            const caseData = {
                ...aiCase,
                projectId: store.currentCase.projectId,
                moduleId: store.currentCase.moduleId,
                isActive: true
            }
            // Use store or API to save
            await store.saveCaseDirectly(caseData)
            successCount++
        }
        ElMessage.success(`Successfully imported ${successCount} test cases`)
        showAiDialog.value = false
        // Refresh module data if needed? Or just let user see them in the list.
    } catch (error) {
        ElMessage.error('Failed to import some cases: ' + error.message)
    } finally {
        importingAi.value = false
    }
}

const handleSuggestAssertions = async (body, index) => {
    if (!body) {
        ElMessage.warning('No response body to analyze')
        return
    }
    suggestingAssertions.value = index
    try {
        const result = await aiApi.suggestAssertions({ responseBody: body })
        // We can either directly append to the script or show a dialog.
        // For now, let's show a MessageBox with the code.
        await ElMessageBox.confirm(
            result,
            'AI Suggested Assertions',
            {
                confirmButtonText: 'Apply to Script',
                cancelButtonText: 'Copy to Clipboard',
                distinguishCancelAndClose: true,
                type: 'info',
                customClass: 'assertion-suggestion-box'
            }
        ).then(() => {
            // Apply logic
            if (index === -1) {
                // Case level
                store.currentCase.assertionScript = (store.currentCase.assertionScript || '') + '\n' + result
            } else {
                // Step level
                const steps = [...store.currentCase.steps]
                steps[index].assertionScript = (steps[index].assertionScript || '') + '\n' + result
                store.currentCase.steps = steps
            }
            ElMessage.success('Assertions applied')
        }).catch((action) => {
            if (action === 'cancel') {
                navigator.clipboard.writeText(result)
                ElMessage.success('Copied to clipboard')
            }
        })
    } catch (error) {
        ElMessage.error('AI Suggestion failed: ' + error.message)
    } finally {
        suggestingAssertions.value = -1
    }
}

const openLibrary = async () => {
    showLibraryDrawer.value = true
    try {
        // Fetch templates for current project (if available) or all
        const projectId = store.currentCase.projectId
        stepTemplates.value = await stepTemplateApi.getAll({ projectId })
    } catch (e) {
        ElMessage.error('Failed to load step templates')
    }
}

const importTemplate = (template) => {
    // Copy logic
    const newStep = {
        stepName: template.name, // Use template name
        method: template.method,
        url: template.url,
        headers: template.headers || '{}',
        body: template.body || '',
        assertionScript: template.assertionScript || '',
        enabled: true,
        // Reset ID and Order
        stepOrder: store.currentCase.steps.length + 1
    }
    // Add to store
    store.currentCase.steps = [...store.currentCase.steps, newStep]
    ElMessage.success('Step added from library')
    // Don't close drawer to allow multiple adds
}

const handleImportCurl = async () => {
    if (!curlCommand.value.trim()) {
        ElMessage.warning('Please paste a cURL command')
        return
    }
    // No validation for project and module here - will be validated on save
    importingCurl.value = true
    try {
        // Always use parseOnly mode - just parse and fill the form, don't create case
        // User needs to click Save button to actually save the case
        // Pass null for projectId and moduleId since we're just parsing
        const result = await importApi.importCurl(
            null, // projectId - not needed for parsing
            null, // moduleId - not needed for parsing
            curlCommand.value,
            false, // asStep = false
            null,
            true // parseOnly = true, just parse and return data
        )
        
        // Update current case with parsed data
        // When parseOnly=true, the response structure is: { method, url, headers, body } at root level
        if (result.method) {
            // Data is at root level (parseOnly mode)
            store.currentCase.method = result.method || store.currentCase.method
            store.currentCase.url = result.url || store.currentCase.url
            store.currentCase.headers = result.headers || store.currentCase.headers || '{}'
            store.currentCase.body = result.body != null ? result.body : ''
            
            // If case name is empty, set a default name
            if (!store.currentCase.caseName || store.currentCase.caseName.trim() === '') {
                const urlParts = result.url.split('/').filter(p => p)
                const lastPart = urlParts[urlParts.length - 1] || 'Imported Case'
                store.currentCase.caseName = `${result.method} ${lastPart}`
            }
            
            ElMessage.success('cURL imported successfully. Please review and click Save to save the case.')
        } else if (result.data && result.data.method) {
            // Fallback: Data is nested in result.data
            store.currentCase.method = result.data.method || store.currentCase.method
            store.currentCase.url = result.data.url || store.currentCase.url
            store.currentCase.headers = result.data.headers || store.currentCase.headers || '{}'
            store.currentCase.body = result.data.body != null ? result.data.body : ''
            
            // If case name is empty, set a default name
            if (!store.currentCase.caseName || store.currentCase.caseName.trim() === '') {
                const urlParts = result.data.url.split('/').filter(p => p)
                const lastPart = urlParts[urlParts.length - 1] || 'Imported Case'
                store.currentCase.caseName = `${result.data.method} ${lastPart}`
            }
            
            ElMessage.success('cURL imported successfully. Please review and click Save to save the case.')
        } else {
            ElMessage.warning('cURL parsed but no data returned')
        }
        
        showCurlDialog.value = false
        curlCommand.value = ''
        // Switch to settings tab to show the imported data
        activeTab.value = 'settings'
    } catch (error) {
        ElMessage.error('Failed to import cURL: ' + (error.response?.data?.error || error.message))
    } finally {
        importingCurl.value = false
    }
}

const resultTitle = computed(() => isDryRun.value ? 'Dry Run Result' : 'Execution Result')
const isDryRunResult = computed(() => isDryRun.value)

const formatJson = (str) => {
  if (!str) return '{}'
  try {
    const parsed = typeof str === 'string' ? JSON.parse(str) : str
    return JSON.stringify(parsed, null, 2)
  } catch (e) {
    return str
  }
}

const currentStep = computed({
  get: () => selectedStepIndex.value >= 0 ? store.currentCase.steps[selectedStepIndex.value] : null,
  set: (val) => {
    if (selectedStepIndex.value >= 0) {
      store.updateStep(selectedStepIndex.value, val)
    }
  }
})

const filteredModules = computed(() => {
  if (!store.currentCase.projectId || !modules.value) return []
  return modules.value.filter(m => m.project && (typeof m.project === 'object' ? m.project.id === store.currentCase.projectId : m.project === store.currentCase.projectId))
})

watch(selectedStepIndex, (val) => {
  if (val >= 0) {
    activeTab.value = 'step'
  } else {
    activeTab.value = 'settings'
  }
})

const loadData = async () => {
  try {
    // Load initial data in parallel
    const [projectsData, modulesData, environmentsData] = await Promise.all([
      projectApi.getAll(),
      testModuleApi.getAll(),
      environmentApi.getAll()
    ])
    
    projects.value = projectsData
    modules.value = modulesData
    environments.value = environmentsData
    
    // Set default environment if available
    if (environments.value.length > 0) {
      dryRunEnv.value = environments.value[0].envName
      executeEnv.value = environments.value[0].envName
    }
    
    if (isEditMode.value) {
      try {
        await store.loadCase(route.params.id)
        // Set project/module IDs correctly - handle all possible data structures
        if (store.currentCase.module) {
          if (typeof store.currentCase.module === 'object') {
            store.currentCase.moduleId = store.currentCase.module.id
            // Try to get project from module
            if (store.currentCase.module.project) {
              if (typeof store.currentCase.module.project === 'object') {
                store.currentCase.projectId = store.currentCase.module.project.id
              } else {
                store.currentCase.projectId = store.currentCase.module.project
              }
            }
          } else {
            store.currentCase.moduleId = store.currentCase.module
          }
        }
        // If no project found, try to infer from moduleId
        if (!store.currentCase.projectId && store.currentCase.moduleId) {
          const module = modules.value.find(m => m.id === store.currentCase.moduleId)
          if (module && module.project) {
            store.currentCase.projectId = typeof module.project === 'object' ? module.project.id : module.project
          }
        }
      } catch (loadError) {
        console.error('Error loading test case:', loadError)
        ElMessage.error('Failed to load test case: ' + (loadError.message || 'Unknown error'))
        router.push('/cases')
      }
    } else {
      store.resetCase()
      // Default to settings tab for new cases
      activeTab.value = 'settings'
    }
  } catch (error) {
    console.error('Failed to load initial data:', error)
    ElMessage.error('Failed to load projects and modules')
  }
}

const handleStepSelect = (index) => {
  selectedStepIndex.value = index
  nextTick(() => {
    activeTab.value = 'step'
  })
}

const loadModules = () => {
  store.currentCase.moduleId = null
}

const handleRemoveStep = (index) => {
  store.removeStep(index)
  if (selectedStepIndex.value >= store.currentCase.steps.length) {
    selectedStepIndex.value = store.currentCase.steps.length - 1
  }
}

const handleUpdateStep = (index, updates) => {
  const step = store.currentCase.steps[index]
  if (step) {
    store.updateStep(index, { ...step, ...updates })
  }
}

const handleSave = async () => {
  try {
    // Validate required fields
    if (!store.currentCase.caseName || !store.currentCase.caseName.trim()) {
      ElMessage.warning('Please enter a case name')
      activeTab.value = 'settings'
      return
    }
    if (!store.currentCase.url || !store.currentCase.url.trim()) {
      ElMessage.warning('Please enter a request URL')
      activeTab.value = 'settings'
      return
    }
    // Validate project and module - required for saving
    if (!store.currentCase.projectId) {
      ElMessage.warning('Please select a project')
      activeTab.value = 'settings'
      return
    }
    if (!store.currentCase.moduleId) {
      ElMessage.warning('Please select a module')
      activeTab.value = 'settings'
      return
    }
    
    await store.saveCase()
    ElMessage.success(isEditMode.value ? 'Test case updated successfully' : 'Test case created successfully')
    
    if (!isEditMode.value) {
      router.replace(`/testing/cases/${store.currentCase.id}/edit`)
    }
  } catch (error) {
    console.error('Save failed:', error)
    ElMessage.error('Failed to save test case: ' + (error.response?.data?.message || error.message))
  }
}

const handleDryRun = () => {
  showDryRunDialog.value = true
}

const handleRun = () => {
  showExecuteDialog.value = true
}

const executeDryRun = async () => {
  await store.runDryRun(dryRunEnv.value)
  isDryRun.value = true
  showDryRunDialog.value = false
  showResult.value = true
}

const executeRun = async () => {
  await store.executeCase(executeEnv.value)
  isDryRun.value = false
  showExecuteDialog.value = false
  showResult.value = true
}


onMounted(() => {
  // Ensure isMounted is set to true when component mounts
  isMounted.value = true
  loadData()
})

onBeforeUnmount(() => {
  // Don't modify reactive state during unmount as it can cause
  // Element Plus components (like menu) to access destroyed DOM elements
  // Vue will automatically handle component cleanup
  // isMounted will be reset when component mounts again
})
</script>

<style scoped>
.test-case-editor {
  height: calc(100vh - 80px); /* Adjust based on layout header */
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.editor-header {
  padding: 10px 20px;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
}

.editor-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 300px;
  border-right: 1px solid #dcdfe6;
  background-color: #fcfcfc;
}

.content-area {
  flex: 1;
  background-color: #f5f7fa;
  padding: 10px;
  overflow: hidden;
}

.main-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.el-tabs__content) {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.settings-form {
  padding: 20px;
  max-width: 800px;
}

.result-content {
  padding: 20px;
}

.result-summary {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.result-message,
.result-detail {
  margin-bottom: 20px;
}

.result-message h3,
.result-detail h3 {
  margin-bottom: 10px;
  color: #303133;
}

.result-content pre {
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.log-title {
  display: flex;
  align-items: center;
  width: 100%;
}

.log-detail-item {
  margin-bottom: 20px;
}

.log-detail-item h4 {
  margin-bottom: 10px;
  color: #606266;
  font-size: 14px;
}

.no-logs {
  padding: 40px;
  text-align: center;
}

.help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.template-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
}
.template-item:hover {
  background-color: #f5f7fa;
}
.tpl-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tpl-name {
  font-size: 14px;
  font-weight: 500;
}
.empty-tpl {
  padding: 20px;
  text-align: center;
  color: #909399;
}

.log-title {
  display: flex;
  align-items: center;
  width: 100%;
}
.log-detail-item h4 {
  margin: 5px 0;
  font-size: 13px;
  color: #606266;
}
</style>
