<template>
  <el-card>
    <h3>充值账单</h3>
    <div class="filters">
      <el-input v-model.number="userId" placeholder="用户 ID" clearable style="width:120px" />
      <el-select v-model="status" placeholder="状态" clearable>
        <el-option label="待支付" value="PENDING" />
        <el-option label="已支付" value="PAID" />
        <el-option label="已退款" value="REFUNDED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-select v-model="payType" placeholder="渠道" clearable>
        <el-option label="微信" value="WECHAT" />
        <el-option label="支付宝" value="ALIPAY" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table :data="items" style="margin-top:12px">
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column prop="userId" label="用户" width="80" />
      <el-table-column prop="payType" label="渠道" width="90" />
      <el-table-column prop="amount" label="金额" width="80" />
      <el-table-column prop="quotaGranted" label="次数" width="70" />
      <el-table-column prop="status" label="状态" width="90" />
      <el-table-column prop="paidAt" label="支付时间" min-width="160" />
      <el-table-column prop="refundedAt" label="退款时间" min-width="160" />
      <el-table-column label="操作" width="100" fixed="right">
        <template slot-scope="{ row }">
          <el-button
            v-if="row.status === 'PAID'"
            type="text"
            size="small"
            @click="refund(row)"
          >退款</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="total > size"
      layout="prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page.sync="page"
      @current-change="load"
    />
  </el-card>
</template>

<script>
import http from '../../api/http'

export default {
  name: 'BillingOrders',
  data() {
    return {
      items: [],
      userId: null,
      status: '',
      payType: '',
      page: 1,
      size: 20,
      total: 0
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      const data = await http.get('/api/v1/admin/billing/orders', {
        params: {
          userId: this.userId || undefined,
          status: this.status || undefined,
          payType: this.payType || undefined,
          page: this.page,
          size: this.size
        }
      })
      this.items = data.items
      this.total = data.total
    },
    refund(row) {
      this.$confirm(
        `确认对订单 ${row.orderNo} 原路退款并扣回 ${row.quotaGranted} 次？余额不足将拒绝。`,
        '退款确认',
        { type: 'warning' }
      ).then(async () => {
        await http.post(`/api/v1/admin/billing/orders/${row.orderNo}/refund`)
        this.$message.success('退款成功')
        this.load()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.filters { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
</style>
