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

    // Delete variable
    delete(id) {
        return request.delete(`/variables/${id}`)
    }
}
