import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '../store'

Vue.use(VueRouter)

const routes = [
  { path: '/login', component: () => import('../views/auth/Login.vue') },
  { path: '/register', component: () => import('../views/auth/Register.vue') },
  { path: '/chat', component: () => import('../views/chat/ChatRoom.vue'), meta: { auth: true } },
  { path: '/gallery', component: () => import('../views/user/ImageGallery.vue'), meta: { auth: true } },
  { path: '/recharge', component: () => import('../views/pay/Recharge.vue'), meta: { auth: true } },
  { path: '/invite', component: () => import('../views/user/InviteCard.vue'), meta: { auth: true } },
  { path: '/templates', component: () => import('../views/template/TemplatePlaza.vue') },
  { path: '/template/publish', component: () => import('../views/template/PublishTemplate.vue'), meta: { auth: true } },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { auth: true, admin: true },
    children: [
      { path: '', redirect: 'audit' },
      { path: 'audit', component: () => import('../views/admin/TemplateAudit.vue') },
      { path: 'users', component: () => import('../views/admin/UserManage.vue') },
      { path: 'config', component: () => import('../views/admin/SystemConfig.vue') },
      { path: 'dashboard', component: () => import('../views/admin/AdminDashboard.vue') }
    ]
  },
  { path: '/', redirect: '/chat' }
]

const router = new VueRouter({ mode: 'history', routes })

router.beforeEach((to, from, next) => {
  const invite = to.query.invite_code
  if (invite) {
    sessionStorage.setItem('invite_code', invite)
  }
  if (to.meta.auth && !store.getters.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (to.matched.some(r => r.meta.admin) && store.state.role !== 'ADMIN') {
    next('/chat')
    return
  }
  next()
})

export default router
