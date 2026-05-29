<template>
  <div class="auth-page">
    <el-card class="box">
      <h2>注册</h2>
      <el-tabs v-model="tab">
        <el-tab-pane label="手机号" name="phone">
          <el-form :model="form">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" />
            </el-form-item>
            <el-form-item label="验证码">
              <el-input v-model="form.verifyCode" style="width:60%">
                <el-button slot="append" :disabled="smsCooldown>0" @click="sendSms">
                  {{ smsCooldown>0 ? smsCooldown+'s' : '获取验证码' }}
                </el-button>
              </el-input>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <el-form :model="form" @submit.native.prevent="onSubmit">
        <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">注册</el-button>
      </el-form>
      <p class="link"><router-link to="/login">已有账号？去登录</router-link></p>
    </el-card>
  </div>
</template>

<script>
import http from '../../api/http'
import { mapMutations } from 'vuex'

export default {
  name: 'Register',
  data() {
    return {
      tab: 'phone',
      loading: false,
      smsCooldown: 0,
      form: {
        phone: '',
        email: null,
        nickname: '',
        password: '',
        verifyCode: '',
        inviteCode: sessionStorage.getItem('invite_code') || ''
      }
    }
  },
  methods: {
    ...mapMutations(['setToken', 'setQuota']),
    async sendSms() {
      await http.post('/api/v1/auth/send-sms', { phone: this.form.phone })
      this.$message.success('验证码已发送（开发环境请查看后端日志）')
      this.smsCooldown = 60
      const t = setInterval(() => {
        this.smsCooldown--
        if (this.smsCooldown <= 0) clearInterval(t)
      }, 1000)
    },
    async onSubmit() {
      this.loading = true
      try {
        const payload = { ...this.form, email: null }
        const data = await http.post('/api/v1/auth/register', payload)
        this.setToken(data.accessToken)
        this.setQuota(data.remainingQuota)
        this.$router.push('/chat')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.auth-page { display:flex; justify-content:center; padding-top: 40px; }
.box { width: 440px; }
.link { text-align: center; margin-top: 16px; }
</style>
