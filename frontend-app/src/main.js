import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia()

// Register Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// Global error handler to catch and suppress navigation-related errors
app.config.errorHandler = (err, instance, info) => {
  // Suppress known navigation errors that occur during component unmount
  if (err && (
    err.message?.includes('parentNode') ||
    err.message?.includes('subTree') ||
    err.message?.includes('Illegal argument') ||
    err.name === 'TypeError' && err.message?.includes('null')
  )) {
    // These are typically harmless errors during component cleanup
    console.debug('Suppressed navigation error:', err.message)
    return
  }
  // Log other errors normally
  console.error('Vue error:', err, info)
}

// Global unhandled promise rejection handler
window.addEventListener('unhandledrejection', (event) => {
  // Suppress known navigation-related promise rejections
  const error = event.reason
  if (error && (
    error.message?.includes('parentNode') ||
    error.message?.includes('subTree') ||
    error.message?.includes('Illegal argument') ||
    error.name === 'TypeError' && error.message?.includes('null')
  )) {
    event.preventDefault()
    console.debug('Suppressed promise rejection:', error.message)
    return
  }
  // Log other rejections normally
  console.error('Unhandled promise rejection:', error)
})

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// Wait for router to be ready before mounting
router.isReady().then(() => {
  app.mount('#app')
}).catch(err => {
  console.error('Router initialization error:', err)
  app.mount('#app')
})
