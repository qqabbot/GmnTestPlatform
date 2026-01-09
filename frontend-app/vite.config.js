import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    server: {
        host: true,
        port: 8888,
        proxy: {
            '/api': {
                target: 'http://localhost:7777',
                changeOrigin: true
            },
            '/videos': {
                target: 'http://localhost:7777',
                changeOrigin: true
            },
            '/screenshots': {
                target: 'http://localhost:7777',
                changeOrigin: true
            }
        }
    }
})
