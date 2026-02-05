<template>
  <div class="cron-simple-container">
    <el-tabs v-model="activeMode" type="border-card">
      <!-- Simple Mode -->
      <el-tab-pane label="常用模式 (Simple)" name="simple">
        <el-form label-width="100px" size="default">
          <el-form-item label="执行频率">
            <el-select v-model="simpleConfig.type" placeholder="请选择频率" style="width: 100%">
              <el-option label="每隔 X 分钟" value="minutes" />
              <el-option label="每小时" value="hourly" />
              <el-option label="每天" value="daily" />
              <el-option label="每周" value="weekly" />
              <el-option label="每月" value="monthly" />
            </el-select>
          </el-form-item>

          <!-- Minutes -->
          <template v-if="simpleConfig.type === 'minutes'">
            <el-form-item label="间隔时间">
              每隔 <el-input-number v-model="simpleConfig.minutes" :min="1" :max="59" /> 分钟执行一次
            </el-form-item>
          </template>

          <!-- Hourly -->
          <template v-if="simpleConfig.type === 'hourly'">
            <el-form-item label="执行时间">
              每小时的第 <el-input-number v-model="simpleConfig.minute" :min="0" :max="59" /> 分钟执行
            </el-form-item>
          </template>

          <!-- Daily -->
          <template v-if="simpleConfig.type === 'daily'">
            <el-form-item label="执行时间">
              <el-time-picker v-model="simpleConfig.time" format="HH:mm" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </template>

          <!-- Weekly -->
          <template v-if="simpleConfig.type === 'weekly'">
            <el-form-item label="执行日期">
              <el-checkbox-group v-model="simpleConfig.weekDays">
                <el-checkbox v-for="(label, val) in weekOptions" :key="val" :label="val">{{ label }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="执行时间">
              <el-time-picker v-model="simpleConfig.time" format="HH:mm" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </template>

          <!-- Monthly -->
          <template v-if="simpleConfig.type === 'monthly'">
            <el-form-item label="执行日期">
              每月的第 <el-input-number v-model="simpleConfig.day" :min="1" :max="31" /> 号
            </el-form-item>
            <el-form-item label="执行时间">
              <el-time-picker v-model="simpleConfig.time" format="HH:mm" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </template>
        </el-form>
      </el-tab-pane>

      <!-- Advanced Mode (Raw Input) -->
      <el-tab-pane label="高级模式 (Advanced)" name="advanced">
        <el-form label-position="top">
          <el-form-item label="Cron 表达式">
            <el-input v-model="advancedCron" placeholder="e.g. 0 0/10 * * * ?" type="textarea" :rows="3" />
          </el-form-item>
          <div class="help-text">
            格式: 秒 分 时 日 月 周 <br>
            示例: <code>0 30 9 * * ?</code> (每天 09:30:00 执行)
          </div>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <div class="footer">
      <div class="preview">
        <span>结果: </span>
        <code>{{ resultCron }}</code>
      </div>
      <el-button type="primary" @click="handleConfirm">确定并回填</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'

const props = defineProps({
  modelValue: String
})
const emit = defineEmits(['update:modelValue', 'confirm'])

const activeMode = ref('simple')
const advancedCron = ref(props.modelValue || '')
const weekOptions = { '2': '周一', '3': '周二', '4': '周三', '5': '周四', '6': '周五', '7': '周六', '1': '周日' }

const simpleConfig = reactive({
  type: 'daily',
  minutes: 10,
  minute: 0,
  time: new Date(2024, 0, 1, 9, 0),
  weekDays: ['2', '3', '4', '5', '6'],
  day: 1
})

const resultCron = computed(() => {
  if (activeMode.value === 'advanced') {
    return advancedCron.value
  }

  const { type, minutes, minute, time, weekDays, day } = simpleConfig
  const date = new Date(time)
  const HH = date.getHours()
  const mm = date.getMinutes()

  switch (type) {
    case 'minutes':
      return `0 0/${minutes} * * * ?`
    case 'hourly':
      return `0 ${minute} * * * ?`
    case 'daily':
      return `0 ${mm} ${HH} * * ?`
    case 'weekly':
      return `0 ${mm} ${HH} ? * ${weekDays.length ? weekDays.sort().join(',') : '*'}`
    case 'monthly':
      return `0 ${mm} ${HH} ${day} * ?`
    default:
      return '* * * * * ?'
  }
})

const handleConfirm = () => {
  emit('update:modelValue', resultCron.value)
  emit('confirm', resultCron.value)
}

// Watch for initial value or external changes
watch(() => props.modelValue, (newVal) => {
  if (newVal && newVal !== resultCron.value) {
    advancedCron.value = newVal
    // Optional: try to detect simple patterns and fill simpleConfig?
    // For now, if modified externally, we default to advanced mode to be safe.
    activeMode.value = 'advanced'
  }
}, { immediate: true })
</script>

<style scoped>
.cron-simple-container {
  background: #fff;
}
.help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.6;
}
.help-text code {
  color: #409eff;
  background: #f0f7ff;
  padding: 2px 4px;
  border-radius: 2px;
}
.footer {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}
.preview {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.preview code {
  background: #2d3748;
  color: #63b3ed;
  padding: 4px 10px;
  border-radius: 4px;
  font-family: monospace;
  margin-left: 8px;
}
</style>
