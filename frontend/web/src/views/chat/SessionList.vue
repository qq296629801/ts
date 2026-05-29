<template>
  <div class="session-list">
    <el-button type="primary" size="small" icon="el-icon-plus" @click="createSession" style="width:100%;margin-bottom:12px">
      新建会话
    </el-button>
    <div
      v-for="s in sessions"
      :key="s.id"
      :class="['item', { active: s.id === currentId }]"
      @click="$emit('select', s.id)"
    >
      <span class="title">{{ s.title }}</span>
      <el-button type="text" icon="el-icon-delete" @click.stop="remove(s.id)" />
    </div>
  </div>
</template>

<script>
import http from '../../api/http'

export default {
  name: 'SessionList',
  props: {
    currentId: { type: String, default: '' }
  },
  data() {
    return { sessions: [] }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      this.sessions = await http.get('/api/v1/ai/sessions')
    },
    async createSession() {
      const s = await http.post('/api/v1/ai/sessions', { mode: 'CHAT' })
      await this.load()
      this.$emit('select', s.id)
    },
    async remove(id) {
      await http.delete(`/api/v1/ai/sessions/${id}`)
      await this.load()
      if (id === this.currentId) {
        this.$emit('select', this.sessions[0]?.id || '')
      }
    }
  }
}
</script>

<style scoped>
.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
}
.item:hover, .item.active { background: #ecf5ff; }
.title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
</style>
