<template>
  <div class="recharge">
    <el-card>
      <h3>充值中心</h3>
      <el-row :gutter="16">
        <el-col :span="6" v-for="pkg in packages" :key="pkg.id">
          <el-card shadow="hover" class="pkg" @click.native="selectPackage(pkg)">
            <div class="name">{{ pkg.name }}</div>
            <div class="quota">{{ pkg.quota }} 次</div>
            <div class="price">¥{{ pkg.price }}</div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-card v-if="order" class="pay-box">
      <h4>订单 {{ order.orderNo }}</h4>
      <p>状态：{{ statusText(order.status) }}</p>
      <p v-if="order.status === 'PENDING'">请在 15 分钟内完成支付</p>
      <p class="mock-url" v-if="order.status === 'PENDING'">{{ order.codeUrl }}</p>
      <el-button v-if="order.status === 'PENDING'" type="warning" @click="simulatePay">开发环境：模拟支付成功</el-button>
    </el-card>
  </div>
</template>

<script>
import http from '../../api/http'

export default {
  name: 'Recharge',
  data() {
    return {
      packages: [],
      order: null,
      pollTimer: null
    }
  },
  async mounted() {
    this.packages = await http.get('/api/v1/pay/packages')
  },
  beforeDestroy() {
    if (this.pollTimer) clearInterval(this.pollTimer)
  },
  methods: {
    async selectPackage(pkg) {
      this.order = await http.post('/api/v1/pay/create-order', { packageId: pkg.id })
      this.startPoll()
    },
    startPoll() {
      if (this.pollTimer) clearInterval(this.pollTimer)
      this.pollTimer = setInterval(this.refreshOrder, 3000)
    },
    async refreshOrder() {
      if (!this.order) return
      const data = await http.get(`/api/v1/pay/order/${this.order.orderNo}`)
      this.order = { ...this.order, ...data }
      if (data.status === 'PAID') {
        clearInterval(this.pollTimer)
        this.$message.success('支付成功，次数已到账')
      }
      if (data.status === 'CANCELLED') {
        clearInterval(this.pollTimer)
        this.$message.warning('订单已取消或超时')
      }
    },
    async simulatePay() {
      await http.post(`/api/v1/pay/dev/simulate/${this.order.orderNo}`)
      await this.refreshOrder()
    },
    statusText(s) {
      const map = { PENDING: '待支付', PAID: '已支付', CANCELLED: '已取消' }
      return map[s] || s
    }
  }
}
</script>

<style scoped>
.recharge { max-width: 960px; margin: 0 auto; }
.pkg { cursor: pointer; text-align: center; margin-bottom: 12px; }
.name { font-weight: bold; }
.quota { color: #409eff; margin: 8px 0; }
.price { font-size: 18px; color: #f56c6c; }
.pay-box { margin-top: 16px; }
.mock-url { word-break: break-all; font-size: 12px; color: #666; }
</style>
