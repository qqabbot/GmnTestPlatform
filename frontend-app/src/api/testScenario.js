import request from './request'

export const testScenarioApi = {
    getAll(params) {
        return request({
            url: '/scenarios',
            method: 'get',
            params
        })
    },
    getById(id) {
        return request({
            url: `/scenarios/${id}`,
            method: 'get'
        })
    },
    create(data) {
        return request({
            url: '/scenarios',
            method: 'post',
            data
        })
    },
    update(id, data) {
        return request({
            url: `/scenarios/${id}`,
            method: 'put',
            data
        })
    },
    delete(id) {
        return request({
            url: `/scenarios/${id}`,
            method: 'delete'
        })
    },

    // Step Management
    getStepsTree(id) {
        return request({
            url: `/scenarios/${id}/steps/tree`,
            method: 'get'
        })
    },
    addStep(id, data) {
        return request({
            url: `/scenarios/${id}/steps`,
            method: 'post',
            data
        })
    },
    updateStep(id, stepId, data) {
        return request({
            url: `/scenarios/${id}/steps/${stepId}`,
            method: 'put',
            data
        })
    },
    deleteStep(id, stepId) {
        return request({
            url: `/scenarios/${id}/steps/${stepId}`,
            method: 'delete'
        })
    },

    syncSteps(id, steps) {
        return request({
            url: `/scenarios/${id}/steps/sync`,
            method: 'post',
            data: steps
        })
    },

    // Execution
    execute(id, envKey) {
        return request({
            url: `/scenarios/${id}/execute`,
            method: 'post',
            params: { envKey }
        })
    },

    // Execution History
    getExecutionHistory(id, limit = 10) {
        return request({
            url: `/scenarios/${id}/history`,
            method: 'get',
            params: { limit }
        })
    },

    getExecutionHistoryByEnv(id, envKey, limit = 10) {
        return request({
            url: `/scenarios/${id}/history/env/${envKey}`,
            method: 'get',
            params: { limit }
        })
    },

    getExecutionRecord(recordId) {
        return request({
            url: `/scenarios/executions/${recordId}`,
            method: 'get'
        })
    },

    getExecutionLogs(recordId) {
        return request({
            url: `/scenarios/executions/${recordId}/logs`,
            method: 'get'
        })
    },

    deleteExecutionRecord(recordId) {
        return request({
            url: `/scenarios/executions/${recordId}`,
            method: 'delete'
        })
    }
}
