import request from './request'

export const scheduleApi = {
    getAll() {
        return request.get('/schedules')
    },
    create(data) {
        return request.post('/schedules', data)
    },
    update(id, data) {
        return request.put(`/schedules/${id}`, data)
    },
    delete(id) {
        return request.delete(`/schedules/${id}`)
    },
    pause(id) {
        return request.post(`/schedules/${id}/pause`)
    },
    resume(id) {
        return request.post(`/schedules/${id}/resume`)
    }
}
