<template>
  <div class="publish">
    <el-card>
      <h3>发布模版</h3>
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input type="textarea" v-model="form.description" :rows="2" /></el-form-item>
        <el-form-item label="Prompt"><el-input type="textarea" v-model="form.prompt" :rows="5" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">提交审核</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="result" :title="'状态：' + result.status" :type="result.status === 'APPROVED' ? 'success' : 'info'" />
    </el-card>
  </div>
</template>

<script>
import http from '../../api/http'

export default {
  name: 'PublishTemplate',
  data() {
    return {
      categories: [],
      form: { title: '', description: '', prompt: '', categoryId: null },
      loading: false,
      result: null
    }
  },
  async mounted() {
    this.categories = await http.get('/api/v1/templates/categories')
  },
  methods: {
    async submit() {
      this.loading = true
      try {
        this.result = await http.post('/api/v1/templates/publish', this.form)
        this.$message.success('已提交')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.publish { max-width: 640px; margin: 0 auto; }
</style>
