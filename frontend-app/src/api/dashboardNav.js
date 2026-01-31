import request from './request'

export const dashboardNavApi = {
    getTree() {
        return request.get('/dashboard-nav/tree')
    },
    getEnvironments() {
        return request.get('/dashboard-nav/environments')
    },
    getEnvironmentById(id) {
        return request.get(`/dashboard-nav/environments/${id}`)
    },
    createEnvironment(data) {
        return request.post('/dashboard-nav/environments', data)
    },
    updateEnvironment(id, data) {
        return request.put(`/dashboard-nav/environments/${id}`, data)
    },
    deleteEnvironment(id) {
        return request.delete(`/dashboard-nav/environments/${id}`)
    },
    getAddresses(envId) {
        return request.get(`/dashboard-nav/environments/${envId}/addresses`)
    },
    createAddress(envId, data) {
        return request.post(`/dashboard-nav/environments/${envId}/addresses`, data)
    },
    updateAddress(id, data) {
        return request.put(`/dashboard-nav/addresses/${id}`, data)
    },
    deleteAddress(id) {
        return request.delete(`/dashboard-nav/addresses/${id}`)
    }
}
