import request from './request'

export const variableApi = {
    // Get variables by environment ID
    getByEnvironment(envId) {
        return request.get(`/variables/environment/${envId}`)
    },

    // Create variable
    create(data) {
        return request.post('/variables', data)
    },

    // Update variable
    update(id, data) {
        return request.put(`/variables/${id}`, data)
    },

    // Delete variable
    delete(id) {
        return request.delete(`/variables/${id}`)
    },

    // Bulk sync variables
    sync(envId, data) {
        return request.post(`/variables/environment/${envId}/sync`, data)
    }
}
