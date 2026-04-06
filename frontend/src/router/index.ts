import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: () => import('@/views/LandingPage.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginPage.vue')
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/ChatPage.vue'),
      meta: { requiresAuth: true, layout: 'main' }
    },
    {
      path: '/chat/:id',
      name: 'chat-session',
      component: () => import('@/views/ChatPage.vue'),
      meta: { requiresAuth: true, layout: 'main' }
    },
    {
      path: '/knowledge',
      name: 'knowledge',
      component: () => import('@/views/KnowledgeBasePage.vue'),
      meta: { requiresAuth: true, layout: 'main' }
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/KnowledgeBasePage.vue'), // Placeholder - adjust as needed
      meta: { requiresAuth: true, layout: 'main' }
    }
  ],
  scrollBehavior(to, _from, savedPosition) {
    if (to.hash) {
      return {
        el: to.hash,
        behavior: 'smooth'
      }
    }
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

// 路由守卫
router.beforeEach((to, _from) => {
  const userStore = useUserStore()
  
  // 需要登录的页面
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  
  // 已登录用户访问登录页，跳转到聊天页
  if (to.name === 'login' && userStore.isLoggedIn) {
    return { name: 'chat' }
  }
  
  return true
})

export default router
