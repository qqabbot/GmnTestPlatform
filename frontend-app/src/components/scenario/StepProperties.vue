<template>
  <div class="step-properties">
    <div class="panel-header">
       <h3>Properties</h3>
    </div>
    
    <div v-if="!step" class="empty-props">
       <el-empty description="Select a step to edit properties" image-size="60" />
    </div>
    
    <div v-else class="props-form">
       <el-form label-position="top" size="small">
          <el-form-item label="Step Name">
             <el-input v-model="step.name" placeholder="Custom step name" />
          </el-form-item>
          
          <!-- Step Enabled/Disabled -->
          <el-form-item label="Enabled">
             <el-switch v-model="stepEnabled" />
             <div class="field-help">Disable this step to skip it during execution</div>
          </el-form-item>
          
          <!-- Loop Properties -->
          <div v-if="step.type === 'LOOP'">
             <el-divider content-position="left">Loop Config</el-divider>
             <el-form-item label="Loop Mode">
                <el-radio-group v-model="loopMode">
                   <el-radio label="count">Count Loop</el-radio>
                   <el-radio label="foreach">ForEach Loop</el-radio>
                   <el-radio label="while">While Loop</el-radio>
                </el-radio-group>
             </el-form-item>

             <div v-if="loopMode === 'count'">
                <el-form-item label="Repeat Count">
                   <el-input-number v-model="loopCount" :min="1" :max="1000" />
                </el-form-item>
             </div>

             <div v-else-if="loopMode === 'foreach'">
                <el-form-item label="Iterable Variable">
                   <el-input v-model="iterableVar" placeholder="e.g. userList" />
                   <div class="field-help">Variable name containing a list/collection</div>
                </el-form-item>
                <el-row :gutter="10">
                   <el-col :span="12">
                      <el-form-item label="Item Variable">
                         <el-input v-model="itemVar" placeholder="item" />
                      </el-form-item>
                   </el-col>
                   <el-col :span="12">
                      <el-form-item label="Index Variable">
                         <el-input v-model="indexVar" placeholder="index" />
                      </el-form-item>
                   </el-col>
                </el-row>
             </div>

             <div v-else-if="loopMode === 'while'">
                <el-form-item label="Condition Expression">
                   <el-input v-model="whileCondition" type="textarea" :rows="2" placeholder="${status} != 'COMPLETE'" />
                   <div class="field-help">Groovy script returning a boolean. Loop continues while condition is true.</div>
                </el-form-item>
                <el-form-item label="Max Iterations">
                   <el-input-number v-model="maxIterations" :min="1" :max="10000" />
                   <div class="field-help">Maximum number of iterations to prevent infinite loops</div>
                </el-form-item>
             </div>
          </div>
          
           <!-- If Properties -->
          <div v-if="step.type === 'IF'">
             <el-divider content-position="left">Condition</el-divider>
             <el-form-item label="Condition Expression">
                <el-input v-model="ifCondition" type="textarea" :rows="2" placeholder="${status} == 'SUCCESS'" />
                <div class="field-help">Groovy script returning a boolean. Access variables via ${name}.</div>
             </el-form-item>
          </div>

           <!-- Wait Properties -->
          <div v-if="step.type === 'WAIT'">
             <el-divider content-position="left">Wait Config</el-divider>
              <el-form-item label="Wait Time (ms)">
                <el-input-number v-model="waitTime" :step="1000" :min="0" />
             </el-form-item>
          </div>
          
           <!-- Case Properties: Data Overrides, Extraction, Assertions -->
          <div v-if="step.type === 'CASE'">
              <el-divider content-position="left">Data Overrides</el-divider>
              <el-alert 
                title="These overrides apply ONLY to this step in the scenario. The original Test Case remains unchanged." 
                type="info" 
                :closable="false" 
                style="margin-bottom: 12px;"
              />
              
              <el-form-item label="Request Parameters (JSON)">
                 <el-input 
                   v-model="overrideParams" 
                   type="textarea" 
                   :rows="3" 
                   placeholder='{"key": "value"}'
                 />
              </el-form-item>
              
              <el-form-item label="Request Headers (JSON)">
                 <el-input 
                   v-model="overrideHeaders" 
                   type="textarea" 
                   :rows="3" 
                   placeholder='{"Authorization": "Bearer ${token}"}'
                 />
              </el-form-item>
              
              <el-form-item label="Request Body (JSON)">
                 <el-input 
                   v-model="overrideBody" 
                   type="textarea" 
                   :rows="4" 
                   placeholder='{"userId": "${userId}"}'
                 />
              </el-form-item>
              
              <el-divider content-position="left">Variable Extraction</el-divider>
              <el-form-item label="Extract Variables from Response">
                 <el-input 
                   v-model="extractConfig" 
                   type="textarea" 
                   :rows="4" 
                   placeholder='[{"varName": "orderId", "jsonPath": "$.data.id"}]'
                 />
                 <template #help>
                   <div style="font-size: 12px; color: #909399; margin-top: 4px;">
                     Format: [{"varName": "myVar", "jsonPath": "$.path.to.value"}]
                   </div>
                 </template>
              </el-form-item>
              
              <el-divider content-position="left">Visual Assertions</el-divider>
              <div class="assertion-builder">
                 <el-button size="small" type="primary" icon="Plus" @click="addVisualAssertion" style="margin-bottom: 10px;">Add Rule</el-button>
                 <el-table :data="visualAssertions" size="small" border stripe style="width: 100%">
                    <el-table-column label="Source" width="90">
                       <template #default="scope">
                          <el-select v-model="scope.row.source" size="small" class="full-width">
                             <el-option label="Body" value="JSON_BODY" />
                             <el-option label="Header" value="RESPONSE_HEADER" />
                             <el-option label="Status" value="STATUS_CODE" />
                          </el-select>
                       </template>
                    </el-table-column>
                    <el-table-column label="Property/Key" min-width="120">
                       <template #default="scope">
                          <el-input v-model="scope.row.property" size="small" placeholder="$.id" />
                       </template>
                    </el-table-column>
                    <el-table-column label="Op" width="90">
                       <template #default="scope">
                          <el-select v-model="scope.row.operator" size="small" class="full-width">
                             <el-option label="==" value="EQUALS" />
                             <el-option label="!=" value="NOT_EQUALS" />
                             <el-option label="Contains" value="CONTAINS" />
                             <el-option label="Exists" value="EXISTS" />
                             <el-option label="> " value="GREATER_THAN" />
                             <el-option label="< " value="LESS_THAN" />
                          </el-select>
                       </template>
                    </el-table-column>
                    <el-table-column label="Value" min-width="90">
                       <template #default="scope">
                          <el-input v-model="scope.row.expected" size="small" placeholder="Exp" />
                       </template>
                    </el-table-column>
                    <el-table-column width="40" align="center">
                       <template #default="scope">
                          <el-button link type="danger" icon="Delete" @click="removeVisualAssertion(scope.$index)" />
                       </template>
                    </el-table-column>
                 </el-table>
              </div>

              <el-divider content-position="left">Assertion Script (Advanced)</el-divider>
              <el-form-item label="Groovy Script">
                 <el-input 
                   v-model="assertionScript" 
                   type="textarea" 
                   :rows="3" 
                   placeholder='assert response.code == 200'
                 />
              </el-form-item>
          </div>

       </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps({
    step: Object
})

