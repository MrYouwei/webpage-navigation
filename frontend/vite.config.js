import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  // GitHub Pages 项目页地址是 https://<用户名>.github.io/<仓库名>/
  // 必须把 base 设为 /<仓库名>/，否则 CSS/JS 路径是 /assets/... 会 404
  // 如果你之后改用自定义域名，把下面改回 '/' 即可
  // base: '/webpage-navigation/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 8381,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 60000,
        proxyTimeout: 60000
      }
    }
  },
  preview: {
    port: 8381,
    host: true
  }
})
