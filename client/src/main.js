import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { loadPublicConfig } from './api/images'

async function bootstrap() {
  await loadPublicConfig().catch(() => {})
  createApp(App).use(router).mount('#app')
}

bootstrap()
