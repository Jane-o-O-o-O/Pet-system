import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '../store/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'owner/pets',
        name: 'MyPets',
        component: () => import('../views/owner/PetList.vue'),
        meta: { roles: ['OWNER'] }
      },
      {
        path: 'owner/orders',
        name: 'MyOrders',
        component: () => import('../views/owner/OrderList.vue'),
        meta: { roles: ['OWNER'] }
      },
      {
        path: 'staff/pets',
        name: 'StaffPets',
        component: () => import('../views/staff/PetManagement.vue'),
        meta: { roles: ['STAFF', 'ADMIN'] }
      },
      {
        path: 'staff/orders',
        name: 'StaffOrders',
        component: () => import('../views/staff/OrderManagement.vue'),
        meta: { roles: ['STAFF', 'ADMIN'] }
      },
      {
        path: 'admin/users',
        name: 'UserManagement',
        component: () => import('../views/admin/UserManagement.vue'),
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/stats',
        name: 'Stats',
        component: () => import('../views/admin/Stats.vue'),
        meta: { roles: ['ADMIN'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  if (to.path === '/login') {
    next()
  } else if (!userStore.token) {
    next('/login')
  } else {
    if (to.meta.roles && !(to.meta.roles as string[]).includes(userStore.role)) {
      next('/dashboard')
    } else {
      next()
    }
  }
})

export default router
