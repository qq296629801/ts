<template>
  <div class="gallery">
    <h2>个人图库</h2>
    <el-row :gutter="12">
      <el-col v-for="img in items" :key="img.id" :xs="12" :sm="8" :md="6">
        <el-card :body-style="{ padding: '8px' }">
          <img :src="img.imageUrl" class="thumb" />
          <p class="prompt">{{ img.prompt }}</p>
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
  name: 'ImageGallery',
  data() {
    return { items: [], page: 1, size: 12, total: 0 }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      const data = await http.get('/api/v1/user/images', { params: { page: this.page, size: this.size } })
      this.items = data.items
      this.total = data.total
    }
  }
}
</script>

<style scoped>
.gallery { max-width: 1100px; margin: 0 auto; }
.thumb { width: 100%; border-radius: 6px; }
.prompt { font-size: 12px; color: #666; margin-top: 6px; height: 36px; overflow: hidden; }
</style>
