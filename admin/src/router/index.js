import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import ImageManageView from '../views/ImageManageView.vue'
import AlbumManageView from '../views/AlbumManageView.vue'
import IpPortraitToolView from '../views/IpPortraitToolView.vue'
import SystemParamsView from '../views/SystemParamsView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true }
    },
    {
      path: '/',
      name: 'image-manage',
      component: ImageManageView,
      meta: { requiresAuth: true }
    },
    {
      path: '/albums',
      name: 'album-manage',
      component: AlbumManageView,
      meta: { requiresAuth: true }
    },
    {
      path: '/tools/ip-portrait',
      name: 'ip-portrait-tool',
      component: IpPortraitToolView,
      meta: { requiresAuth: true }
    },
    {
      path: '/settings/system-params',
      name: 'system-params',
      component: SystemParamsView,
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to) => {
  if (to.meta.public) {
    if (to.name === 'login' && sessionStorage.getItem('adminToken')) {
      return { path: '/' }
    }
    return true
  }
  if (to.meta.requiresAuth && !sessionStorage.getItem('adminToken')) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
