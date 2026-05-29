import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    token: localStorage.getItem('accessToken') || '',
    role: localStorage.getItem('userRole') || '',
    quota: 0
  },
  getters: {
    isLoggedIn: state => !!state.token
  },
  mutations: {
    setToken(state, token) {
      state.token = token
      localStorage.setItem('accessToken', token)
    },
    setRole(state, role) {
      state.role = role || ''
      if (role) localStorage.setItem('userRole', role)
      else localStorage.removeItem('userRole')
    },
    setQuota(state, quota) {
      state.quota = quota
    },
    logout(state) {
      state.token = ''
      state.role = ''
      state.quota = 0
      localStorage.removeItem('accessToken')
      localStorage.removeItem('userRole')
    }
  }
})
