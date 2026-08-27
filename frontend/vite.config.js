import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发时把 /api 代理到网关，避免跨域
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:7788',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
