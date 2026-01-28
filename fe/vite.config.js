import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: false
      },
      '/oauth2': {
        target: 'http://localhost:9000',
        changeOrigin: false
      },
      '/.well-known': {
        target: 'http://localhost:9000',
        changeOrigin: false
      },
      '/userinfo': {
        target: 'http://localhost:9000',
        changeOrigin: false
      }
    }
  }
})
