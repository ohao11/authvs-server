import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import ProfileView from '../views/ProfileView.vue'
import ConsentView from '../views/ConsentView.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView },
  { path: '/profile', component: ProfileView },
  { path: '/consent', component: ConsentView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
