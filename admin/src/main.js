import { createApp } from 'vue'
import './style.css'
import 'primeicons/primeicons.css'
import App from './App.vue'
import router from './router'
import PrimeVue from 'primevue/config'
import Aura from '@primeuix/themes/aura'

createApp(App)
  .use(router)
  .use(PrimeVue, {
    theme: {
      preset: Aura
    }
  })
  .mount('#app')
