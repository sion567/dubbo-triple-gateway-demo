import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/orders',
    children: [
      { path: 'users', component: () => import('../views/Users.vue'), meta: { admin: true } },
      { path: 'orders', component: () => import('../views/Orders.vue') },
      { path: 'storage', component: () => import('../views/Storage.vue'), meta: { admin: true } },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

// 路由守卫：未登录跳登录页；非管理员访问管理页拦下
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.isLoggedIn) return '/login'
  if (to.meta?.admin && !auth.isAdmin) return '/orders'
})

export default router
