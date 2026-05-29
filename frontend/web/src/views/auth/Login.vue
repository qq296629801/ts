<template>
  <div class="auth-page">
    <el-card class="box">
      <h2>登录</h2>
      <el-form :model="form" @submit.native.prevent="onSubmit">
        <el-form-item label="账号（手机/邮箱）">
          <el-input v-model="form.account" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.rememberMe">记住我（30 天）</el-checkbox>
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">登录</el-button>
      </el-form>
      <p class="link"><router-link to="/register">没有账号？去注册</router-link></p>
    </el-card>
  </div>
</template>

<script>
import http from '../../api/http'
import { mapMutations } from 'vuex'

export default {
  name: 'Login',
  data() {
    return {
      loading: false,
      form: { account: '', password: '', rememberMe: false }
    }
  },
  methods: {
    ...mapMutations(['setToken', 'setQuota', 'setRole']),
    async onSubmit() {
      this.loading = true
      try {
        const data = await http.post('/api/v1/auth/login', this.form)
        this.setToken(data.accessToken)
        this.setQuota(data.remainingQuota)
        this.setRole(data.role || 'USER')
        const redirect = this.$route.query.redirect || '/chat'
        this.$router.push(redirect)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.auth-page { display:flex; justify-content:center; padding-top: 80px; }
.box { width: 400px; }
.link { text-align: center; margin-top: 16px; }
</style>
