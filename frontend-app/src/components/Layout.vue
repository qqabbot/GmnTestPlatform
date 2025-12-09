<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <h2>GMN Test Platform</h2>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#fff"
        active-text-color="#409EFF"
      >
        <!-- Dashboard -->
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>Dashboard</span>
        </el-menu-item>

        <!-- API Testing -->
        <el-sub-menu index="testing">
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>API Testing</span>
          </template>
          <el-menu-item index="/testing/plans">
            <el-icon><List /></el-icon>
            <span>Test Plans</span>
          </el-menu-item>
          <el-menu-item index="/testing/schedules">
            <el-icon><Timer /></el-icon>
            <span>Schedules</span>
          </el-menu-item>
          <el-menu-item index="/testing/projects">
            <el-icon><Folder /></el-icon>
            <span>Projects</span>
          </el-menu-item>
          <el-menu-item index="/testing/modules">
            <el-icon><Grid /></el-icon>
            <span>Modules</span>
          </el-menu-item>
          <el-menu-item index="/testing/cases">
            <el-icon><Document /></el-icon>
            <span>Test Cases</span>
          </el-menu-item>
          <el-menu-item index="/testing/execution">
            <el-icon><VideoPlay /></el-icon>
            <span>Execution</span>
          </el-menu-item>
          <el-menu-item index="/testing/reports">
            <el-icon><Tickets /></el-icon>
            <span>Reports</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- UI Testing (Placeholder for future) -->
        <el-sub-menu index="ui" disabled>
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>UI Testing</span>
          </template>
          <el-menu-item index="/ui/projects">Projects</el-menu-item>
          <el-menu-item index="/ui/cases">Test Cases</el-menu-item>
          <el-menu-item index="/ui/execution">Execution</el-menu-item>
        </el-sub-menu>

         <!-- App Testing (Placeholder for future) -->
        <el-sub-menu index="app" disabled>
          <template #title>
            <el-icon><Iphone /></el-icon>
            <span>App Testing</span>
          </template>
          <el-menu-item index="/app/projects">Projects</el-menu-item>
          <el-menu-item index="/app/cases">Test Cases</el-menu-item>
          <el-menu-item index="/app/execution">Execution</el-menu-item>
        </el-sub-menu>

        <!-- Performance Testing (Placeholder for future) -->
        <el-sub-menu index="performance" disabled>
          <template #title>
            <el-icon><Odometer /></el-icon>
            <span>Performance</span>
          </template>
          <el-menu-item index="/performance/scenarios">Scenarios</el-menu-item>
          <el-menu-item index="/performance/execution">Execution</el-menu-item>
          <el-menu-item index="/performance/reports">Reports</el-menu-item>
        </el-sub-menu>

        <!-- Divider -->
        <el-divider style="margin: 10px 0; border-color: #26334d;" />

        <!-- Configuration -->
        <el-sub-menu index="config">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>Configuration</span>
          </template>
          <el-menu-item index="/config/environments">
            <el-icon><Connection /></el-icon>
            <span>Environments</span>
          </el-menu-item>
          <el-menu-item index="/config/variables">
            <el-icon><Coin /></el-icon>
            <span>Variables</span>
          </el-menu-item>
          <el-menu-item index="/config/step-templates">
            <el-icon><Collection /></el-icon>
            <span>Step Library</span>
          </el-menu-item>
          <el-menu-item index="/import">
             <el-icon><Upload /></el-icon>
             <span>API Import</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-content">
          <span class="title">{{ pageTitle }}</span>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const pageTitle = computed(() => {
  const titles = {
    '/dashboard': 'Dashboard',
    '/testing/projects': 'API Testing - Projects',
    '/testing/modules': 'API Testing - Modules',
    '/testing/cases': 'API Testing - Test Cases',
    '/testing/execution': 'API Testing - Execution',
    '/testing/reports': 'API Testing - Reports',
    '/config/environments': 'Configuration - Environments',
    '/config/variables': 'Configuration - Variables'
  }
  
  // Handle dynamic routes (e.g., /testing/cases/new, /testing/cases/:id/edit)
  if (route.path.startsWith('/testing/cases/')) {
    return 'API Testing - Test Case Editor'
  }
  
  return titles[route.path] || 'GMN Test Platform'
})
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.sidebar {
  background-color: #304156;
  overflow-x: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  border-bottom: 1px solid #26334d;
}

.logo h2 {
  font-size: 16px;
  font-weight: 600;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
}
</style>
