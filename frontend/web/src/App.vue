<template>
  <div id="app">
    <el-container>
      <el-header>
        <span class="logo">AI 图像生成平台</span>
        <nav v-if="isLoggedIn" class="nav">
          <router-link to="/chat">聊天</router-link>
          <router-link to="/templates">模版广场</router-link>
          <router-link to="/gallery">图库</router-link>
          <router-link to="/recharge">充值</router-link>
          <router-link to="/invite">邀请</router-link>
          <router-link v-if="isAdmin" to="/admin">管理</router-link>
        </nav>
        <quota-badge v-if="isLoggedIn" />
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import QuotaBadge from './components/QuotaBadge.vue'
import { mapGetters } from 'vuex'

export default {
  name: 'App',
  components: { QuotaBadge },
  computed: {
    ...mapGetters(['isLoggedIn']),
    isAdmin() {
      return this.$store.state.role === 'ADMIN'
    }
  }
}
</script>

<style>
#app { font-family: 'Helvetica Neue', Arial, sans-serif; }
.el-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1a1a2e;
  color: #fff;
}
.logo { font-weight: bold; font-size: 18px; }
.nav a { color: #ccc; margin-right: 16px; text-decoration: none; }
.nav a.router-link-active { color: #fff; }
</style>
