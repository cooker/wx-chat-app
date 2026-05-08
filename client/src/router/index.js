import { createRouter, createWebHistory } from 'vue-router'
import FeedView from '../views/FeedView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'feed',
      component: FeedView
    }
  ]
})

export default router
