import { createRouter, createWebHistory } from 'vue-router';

const routes = [
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
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;