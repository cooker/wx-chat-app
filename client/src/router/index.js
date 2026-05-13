import { createRouter, createWebHistory } from 'vue-router'
import FeedView from '../views/FeedView.vue'
import AlbumDetailView from '../views/AlbumDetailView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'feed',
      component: FeedView
    },
    {
      path: '/albums/:id',
      name: 'album-detail',
      component: AlbumDetailView
    }
  ]
})

export default router
