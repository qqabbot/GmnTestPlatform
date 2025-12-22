import request from './request'

export const aiApi = {
    generateCases(data) {
        return request({
            url: '/ai/generate-cases',
            method: 'post',
            data
        })
    },
    diagnose(data) {
        return request({
            url: '/ai/diagnose',
            method: 'post',
            data
        })
    },
    suggestAssertions(data) {
        return request({
            url: '/ai/suggest-assertions',
            method: 'post',
            data
        })
    },
    generateMock(data) {
        return request({
            url: '/ai/generate-mock',
            method: 'post',
            data
        })
    }
}
