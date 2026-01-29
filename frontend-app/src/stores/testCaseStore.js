import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { testCaseApi } from '../api/testCase'
import { ElMessage } from 'element-plus'

export const useTestCaseStore = defineStore('testCase', () => {
    const currentCase = ref({
        id: null,
        caseName: '',
        projectId: null,
        moduleId: null,
        method: 'GET',
        url: '',
        headers: '{}',
        body: '',
        assertionScript: 'status_code == 200',
        isActive: true,
        steps: []
    })

    const loading = ref(false)
    const executionResult = ref(null)

    // Actions
    const resetCase = () => {
        currentCase.value = {
            id: null,
            caseName: '',
            projectId: null,
            moduleId: null,
            method: 'GET',
            url: '',
            headers: '{}',
            body: '',
            assertionScript: 'status_code == 200',
            isActive: true,
            steps: []
        }
        executionResult.value = null
    }

    const loadCase = async (id) => {
        loading.value = true
        try {
            const data = await testCaseApi.getById(id)
            currentCase.value = data
            // Ensure steps is an array
            if (!currentCase.value.steps) {
                currentCase.value.steps = []
            }
            // Ensure body is a string (not null)
            if (!currentCase.value.body) {
                currentCase.value.body = ''
            }
        } catch (error) {
            ElMessage.error('Failed to load test case')
            throw error  // Re-throw to allow caller to handle
        } finally {
            loading.value = false
        }
    }

    const saveCase = async (options = { suppressLoading: false }) => {
        if (!options.suppressLoading) loading.value = true
        try {
            // Validate moduleId before saving
            if (!currentCase.value.moduleId) {
                throw new Error('Module is required. Please select a module before saving.')
            }

            // Prepare clean payload
            const payload = {
                caseName: currentCase.value.caseName,
                method: currentCase.value.method,
                url: currentCase.value.url,
                headers: currentCase.value.headers || '{}',
                body: currentCase.value.body || '',
                precondition: currentCase.value.precondition,
                setupScript: currentCase.value.setupScript,
                assertionScript: currentCase.value.assertionScript,
                isActive: currentCase.value.isActive !== false,
                module: { id: currentCase.value.moduleId },
                steps: (currentCase.value.steps || []).map((step, index) => ({
                    stepName: step.stepName,
                    stepOrder: index + 1,
                    method: step.method,
                    url: step.url || '',
                    headers: step.headers || '{}',
                    body: step.body || '',
                    assertionScript: step.assertionScript || '',
                    enabled: step.enabled !== false,
                    referenceCaseId: step.referenceCaseId || null,
                    extractors: step.extractors || step._ui_extractors || [],
                    assertions: step.assertions || step._ui_assertions || []
                }))
            }

            if (currentCase.value.id) {
                await testCaseApi.update(currentCase.value.id, payload)
            } else {
                const res = await testCaseApi.create(payload)
                currentCase.value.id = res.id
            }
            ElMessage.success('Test case saved successfully')
        } catch (error) {
            ElMessage.error('Failed to save test case: ' + (error.message || error.response?.data?.message || 'Unknown error'))
            throw error
        } finally {
            if (!options.suppressLoading) loading.value = false
        }
    }

    const addStep = () => {
        const newStep = {
            stepName: 'New Step',
            method: 'GET',
            url: '',
            headers: '{}',
            body: '',
            enabled: true,
            stepOrder: currentCase.value.steps.length + 1
        }
        // 创建新数组避免无限循环
        currentCase.value.steps = [...currentCase.value.steps, newStep]
    }

    const removeStep = (index) => {
        // 创建新数组避免无限循环
        const newSteps = currentCase.value.steps.filter((_, idx) => idx !== index)
        // 重新排序
        newSteps.forEach((step, idx) => {
            step.stepOrder = idx + 1
        })
        currentCase.value.steps = newSteps
    }

    const updateStep = (index, stepData) => {
        const newSteps = [...currentCase.value.steps]
        newSteps[index] = { ...newSteps[index], ...stepData }
        currentCase.value.steps = newSteps
    }

    const runDryRun = async (envKey) => {
        loading.value = true
        try {
            const res = await testCaseApi.dryRun(currentCase.value.id, { envKey })
            executionResult.value = res
            ElMessage.success('Dry run completed')
        } catch (error) {
            ElMessage.error('Dry run failed')
        } finally {
            loading.value = false
        }
    }

    const executeCase = async (envKey) => {
        loading.value = true
        try {
            // For new cases (no ID yet), validate and save first
            if (!currentCase.value.id) {
                // Validate required fields before saving
                if (!currentCase.value.moduleId) {
                    throw new Error('Please select a project and module, then click Save before running the test case.')
                }
                await saveCase({ suppressLoading: true })
            }

            // Execute single case - pass caseId to execute only this case
            const res = await testCaseApi.execute({
                envKey,
                caseId: currentCase.value.id
            })
            // The execute endpoint returns a list of results.
            if (res && res.length > 0) {
                executionResult.value = res[0]
                ElMessage.success('Execution completed')
            } else {
                ElMessage.warning('No results returned')
            }
        } catch (error) {
            ElMessage.error('Execution failed: ' + (error.message || error.response?.data?.message || 'Unknown error'))
        } finally {
            loading.value = false
        }
    }

    const saveCaseDirectly = async (data) => {
        const payload = {
            caseName: data.caseName,
            method: data.method,
            url: data.url,
            headers: data.headers || '{}',
            body: data.body || '',
            assertionScript: data.assertionScript || '',
            isActive: true,
            module: { id: data.moduleId }
        }
        return await testCaseApi.create(payload)
    }

    return {
        currentCase,
        loading,
        executionResult,
        resetCase,
        loadCase,
        saveCase,
        saveCaseDirectly,
        addStep,
        removeStep,
        updateStep,
        runDryRun,
        executeCase
    }
})
