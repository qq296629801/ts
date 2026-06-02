<template>
  <el-card>
    <h3>系统账单汇总</h3>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="统计区间">{{ summary.from }} ~ {{ summary.to }}</el-descriptions-item>
      <el-descriptions-item label="成功笔数">{{ summary.paidCount }}</el-descriptions-item>
      <el-descriptions-item label="总金额">¥{{ summary.totalAmount }}</el-descriptions-item>
    </el-descriptions>
    <h4 style="margin-top:16px">按渠道</h4>
    <el-table :data="summary.byPayType || []">
      <el-table-column prop="payType" label="渠道" />
      <el-table-column prop="count" label="笔数" />
      <el-table-column prop="amount" label="金额" />
    </el-table>
  </el-card>
</template>

<script>
import http from '../../api/http'

export default {
  name: 'BillingSummary',
  data() {
    return {
      summary: { from: '', to: '', paidCount: 0, totalAmount: 0, byPayType: [] }
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      this.summary = await http.get('/api/v1/admin/billing/summary')
    }
  }
}
</script>
