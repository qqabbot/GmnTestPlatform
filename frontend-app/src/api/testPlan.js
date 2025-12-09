import request from './request'

export const testPlanApi = {
    // Get all plans (optional project filter)
    getAll(params) {
        return request.get('/test-plans', { params })
    },

    // Get plan by ID
    getById(id) {
        return request.get(`/test-plans/${id}`)
    },

    // Create plan
    create(data) {
        return request.post('/test-plans', data)
    },

    // Update plan
    update(id, data) {
        return request.put(`/test-plans/${id}`, data)
    },

    // Delete plan
    delete(id) {
        return request.delete(`/test-plans/${id}`)
    },

    // Execute plan
    execute(id, envKey) {
        return request.post(`/test-plans/${id}/execute`, null, {
            params: { envKey }
        })
    }
}
