import { defineStore } from 'pinia'
import { ref } from 'vue'
import { environmentApi } from '../api/environment'
import { ElMessage } from 'element-plus'

export const useAppStore = defineStore('app', () => {
    const environments = ref([])
    const selectedEnv = ref('')
    const loading = ref(false)

    const loadEnvironments = async () => {
        loading.value = true
        try {
            const data = await environmentApi.getAll()
            environments.value = data

            // Restore from local storage or default to first
            const storedEnv = localStorage.getItem('gmn_selected_env')
            if (storedEnv && data.some(e => e.envName === storedEnv)) {
                selectedEnv.value = storedEnv
            } else if (data.length > 0) {
                selectedEnv.value = data[0].envName
                localStorage.setItem('gmn_selected_env', selectedEnv.value)
            }
        } catch (error) {
            console.error('Failed to load environments', error)
            ElMessage.error('Failed to load environments')
        } finally {
            loading.value = false
        }
    }

    const setSelectedEnv = (env) => {
        selectedEnv.value = env
        localStorage.setItem('gmn_selected_env', env)
    }

    const initialize = () => {
        loadEnvironments()
    }

    return {
        environments,
        selectedEnv,
        loading,
        loadEnvironments,
        setSelectedEnv,
        initialize
    }
})
