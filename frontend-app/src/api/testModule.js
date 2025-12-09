import request from './request'

export const testModuleApi = {
    // Get all modules
    getAll() {
        return request.get('/modules')
    },

    // Create module
    create(data) {
        return request.post('/modules', data)
    },

    // Get module by ID
    getById(id) {
        return request.get(`/modules/${id}`)
    },

    // Delete module
    delete(id) {
        return request.delete(`/modules/${id}`)
    }
}
