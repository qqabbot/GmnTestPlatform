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
    },

    importCurl(projectId, moduleId, curlCommand, asStep = false, caseId = null, parseOnly = false) {
        const params = { projectId, asStep, parseOnly }
        if (moduleId) params.moduleId = moduleId
        if (caseId) params.caseId = caseId
        return request.post('/import/curl', curlCommand, {
            params,
            headers: {
                'Content-Type': 'text/plain'
            }
        })
    }
}
