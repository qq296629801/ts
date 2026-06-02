<template>
  <div id="app">
    <div class="app-page-bg" aria-hidden="true" />
    <el-container class="app-shell">
      <el-header class="app-header--glass">
        <span class="logo">AI 图像生成平台</span>
        <nav class="nav">
          <template v-if="isLoggedIn">
            <router-link to="/chat">聊天</router-link>
            <router-link to="/templates">模版广场</router-link>
            <router-link to="/gallery/public">作品展示</router-link>
            <router-link to="/my/gallery">我的图库</router-link>
            <router-link to="/recharge">充值</router-link>
            <router-link to="/invite">邀请</router-link>
            <router-link v-if="isAdmin" to="/admin">管理</router-link>
          </template>
          <template v-else>
            <router-link to="/templates">模版广场</router-link>
            <router-link to="/gallery/public">作品展示</router-link>
            <router-link to="/login">登录</router-link>
            <router-link to="/register">注册</router-link>
          </template>
        </nav>
        <quota-badge v-if="isLoggedIn" class="header-quota" />
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
/* UI token：specs/001-ai-image-platform/design-ui-tokens.md */
#app {
  font-family: 'Helvetica Neue', Arial, sans-serif;
  position: relative;
  min-height: 100vh;
}

.app-page-bg {
  --page-bg-color: #0f0f1a;
  --page-bg-overlay-top: rgba(15, 15, 26, 0.45);
  --page-bg-overlay-bottom: rgba(26, 26, 46, 0.88);

  position: fixed;
  inset: 0;
  z-index: 0;
  background-color: var(--page-bg-color);
  background-image: url('~@/assets/images/app-bg.webp');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  pointer-events: none;
}

.app-page-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    var(--page-bg-overlay-top) 0%,
    rgba(20, 20, 36, 0.65) 45%,
    var(--page-bg-overlay-bottom) 100%
  );
}

.app-shell.el-container {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  background: transparent;
}

.app-shell .el-main {
  background: transparent !important;
}

.app-header--glass.el-header {
  --header-bg-glass: rgba(26, 26, 46, 0.72);
  --header-bg-fallback: rgba(26, 26, 46, 0.92);
  --header-blur: 14px;
  --nav-text: rgba(255, 255, 255, 0.75);
  --nav-text-active: #ffffff;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 20px;
  position: sticky;
  top: 0;
  z-index: 1000;
  color: #fff;
  background: var(--header-bg-glass);
  backdrop-filter: blur(var(--header-blur)) saturate(140%);
  -webkit-backdrop-filter: blur(var(--header-blur)) saturate(140%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .app-header--glass.el-header {
    background: var(--header-bg-fallback);
  }
}

.logo {
  flex-shrink: 0;
  font-weight: bold;
  font-size: 18px;
}

.nav {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  justify-content: center;
}

.nav a {
  color: var(--nav-text);
  margin-right: 16px;
  text-decoration: none;
  white-space: nowrap;
  transition: color 0.2s ease;
}

.nav a:last-child {
  margin-right: 0;
}

.nav a:hover {
  color: var(--nav-text-active);
}

.nav a.router-link-active {
  color: var(--nav-text-active);
  font-weight: 600;
  border-bottom: 2px solid rgba(255, 255, 255, 0.9);
  padding-bottom: 2px;
}

.header-quota {
  flex-shrink: 0;
}

/* 业务卡片在深色背景上保持可读 */
.app-shell .el-card {
  background: rgba(255, 255, 255, 0.96);
}

@media (prefers-reduced-motion: reduce) {
  .nav a {
    transition: none;
  }
}

@media (max-width: 768px) {
  .app-page-bg {
    background-attachment: scroll;
  }

  .app-header--glass.el-header {
    flex-wrap: wrap;
    height: auto !important;
    min-height: 60px;
    padding: 8px 12px;
  }

  .nav {
    order: 3;
    flex: 1 1 100%;
    justify-content: flex-start;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    padding-bottom: 2px;
  }

  .nav::-webkit-scrollbar {
    display: none;
  }

  .header-quota {
    margin-left: auto;
  }
}
</style>
