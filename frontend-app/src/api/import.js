import request from './request'

export const importApi = {
    importSwagger(projectId, url, content) {
        return request.post('/import/swagger', content, {
            params: { projectId, url }
        })
    },

    importYapi(projectId, content) {
        return request.post('/import/yapi', content, {
            params: { projectId }
        })
    }
}
