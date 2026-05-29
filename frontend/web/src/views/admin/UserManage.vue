<template>
  <el-card>
    <h3>用户管理</h3>
    <el-table :data="items">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="phone" label="手机" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="角色" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="160">
        <template slot-scope="{ row }">
          <el-button v-if="row.status === 1" size="mini" @click="setStatus(row.id, 0)">禁用</el-button>
          <el-button v-else size="mini" @click="setStatus(row.id, 1)">启用</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script>
import http from '../../api/http'

export default {
  name: 'UserManage',
  data() {
    return { items: [] }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      const data = await http.get('/api/v1/admin/users')
      this.items = data.items
    },
    async setStatus(id, status) {
      await http.patch(`/api/v1/admin/users/${id}/status`, { status })
      this.$message.success('已更新')
      await this.load()
    }
  }
}
</script>
