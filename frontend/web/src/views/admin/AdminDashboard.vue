<template>
  <el-card>
    <h3>数据概览</h3>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="总用户">{{ stats.totalUsers }}</el-descriptions-item>
      <el-descriptions-item label="今日新增">{{ stats.todayNewUsers }}</el-descriptions-item>
      <el-descriptions-item label="总生图">{{ stats.totalImages }}</el-descriptions-item>
      <el-descriptions-item label="今日生图">{{ stats.todayImages }}</el-descriptions-item>
      <el-descriptions-item label="已支付订单">{{ stats.paidOrderCount }}</el-descriptions-item>
      <el-descriptions-item label="今日收入">¥{{ stats.todayRevenue }}</el-descriptions-item>
      <el-descriptions-item label="已上架模版">{{ stats.approvedTemplates }}</el-descriptions-item>
      <el-descriptions-item label="待审核模版">{{ stats.pendingTemplates }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script>
import http from '../../api/http'

export default {
  name: 'AdminDashboard',
  data() {
    return { stats: {} }
  },
  async mounted() {
    this.stats = await http.get('/api/v1/admin/reports/summary')
  }
}
</script>
