<template>
  <span class="quota">剩余 {{ quota }} 次</span>
</template>

<script>
import http from '../api/http'
import { mapState, mapMutations } from 'vuex'

export default {
  name: 'QuotaBadge',
  computed: mapState(['quota']),
  mounted() {
    this.refresh()
  },
  methods: {
    ...mapMutations(['setQuota']),
    async refresh() {
      try {
        const data = await http.get('/api/v1/user/quota')
        this.setQuota(data.balance)
      } catch (e) {
        /* 忽略 */
      }
    }
  }
}
</script>

<style scoped>
.quota {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  flex-shrink: 0;
  white-space: nowrap;
}
</style>
