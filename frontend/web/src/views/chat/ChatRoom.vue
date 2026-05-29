<template>
  <div class="chat">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card>
          <session-list :current-id="sessionId" @select="onSelectSession" />
        </el-card>
      </el-col>
      <el-col :span="18">
        <el-card class="main">
          <div class="messages" ref="msgBox">
            <div v-if="!sessionId" class="empty">请选择或新建会话</div>
            <div v-for="m in messages" :key="m.id" :class="['msg', m.role]">
              <span v-if="m.contentType === 'TEXT'">{{ m.content }}</span>
              <img v-else-if="isImage(m)" :src="imageUrl(m)" class="gen-img" />
              <span v-else>{{ m.content }}</span>
            </div>
          </div>
          <div class="input-area" v-if="sessionId">
            <el-input type="textarea" :rows="3" v-model="input" placeholder="输入消息，Enter 发送" @keydown.native="onKey" />
            <div class="actions">
              <el-button @click="sendText" :loading="chatLoading">发送文字</el-button>
              <el-button type="primary" @click="generateImage" :loading="genLoading">生成图片</el-button>
              <router-link to="/gallery"><el-button type="text">图库</el-button></router-link>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import http from '../../api/http'
import SessionList from './SessionList.vue'
import { mapMutations } from 'vuex'

export default {
  name: 'ChatRoom',
  components: { SessionList },
  data() {
    return {
      input: '',
      messages: [],
      chatLoading: false,
      genLoading: false,
      sessionId: ''
    }
  },
  mounted() {
    const prompt = this.$route.query.prompt
    if (prompt) {
      this.input = prompt
    }
  },
  methods: {
    ...mapMutations(['setQuota']),
    isImage(m) {
      return m.contentType === 'IMAGE_RESULT' || (m.content && m.content.includes('"type":"image"'))
    },
    imageUrl(m) {
      try {
        const o = JSON.parse(m.content)
        return o.url
      } catch (e) {
        return m.content
      }
    },
    async onSelectSession(id) {
      this.sessionId = id
      if (!id) {
        this.messages = []
        return
      }
      const data = await http.get(`/api/v1/ai/sessions/${id}/messages`, { params: { page: 1, size: 20 } })
      this.messages = data.items
      this.$nextTick(() => {
        const box = this.$refs.msgBox
        if (box) box.scrollTop = box.scrollHeight
      })
    },
    onKey(e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        this.sendText()
      }
    },
    async sendText() {
      if (!this.input.trim() || !this.sessionId) return
      const text = this.input.trim()
      this.input = ''
      this.chatLoading = true
      try {
        const token = localStorage.getItem('accessToken')
        const res = await fetch('/api/v1/ai/chat', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify({
            sessionId: this.sessionId,
            messages: [{ role: 'user', content: text }]
          })
        })
        let assistantText = ''
        const reader = res.body.getReader()
        const decoder = new TextDecoder()
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          assistantText += decoder.decode(value)
        }
        await this.onSelectSession(this.sessionId)
      } catch (e) {
        this.$message.error('对话失败')
      } finally {
        this.chatLoading = false
      }
    },
    async generateImage() {
      const prompt = this.input.trim() || '一只可爱的猫咪'
      this.genLoading = true
      try {
        // 中继生图常需 30s～3min，需长于 http 默认 60s
        const data = await http.post(
          '/api/v1/ai/image/generate',
          { prompt, size: '1024x1024', sessionId: this.sessionId },
          { timeout: 360000 }
        )
        this.setQuota(data.remainingQuota)
        this.input = ''
        await this.onSelectSession(this.sessionId)
      } catch (e) {
        this.$message.error('生图失败')
      } finally {
        this.genLoading = false
      }
    }
  }
}
</script>

<style scoped>
.chat { max-width: 1200px; margin: 0 auto; }
.messages { min-height: 320px; max-height: 480px; overflow-y: auto; margin-bottom: 12px; }
.msg { margin: 8px 0; padding: 8px 12px; border-radius: 8px; }
.msg.user { background: #e8f4ff; text-align: right; }
.msg.assistant { background: #f5f5f5; }
.gen-img { max-width: 320px; border-radius: 8px; }
.actions { margin-top: 8px; text-align: right; }
.empty { color: #999; text-align: center; padding: 40px; }
</style>
