<template>
  <el-card>
    <h3>待审核模版</h3>
    <el-table :data="items">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="prompt" label="Prompt" show-overflow-tooltip />
      <el-table-column label="操作" width="200">
        <template slot-scope="{ row }">
          <el-button size="mini" type="success" @click="audit(row.id, 'APPROVE')">通过</el-button>
          <el-button size="mini" type="danger" @click="audit(row.id, 'REJECT')">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script>
import http from '../../api/http'

export default {
  name: 'TemplateAudit',
  data() {
    return { items: [] }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      const data = await http.get('/api/v1/admin/templates/pending')
      this.items = data.items
    },
    async audit(id, action) {
      const reason = action === 'REJECT' ? '人工拒绝' : null
      await http.post(`/api/v1/admin/templates/${id}/audit`, { action, reason })
      this.$message.success('已处理')
      await this.load()
    }
  }
}
</script>
