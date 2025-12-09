import request from './request'

export const projectApi = {
    // Get all projects
    getAll() {
        return request.get('/projects')
    },

    // Create project
    create(data) {
        return request.post('/projects', data)
    },

    // Get project by ID
    getById(id) {
        return request.get(`/projects/${id}`)
    },

    // Update project
    update(id, data) {
        return request.put(`/projects/${id}`, data)
    },

    // Delete project
    delete(id) {
        return request.delete(`/projects/${id}`)
    }
}