// Helper to access controlLogic safely
const controlLogic = computed(() => {
    if (!props.step) return {}
    if (!props.step.controlLogic) props.step.controlLogic = {}
    return props.step.controlLogic
})

// Helper to access dataOverrides safely
const dataOverrides = computed(() => {
    if (!props.step) return {}
    if (!props.step.dataOverrides) props.step.dataOverrides = {}
    return props.step.dataOverrides
})

// Loop Config
const loopMode = computed({
    get: () => controlLogic.value.mode || 'count',
    set: (val) => { props.step.controlLogic = { ...controlLogic.value, mode: val } }
})
const loopCount = computed({
    get: () => controlLogic.value.count || 1,
    set: (val) => { props.step.controlLogic = { ...controlLogic.value, count: val } }
})
const iterableVar = computed({
    get: () => controlLogic.value.iterableVar || '',
    set: (val) => { props.step.controlLogic = { ...controlLogic.value, iterableVar: val } }
})
const itemVar = computed({
    get: () => controlLogic.value.itemVar || 'item',
    set: (val) => { props.step.controlLogic = { ...controlLogic.value, itemVar: val } }
})
const indexVar = computed({
    get: () => controlLogic.value.indexVar || 'index',
    set: (val) => { props.step.controlLogic = { ...controlLogic.value, indexVar: val } }
})

