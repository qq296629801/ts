<template>
  <el-card class="invite-card">
    <h4>邀请好友</h4>
    <p>邀请码：<strong>{{ info.inviteCode }}</strong></p>
    <el-input :value="info.inviteLink" readonly>
      <el-button slot="append" @click="copy">复制链接</el-button>
    </el-input>
    <p class="tip">好友注册双方均可获得奖励；好友首次充值您再得额外次数。</p>
  </el-card>
</template>

<script>
import http from '../../api/http'

export default {
  name: 'InviteCard',
  data() {
    return { info: { inviteCode: '', inviteLink: '' } }
  },
  async mounted() {
    this.info = await http.get('/api/v1/user/invite')
  },
  methods: {
    copy() {
      navigator.clipboard.writeText(this.info.inviteLink)
      this.$message.success('已复制')
    }
  }
}
</script>

<style scoped>
.invite-card { margin-top: 16px; }
.tip { font-size: 12px; color: #999; margin-top: 8px; }
</style>
