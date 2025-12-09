import request from './request'

export const reportApi = {
    // Get all execution records
    getAll() {
        return request.get('/reports')
    },

    // Get records by project
    getByProject(projectId) {
        return request.get(`/reports/project/${projectId}`)
    },

    // Get records by module
    getByModule(moduleId) {
        return request.get(`/reports/module/${moduleId}`)
    }
}
