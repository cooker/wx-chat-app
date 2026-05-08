import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  base: '/client/',
  build: {
    outDir: '../backend/src/main/resources/static/client',
    emptyOutDir: true
  }
})
