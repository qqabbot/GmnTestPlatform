import request from './request'

export const testCaseApi = {
    // Get all test cases
    getAll(moduleId) {
        return request.get('/cases', {
            params: { moduleId }
        })
    },

    // Get test cases by project with pagination
    getByProject(projectId, page = 0, size = 20, keyword = '') {
        return request.get('/cases/project', {
            params: { projectId, page, size, keyword }
        })
    },

    // Get test case by ID
    getById(id) {
        return request.get(`/cases/${id}`)
    },

    // Dry run test case
    dryRun(id, data) {
        return request.post(`/cases/${id}/dry-run`, data)
    },

    // Create test case
    create(data) {
        return request.post('/cases', data)
    },

    // Update test case
    update(id, data) {
        return request.put(`/cases/${id}`, data)
    },

    // Delete test case
    delete(id) {
        return request.delete(`/cases/${id}`)
    },

    // Execute test cases
    execute(params) {
        return request.post('/cases/execute', null, { params })
    }
}
