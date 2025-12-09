import request from './request'

export const stepTemplateApi = {
    getAll(params) {
        return request({
            url: '/step-templates',
            method: 'get',
            params
        })
    },
    create(data) {
        return request({
            url: '/step-templates',
            method: 'post',
            data
        })
    },
    update(id, data) {
        return request({
            url: `/step-templates/${id}`,
            method: 'put',
            data
        })
    },
    delete(id) {
        return request({
            url: `/step-templates/${id}`,
            method: 'delete'
        })
    }
}
