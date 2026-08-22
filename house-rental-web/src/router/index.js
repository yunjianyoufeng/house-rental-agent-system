import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import HouseListView from '../views/HouseListView.vue'
import HouseDetailView from '../views/HouseDetailView.vue'
import AdminHomeView from '../views/AdminHomeView.vue'
import LandlordHomeView from '../views/LandlordHomeView.vue'
import TenantHomeView from '../views/TenantHomeView.vue'
import AgentAssistantView from '../views/AgentAssistantView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      guestOnly: true,
    },
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
    meta: {
      guestOnly: true,
    },
  },
  {
    path: '/houses',
    name: 'houses',
    component: HouseListView,
  },
  {
    path: '/house/:id',
    name: 'houseDetail',
    component: HouseDetailView,
  },
  {
    path: '/admin',
    name: 'admin',
    component: AdminHomeView,
    meta: {
      requiresAuth: true,
      role: 'ADMIN',
    },
  },
  {
    path: '/landlord',
    name: 'landlord',
    component: LandlordHomeView,
    meta: {
      requiresAuth: true,
      role: 'LANDLORD',
    },
  },
  {
    path: '/tenant',
    name: 'tenant',
    component: TenantHomeView,
    meta: {
      requiresAuth: true,
      role: 'TENANT',
    },
  },
  {
    path: '/tenant/agent',
    name: 'tenantAgent',
    component: AgentAssistantView,
    meta: {
      requiresAuth: true,
      role: 'TENANT',
    },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

function getHomePathByRole(roleCode) {
  if (roleCode === 'ADMIN') return '/admin'
  if (roleCode === 'LANDLORD') return '/landlord'
  if (roleCode === 'TENANT') return '/tenant'
  return '/'
}

router.beforeEach((to) => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  const requiresAuth = to.meta?.requiresAuth
  const guestOnly = to.meta?.guestOnly
  const requiredRole = to.meta?.role

  if (requiresAuth && !userInfo) {
    return '/login'
  }

  if (guestOnly && userInfo) {
    return getHomePathByRole(userInfo.roleCode)
  }

  if (requiresAuth && requiredRole && userInfo?.roleCode !== requiredRole) {
    return getHomePathByRole(userInfo.roleCode)
  }

  return true
})

export default router
