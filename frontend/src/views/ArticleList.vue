<template>
  <div class="article-list">
    <div class="sg-page-title">
      <div>
        <h1>文章</h1>
        
      </div>
      <el-button v-if="userStore.token" type="primary" @click="showPublish = true">发布文章</el-button>
    </div>
    <el-dialog v-model="showPublish" title="发布文章" width="700px" @close="resetPublish">
      <el-form :model="publishForm">
        <el-form-item label="标题"><el-input v-model="publishForm.title" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="publishForm.category" placeholder="选填" /></el-form-item>
        <el-form-item label="封面">
          <ImageUpload v-model="publishForm.coverUrl" />
        </el-form-item>
        <el-form-item label="内容"><el-input v-model="publishForm.content" type="textarea" :rows="8" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublish = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="publish">提交审核</el-button>
      </template>
    </el-dialog>

    <div class="ih-list-wrapper" v-loading="loading">
      <div v-for="item in list" :key="item.id" class="ih-list-item" @click="$router.push('/article/'+item.id)">
        <div class="item-content">
          <h3 class="item-title">{{ item.title }}</h3>
          <div class="item-meta">
            <span>{{ item.authorName }}</span>
            <span v-if="item.category" class="meta-tag">{{ item.category }}</span>
            <span>阅读 {{ item.viewCount || 0 }}</span>
            <span>点赞 {{ item.likeCount || 0 }}</span>
          </div>
          <div class="item-time">{{ item.createTime }}</div>
        </div>
        <div class="item-cover" v-if="item.coverUrl">
          <img :src="item.coverUrl" />
        </div>
      </div>
      
      <div v-if="!loading && list.length === 0" class="ih-empty">暂无文章</div>

      <div class="ih-pager" v-if="total > 0">
        <el-pagination background v-model:current-page="current" :page-size="size" :total="total" layout="prev, pager, next" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../utils/request'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import ImageUpload from '../components/ImageUpload.vue'

// 文章列表页（路由：/article）
// - 列表数据：GET /article/list（分页）
// - 发布文章：登录后可打开弹窗，POST /article/publish（提交后进入审核流程）
// - 文章详情：点击条目跳转到 /article/:id
const userStore = useUserStore()
const list = ref([])
const loading = ref(false)
const current = ref(1)
const size = ref(10)
const total = ref(0)
const showPublish = ref(false)
const publishing = ref(false)
const publishForm = reactive({ title: '', category: '', coverUrl: '', content: '' })

async function load() {
  loading.value = true
  try {
    const res = await request.get('/article/list', { params: { current: current.value, size: size.value } })
    list.value = (res.records || []).map(r => ({ ...r, createTime: formatTime(r.createTime) }))
    total.value = res.total || 0
  } finally { loading.value = false }
}
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }

function resetPublish() {
  Object.assign(publishForm, { title: '', category: '', coverUrl: '', content: '' })
}

async function publish() {
  if (!publishForm.title || !publishForm.content) return
  publishing.value = true
  try {
    await request.post('/article/save', { ...publishForm, status: 0 })
    ElMessage.success('已提交审核，可在个人中心查看')
    showPublish.value = false
    resetPublish()
  } finally { publishing.value = false }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.ih-list-wrapper {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  border: 1px solid var(--sanguo-border);
}

.ih-list-item {
  display: flex;
  justify-content: space-between;
  padding: 20px 0;
  border-bottom: 1px dashed var(--sanguo-border);
  cursor: pointer;
  transition: all 0.3s;
}

.ih-list-item:last-child {
  border-bottom: none;
}

.ih-list-item:hover {
  background-color: #fafafa;
  padding-left: 8px;
  padding-right: 8px;
  margin: 0 -8px;
}

.ih-list-item:hover .item-title {
  color: var(--sanguo-primary);
}

.item-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-right: 20px;
}

.item-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--sanguo-text);
  margin: 0 0 12px;
  line-height: 1.4;
}

.item-title::before {
  content: "•";
  color: var(--sanguo-primary);
  margin-right: 8px;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--sanguo-text-secondary);
  margin-bottom: 8px;
}

.meta-tag {
  background: rgba(179, 0, 0, 0.05);
  color: var(--sanguo-primary);
  padding: 2px 8px;
  border-radius: 4px;
}

.item-time {
  font-size: 12px;
  color: #999;
}

.item-cover {
  width: 160px;
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid var(--sanguo-border);
}

.item-cover img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f4f0e8;
  transition: transform 0.3s;
}

.ih-list-item:hover .item-cover img {
  transform: scale(1.05);
}

.ih-empty {
  text-align: center;
  padding: 40px;
  color: #999;
}

.ih-pager {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@media (max-width: 600px) {
  .ih-list-item {
    flex-direction: column-reverse;
    gap: 12px;
  }
  .item-content {
    padding-right: 0;
  }
  .item-cover {
    width: 100%;
    height: 180px;
  }
}
</style>
