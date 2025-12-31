<template>
  <el-dialog
    v-model="visible"
    :title="`Override Step: ${stepData?.stepName || 'Unnamed'}`"
    width="600px"
    append-to-body
    destroy-on-close
  >
    <el-form v-if="stepData" label-width="120px">
      <el-alert
        title="Step Overrides"
        type="warning"
        description="Only fields modified here will be overridden in this test plan."
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />

      <el-form-item label="Original URL">
        <div class="original-value">{{ originalStep?.url }}</div>
      </el-form-item>

      <el-form-item label="Method Override">
        <el-radio-group v-model="stepData.method">
          <el-radio label="GET">GET</el-radio>
          <el-radio label="POST">POST</el-radio>
          <el-radio label="PUT">PUT</el-radio>
          <el-radio label="DELETE">DELETE</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="URL Override">
        <el-input v-model="stepData.url" placeholder="Enter URL override" />
      </el-form-item>

      <el-form-item label="Body Override">
        <el-input
          v-model="stepData.body"
          type="textarea"
          :rows="4"
          placeholder='{"key": "override-value"}'
        />
      </el-form-item>

      <el-form-item label="Assertion Override">
        <el-input
          v-model="stepData.assertionScript"
          type="textarea"
          :rows="4"
          placeholder="assert responseCode == 200"
        />
      </el-form-item>

      <el-form-item label="Enabled">
        <el-switch v-model="stepData.enabled" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">Cancel</el-button>
        <el-button type="danger" link @click="resetToOriginal">Reset to Original</el-button>
        <el-button type="primary" @click="confirm">Confirm Override</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  step: Object,
  override: Object
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const stepData = ref(null)
const originalStep = ref(null)

watch(() => props.modelValue, (val) => {
  if (val && props.step) {
    originalStep.value = JSON.parse(JSON.stringify(props.step))
    // Merge existing override if any, otherwise start with original values
    stepData.value = {
      stepId: props.step.id,
      method: props.override?.method || props.step.method,
      url: props.override?.url || props.step.url,
      body: props.override?.body || props.step.body,
      assertionScript: props.override?.assertionScript || props.step.assertionScript,
      enabled: props.override?.enabled !== undefined ? props.override.enabled : props.step.enabled
    }
  }
})

const resetToOriginal = () => {
  if (originalStep.value) {
    stepData.value.method = originalStep.value.method
    stepData.value.url = originalStep.value.url
    stepData.value.body = originalStep.value.body
    stepData.value.assertionScript = originalStep.value.assertionScript
    stepData.value.enabled = originalStep.value.enabled
  }
}

const confirm = () => {
  // Only return fields that are different from original
  const diff = { stepId: stepData.value.stepId }
  let changed = false

  if (stepData.value.method !== originalStep.value.method) { diff.method = stepData.value.method; changed = true }
  if (stepData.value.url !== originalStep.value.url) { diff.url = stepData.value.url; changed = true }
  if (stepData.value.body !== originalStep.value.body) { diff.body = stepData.value.body; changed = true }
  if (stepData.value.assertionScript !== originalStep.value.assertionScript) { diff.assertionScript = stepData.value.assertionScript; changed = true }
  if (stepData.value.enabled !== originalStep.value.enabled) { diff.enabled = stepData.value.enabled; changed = true }

  emit('confirm', changed ? diff : null)
  visible.value = false
}
</script>

<style scoped>
.original-value {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
  word-break: break-all;
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
}
</style>
