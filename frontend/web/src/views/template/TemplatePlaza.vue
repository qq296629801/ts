<template>
  <div class="plaza">
    <el-card>
      <div class="toolbar">
        <el-select v-model="categoryId" placeholder="分类" clearable @change="load">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索标题" clearable style="width:200px" @keyup.enter.native="load" />
        <el-radio-group v-model="sort" @change="load">
          <el-radio-button label="latest">最新</el-radio-button>
          <el-radio-button label="hot">最热</el-radio-button>
        </el-radio-group>
        <router-link to="/template/publish"><el-button type="primary">发布模版</el-button></router-link>
      </div>
      <el-row :gutter="12">
        <el-col :span="6" v-for="t in items" :key="t.id">
          <el-card class="tpl" shadow="hover">
            <div class="title">{{ t.title }}</div>
            <p class="desc">{{ t.description || t.prompt.slice(0, 60) }}</p>
            <div class="meta">使用 {{ t.useCount }} · 赞 {{ t.likeCount }}</div>
            <el-button size="mini" type="primary" @click="useTemplate(t)">使用</el-button>
            <el-button size="mini" @click="like(t)">{{ t.liked ? '已赞' : '点赞' }}</el-button>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import http from '../../api/http'

export default {
  name: 'TemplatePlaza',
  data() {
    return {
      categories: [],
      items: [],
      categoryId: null,
      keyword: '',
      sort: 'latest'
    }
  },
  async mounted() {
    this.categories = await http.get('/api/v1/templates/categories')
    await this.load()
  },
  methods: {
    async load() {
      const data = await http.get('/api/v1/templates', {
        params: {
          categoryId: this.categoryId || undefined,
          keyword: this.keyword || undefined,
          sort: this.sort,
          page: 1,
          size: 24
        }
      })
      this.items = data.items
    },
    async useTemplate(t) {
      const data = await http.post(`/api/v1/templates/${t.id}/use`)
      this.$router.push({ path: '/chat', query: { prompt: data.prompt } })
    },
    async like(t) {
      const data = await http.post(`/api/v1/templates/${t.id}/like`)
      t.liked = data.liked
      t.likeCount = data.likeCount
    }
  }
}
</script>

<style scoped>
.plaza { max-width: 1200px; margin: 0 auto; }
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.tpl { margin-bottom: 12px; min-height: 140px; }
.title { font-weight: bold; }
.desc { font-size: 12px; color: #666; min-height: 36px; }
.meta { font-size: 12px; color: #999; margin: 8px 0; }
</style>
