import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../components/Layout.vue'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            component: Layout,
            redirect: '/dashboard',
            children: [
                // Dashboard
                {
                    path: '/dashboard',
                    name: 'Dashboard',
                    component: () => import('../views/TestReport.vue') // Reuse report view as dashboard
                },
                // API Testing Routes (changed from /api to /testing to avoid proxy conflict)
                {
                    path: '/testing/projects',
                    name: 'APIProjects',
                    component: () => import('../views/ProjectList.vue')
                },
                {
                    path: '/testing/modules',
                    name: 'APIModules',
                    component: () => import('../views/ModuleList.vue')
                },
                {
                    path: '/testing/plans',
                    name: 'TestPlanList',
                    component: () => import('../views/TestPlanList.vue')
                },
                {
                    path: '/testing/schedules',
                    name: 'ScheduleList',
                    component: () => import('../views/ScheduleList.vue')
                },
                {
                    path: '/import',
                    name: 'ImportView',
                    component: () => import('../views/ImportView.vue')
                },
                {
                    path: '/testing/cases',
                    name: 'APITestCases',
                    component: () => import('../views/TestCaseList.vue')
                },
                {
                    path: '/testing/cases/new',
                    name: 'NewAPITestCase',
                    component: () => import('../views/TestCaseEditor.vue')
                },
                {
                    path: '/testing/cases/:id/edit',
                    name: 'EditAPITestCase',
                    component: () => import('../views/TestCaseEditor.vue')
                },
                {
                    path: '/testing/execution',
                    name: 'APIExecution',
                    component: () => import('../views/TestExecution.vue')
                },
                {
                    path: '/testing/reports',
                    name: 'APIReports',
                    component: () => import('../views/TestReport.vue')
                },
                {
                    path: '/testing/guide',
                    name: 'ApiGuide',
                    component: () => import('../views/ApiGuide.vue')
                },
                // UI Testing Routes
                {
                    path: '/ui-testing/cases',
                    name: 'UiTestCases',
                    component: () => import('../views/UiTestCaseList.vue')
                },
                {
                    path: '/ui-testing/cases/new',
                    name: 'NewUiTestCase',
                    component: () => import('../views/UiTestCaseEditor.vue')
                },
                {
                    path: '/ui-testing/cases/:id/edit',
                    name: 'EditUiTestCase',
                    component: () => import('../views/UiTestCaseEditor.vue')
                },
                {
                    path: '/ui-testing/reports',
                    name: 'UiTestReports',
                    component: () => import('../views/UiTestReportList.vue')
                },
                {
                    path: '/ui-testing/reports/:id',
                    name: 'UiTestReportDetail',
                    component: () => import('../views/UiTestReportDetail.vue')
                },
                {
                    path: '/ui-testing/guide',
                    name: 'UserGuide',
                    component: () => import('../views/UserGuide.vue')
                },
                // Configuration Routes
                {
                    path: '/config/environments',
                    name: 'Environments',
                    component: () => import('../views/EnvironmentList.vue')
                },
                {
                    path: '/config/variables',
                    name: 'Variables',
                    component: () => import('../views/EnvironmentList.vue') // Placeholder
                },
                {
                    path: '/config/step-templates',
                    name: 'StepTemplates',
                    component: () => import('../views/StepTemplateList.vue')
                },
                // Legacy routes redirect (for backward compatibility)
                {
                    path: '/api/projects',
                    redirect: '/testing/projects'
                },
                {
                    path: '/api/modules',
                    redirect: '/testing/modules'
                },
                {
                    path: '/api/cases',
                    redirect: '/testing/cases'
                },
                {
                    path: '/api/cases/new',
                    redirect: '/testing/cases/new'
                },
                {
                    path: '/api/cases/:id/edit',
                    redirect: to => `/testing/cases/${to.params.id}/edit`
                },
                {
                    path: '/api/execution',
                    redirect: '/testing/execution'
                },
                {
                    path: '/api/reports',
                    redirect: '/testing/reports'
                },
                {
                    path: '/environments',
                    redirect: '/config/environments'
                },
                {
                    path: '/projects',
                    redirect: '/testing/projects'
                },
                {
                    path: '/modules',
                    redirect: '/testing/modules'
                },
                {
                    path: '/cases',
                    redirect: '/testing/cases'
                },
                {
                    path: '/cases/new',
                    redirect: '/testing/cases/new'
                },
                {
                    path: '/cases/:id/edit',
                    redirect: to => `/testing/cases/${to.params.id}/edit`
                },
                {
                    path: '/execution',
                    redirect: '/testing/execution'
                },
                {
                    path: '/reports',
                    redirect: '/testing/reports'
                }
            ]
        }
    ]
})

export default router
