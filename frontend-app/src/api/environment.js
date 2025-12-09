import request from './request'

export const environmentApi = {
    // Get all environments
    getAll() {
        return request.get('/environments')
    },

    // Create environment
    create(data) {
        return request.post('/environments', data)
    },

    // Get environment by ID
    getById(id) {
        return request.get(`/environments/${id}`)
    },

    // Update environment
    update(id, data) {
        return request.put(`/environments/${id}`, data)
    },

    // Delete environment
    delete(id) {
        return request.delete(`/environments/${id}`)
    }
}
