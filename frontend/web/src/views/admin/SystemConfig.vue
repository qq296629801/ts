<template>
  <el-card>
    <h3>系统配置</h3>
    <p class="tip">保存后仅对未来新业务生效</p>
    <el-form label-width="220px">
      <el-form-item v-for="(val, key) in config" :key="key" :label="key">
        <el-input v-model="config[key]" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script>
import http from '../../api/http'

export default {
  name: 'SystemConfig',
  data() {
    return { config: {} }
  },
  async mounted() {
    this.config = await http.get('/api/v1/admin/config')
  },
  methods: {
    async save() {
      await http.put('/api/v1/admin/config', this.config)
      this.$message.success('已保存')
    }
  }
}
</script>

<style scoped>
.tip { color: #999; font-size: 12px; margin-bottom: 12px; }
</style>
