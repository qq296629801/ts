<template>
  <div class="public-gallery">
    <h2>作品展示</h2>
    <p class="hint">精选已上架模版封面，登录后可使用模版生图或充值</p>
    <el-row :gutter="12">
      <el-col v-for="item in items" :key="item.id" :xs="12" :sm="8" :md="6">
        <el-card :body-style="{ padding: '8px' }" shadow="hover">
          <img :src="item.imageUrl" class="thumb" :alt="item.title" />
          <p class="title">{{ item.title }}</p>
          <p v-if="item.type === 'TEMPLATE'" class="meta">使用 {{ item.useCount }} 次</p>
        </el-card>
      </el-col>
    </el-row>
    <el-pagination
      v-if="total > size"
      layout="prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page.sync="page"
      @current-change="load"
    />
  </div>
</template>

<script>
import http from '../../api/http'

export default {
  name: 'PublicGallery',
  data() {
    return { items: [], page: 1, size: 20, total: 0 }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      const data = await http.get('/api/v1/gallery/public', {
        params: { page: this.page, size: this.size }
      })
      this.items = data.items
      this.total = data.total
    }
  }
}
</script>

<style scoped>
.public-gallery { max-width: 1200px; margin: 0 auto; }
.hint { color: #666; font-size: 14px; margin-bottom: 16px; }
.thumb { width: 100%; height: 160px; object-fit: cover; border-radius: 4px; }
.title { font-weight: bold; margin: 8px 0 4px; }
.meta { font-size: 12px; color: #999; }
</style>
