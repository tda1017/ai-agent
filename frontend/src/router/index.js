import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/auth/login',
    name: 'login',
    component: () => import('../views/auth/LoginView.vue'),
    meta: { public: true, hideNav: true }
  },
  {
    path: '/auth/register',
    name: 'register',
    component: () => import('../views/auth/RegisterView.vue'),
    meta: { public: true, hideNav: true }
  },
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomeView.vue')
  },
  {
    path: '/apps',
    name: 'app-chat',
    component: () => import('../views/AppChatView.vue')
  },
  {
    path: '/manus',
    name: 'manus-chat',
    component: () => import('../views/ManusChatView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const isPublic = to.meta.public
  const token = localStorage.getItem('token')
  
  if (!isPublic && !token) {
    return next({
      path: '/auth/login',
      query: { redirect: to.fullPath }
    })
  }
  
  if (isPublic && token && to.path.startsWith('/auth')) {
    return next('/')
  }
  
  next()
})

export default router