<template>
  <div class="admin">
    <el-menu mode="horizontal" :default-active="$route.path" router>
      <el-menu-item index="/admin/audit">模版审核</el-menu-item>
      <el-menu-item index="/admin/users">用户管理</el-menu-item>
      <el-menu-item index="/admin/config">系统配置</el-menu-item>
      <el-menu-item index="/admin/dashboard">数据概览</el-menu-item>
      <el-menu-item index="/admin/billing/orders">充值账单</el-menu-item>
      <el-menu-item index="/admin/billing/summary">系统汇总</el-menu-item>
    </el-menu>
    <router-view />
  </div>
</template>

<script>
export default { name: 'AdminLayout' }
</script>

<style scoped>
.admin { max-width: 1200px; margin: 0 auto; padding-top: 12px; }
</style>