// While Loop Config
const whileCondition = computed({
    get: () => controlLogic.value.condition || '',
    set: (val) => { props.step.controlLogic = { ...controlLogic.value, condition: val } }
})
const maxIterations = computed({
    get: () => controlLogic.value.maxIterations || 100,
    set: (val) => { props.step.controlLogic = { ...controlLogic.value, maxIterations: val } }
})

// Step Enabled/Disabled
const stepEnabled = computed({
    get: () => {
        if (!dataOverrides.value.hasOwnProperty('enabled')) {
            return true // Default to enabled
        }
        return dataOverrides.value.enabled !== false
    },
    set: (val) => {
        props.step.dataOverrides = { ...dataOverrides.value, enabled: val }
    }
})

// If Config
const ifCondition = computed({
    get: () => controlLogic.value.condition || '',
    set: (val) => { props.step.controlLogic = { ...controlLogic.value, condition: val } }
})

// Wait Config
const waitTime = computed({
    get: () => controlLogic.value.waitMs || 1000,
    set: (val) => { props.step.controlLogic = { ...controlLogic.value, waitMs: val } }
})

// Data Overrides for CASE steps
const overrideParams = computed({
    get: () => {
        const params = dataOverrides.value.params
        return params ? JSON.stringify(params, null, 2) : ''
    },
    set: (val) => {
        try {
            const parsed = val.trim() ? JSON.parse(val) : null
            props.step.dataOverrides = { ...dataOverrides.value, params: parsed }
        } catch (e) {
            // Keep invalid JSON as-is, user will see error on save
        }
    }
})

const overrideHeaders = computed({
    get: () => {
        const headers = dataOverrides.value.headers
        return headers ? JSON.stringify(headers, null, 2) : ''
    },
    set: (val) => {
        try {
            const parsed = val.trim() ? JSON.parse(val) : null
            props.step.dataOverrides = { ...dataOverrides.value, headers: parsed }
        } catch (e) {
            // Keep invalid JSON
        }
    }
})

const overrideBody = computed({
    get: () => {
        const body = dataOverrides.value.body
        return body ? JSON.stringify(body, null, 2) : ''
    },
    set: (val) => {
        try {
            const parsed = val.trim() ? JSON.parse(val) : null
            props.step.dataOverrides = { ...dataOverrides.value, body: parsed }
        } catch (e) {
            // Keep invalid JSON
        }
    }
})

// Variable Extraction
const extractConfig = computed({
    get: () => {
        const extract = dataOverrides.value.extract
        return extract ? JSON.stringify(extract, null, 2) : ''
    },
    set: (val) => {
        try {
            const parsed = val.trim() ? JSON.parse(val) : null
            props.step.dataOverrides = { ...dataOverrides.value, extract: parsed }
        } catch (e) {
            // Keep invalid JSON
        }
    }
})

// Assertion Script
const assertionScript = computed({
    get: () => dataOverrides.value.assertionScript || '',
    set: (val) => {
        props.step.dataOverrides = { ...dataOverrides.value, assertionScript: val }
    }
})

// Visual Assertions
const visualAssertions = computed({
    get: () => dataOverrides.value.visualAssertions || [],
    set: (val) => {
        props.step.dataOverrides = { ...dataOverrides.value, visualAssertions: val }
    }
})

const addVisualAssertion = () => {
    const current = [...visualAssertions.value]
    current.push({
        source: 'JSON_BODY',
        property: '',
        operator: 'EQUALS',
        expected: ''
    })
    visualAssertions.value = current
}

const removeVisualAssertion = (index) => {
    const current = [...visualAssertions.value]
    current.splice(index, 1)
    visualAssertions.value = current
}

</script>

<style scoped>
.step-properties {
    height: 100%;
    background: #fff;
    border-left: 1px solid #dcdfe6;
    display: flex;
    flex-direction: column;
}
.panel-header {
    padding: 10px 15px;
    border-bottom: 1px solid #f0f2f5;
}
.props-form {
    padding: 20px;
    overflow-y: auto;
    flex: 1;
}
.empty-props {
    display: flex;
    justify-content: center;
    align-items: center;
    flex: 1;
    color: #909399;
}
.field-help {
    font-size: 11px;
    color: #909399;
    margin-top: 4px;
}
.full-width {
    width: 100%;
}
:deep(.el-form-item) {
    margin-bottom: 18px;
}
:deep(.el-divider--horizontal) {
    margin: 24px 0 16px 0;
}
</style>
