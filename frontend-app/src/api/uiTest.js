import request from './request';

export const uiTestApi = {
    // Case CRUD
    saveCase(uiCase) {
        return request.post('/ui-tests/cases', uiCase);
    },
    getAllCases() {
        return request.get('/ui-tests/cases');
    },
    getCase(id) {
        return request.get(`/ui-tests/cases/${id}`);
    },
    getCasesByModule(moduleId) {
        return request.get(`/ui-tests/modules/${moduleId}/cases`);
    },
    getCasesByProject(projectId) {
        return request.get(`/ui-tests/projects/${projectId}/cases`);
    },
    deleteCase(id) {
        return request.delete(`/ui-tests/cases/${id}`);
    },

    // Step CRUD
    saveStep(step) {
        return request.post('/ui-tests/steps', step);
    },
    getSteps(caseId) {
        return request.get(`/ui-tests/cases/${caseId}/steps`);
    },
    deleteStep(id) {
        return request.delete(`/ui-tests/steps/${id}`);
    },

    // Execution
    executeCase(id, mode = 'server') {
        return request.post(`/ui-tests/cases/${id}/execute?mode=${mode}`);
    },
    getLocalScript(id) {
        return request.get(`/ui-tests/cases/${id}/local-script`);
    },
    checkPlaywright() {
        return request.get('/ui-tests/check-playwright');
    },
    installPlaywright() {
        return request.post('/ui-tests/install-playwright');
    },

    // Recording
    startRecording(caseId, options) {
        return request.post(`/ui-tests/cases/${caseId}/start-recording`, options);
    },
    stopRecording(caseId) {
        return request.post(`/ui-tests/cases/${caseId}/stop-recording`);
    },
    getRecordingCode(caseId) {
        return request.get(`/ui-tests/cases/${caseId}/recording-code`);
    },
    getRecordingStatus(caseId) {
        return request.get(`/ui-tests/cases/${caseId}/recording-status`);
    },

    // Reports
    getRecordsByCase(caseId) {
        return request.get(`/ui-tests/cases/${caseId}/records`);
    },
    getRecordsByProject(projectId) {
        return request.get(`/ui-tests/projects/${projectId}/records`);
    },
    getAllRecords() {
        return request.get('/ui-tests/records');
    },
    getRecord(id) {
        return request.get(`/ui-tests/records/${id}`);
    },
    getLogsByRecord(recordId) {
        return request.get(`/ui-tests/records/${recordId}/logs`);
    }
};
