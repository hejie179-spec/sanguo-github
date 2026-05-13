<template>
  <div class="article-detail" v-loading="loading">
    <template v-if="detail">
      <div class="sg-page-title">
        <div>
          <h1>{{ detail.article?.title }}</h1>
          <div class="sg-subtitle">作者：{{ detail.authorName }} · 阅读 {{ detail.article?.viewCount }} · 点赞 {{ detail.article?.likeCount }}</div>
        </div>
        <div class="actions">
          <el-button :type="detail.liked ? 'warning' : 'primary'" @click="toggleLike">{{ detail.liked ? '已赞' : '点赞' }}</el-button>
          <el-button v-if="userStore.token" :type="collected ? 'warning' : 'primary'" @click="toggleCollect">{{ collected ? '已收藏' : '收藏' }}</el-button>
        </div>
      </div>

      <el-row :gutter="16">
        <el-col :xs="24" :lg="16">
          <el-card class="sg-card">
            <div v-if="detail.article?.coverUrl" class="cover">
              <el-image :src="detail.article.coverUrl" fit="contain" class="cover-img" />
            </div>
            <div class="sg-prose content" v-html="detail.article?.content"></div>
          </el-card>

          <el-card class="sg-card comment-card">
            <template #header>
              <div class="card-head">评论</div>
            </template>
            <div v-if="userStore.token" class="comment-editor">
              <el-input v-model="commentContent" type="textarea" placeholder="评论" :rows="2" />
              <div class="comment-actions">
                <el-button type="primary" size="small" @click="submitComment">提交</el-button>
              </div>
            </div>
            <div v-else class="sg-muted">登录后可评论</div>
            <CommentTree :items="comments" :can-reply="!!userStore.token" @reply="openReply" />
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="8">
          <el-card class="sg-card side">
            <template #header>
              <div class="card-head">阅读提示</div>
            </template>
            <div class="sg-muted tip">文章内容由用户发布，若存在不准确之处可在评论区补充史料或引用来源。</div>
            <div class="sg-divider" />
            <div class="kv">
              <div class="k">收藏</div>
              <div class="v">用于个人中心快速回看</div>
            </div>
            <div class="kv">
              <div class="k">点赞</div>
              <div class="v">用于热度与推荐</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
    <el-dialog v-model="replyVisible" title="回复评论" width="520px">
      <div v-if="replyTarget" style="margin-bottom:10px; color: var(--sanguo-text-secondary)">
        回复：{{ replyTarget.username }}
      </div>
      <el-input v-model="replyContent" type="textarea" :rows="3" placeholder="请输入回复内容" />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="submitReply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'
import { useUserStore } from '../stores/user'
import CommentTree from '../components/CommentTree.vue'

// 文章详情页（路由：/article/:id）
// - 详情数据：GET /article/detail/:id（通常包含 article + liked 等扩展字段）
// - 点赞：POST /article/like（或类似接口，见本文件的 request 调用）
// - 收藏：登录后可收藏（用于个人中心回看）
// - 评论：CommentTree 负责树形评论渲染
const route = useRoute()
const userStore = useUserStore()
const id = computed(() => route.params.id)
const loading = ref(true)
const detail = ref(null)
const comments = ref([])
const commentContent = ref('')
const collected = ref(false)
const replyVisible = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')
const replying = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    detail.value = await request.get('/article/detail/' + id.value)
    if (detail.value.article) {
      detail.value.article.createTime = detail.value.article.createTime ? new Date(detail.value.article.createTime).toLocaleString('zh-CN') : ''
    }
    await reloadComments()
    if (userStore.token) {
      const check = await request.get('/collection/check', { params: { targetType: 1, targetId: id.value } })
      collected.value = !!check
    }
  } finally { loading.value = false }
})

function formatTree(list) {
  return (list || []).map(c => ({
    ...c,
    createTime: c.createTime ? new Date(c.createTime).toLocaleString('zh-CN') : '',
    children: formatTree(c.children || [])
  }))
}

async function reloadComments() {
  const res = await request.get('/comment/list/1/' + id.value)
  comments.value = formatTree(res || [])
}

async function toggleLike() {
  const liked = await request.post('/article/like/' + id.value)
  detail.value.liked = liked
  detail.value.article.likeCount = (detail.value.article.likeCount || 0) + (liked ? 1 : -1)
}

async function toggleCollect() {
  const res = await request.post('/collection/toggle', null, { params: { targetType: 1, targetId: id.value } })
  collected.value = res
}

async function submitComment() {
  if (!commentContent.value.trim()) return
  await request.post('/comment/add', { targetType: 1, targetId: parseInt(id.value), content: commentContent.value.trim() })
  commentContent.value = ''
  await reloadComments()
}

function openReply(c) {
  replyTarget.value = c
  replyContent.value = ''
  replyVisible.value = true
}

async function submitReply() {
  if (!replyTarget.value) return
  if (!replyContent.value.trim()) return
  replying.value = true
  try {
    await request.post('/comment/add', {
      targetType: 1,
      targetId: parseInt(id.value),
      parentId: replyTarget.value.id,
      content: replyContent.value.trim()
    })
    replyVisible.value = false
    await reloadComments()
  } finally {
    replying.value = false
  }
}
</script>

<style lang="scss" scoped>
.article-detail {
  max-width: 1200px;
  margin: 0 auto;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.cover {
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid var(--sanguo-border);
  margin-bottom: 24px;
  background: #f4f0e8;
}

.cover-img {
  width: 100%;
  height: 360px;
  display: block;
}

.content {
  line-height: 1.8;
  font-size: 16px;
  color: #333;
}

.content :deep(p) {
  margin-bottom: 1em;
}

.comment-card {
  margin-top: 24px;
}

.card-head {
  font-weight: 600;
  font-size: 18px;
  color: var(--sanguo-primary);
  border-bottom: 2px solid var(--sanguo-primary);
  padding-bottom: 12px;
  display: inline-block;
}

.comment-editor {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px dashed #e5e5e5;
}

.comment-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.side {
  position: sticky;
  top: 88px;
}

.tip {
  line-height: 1.6;
  padding: 12px;
  background: #fcfcfc;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  margin-bottom: 16px;
}

.kv {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px dashed var(--sanguo-border);
}

.kv:last-child {
  border-bottom: none;
}

.k {
  color: var(--sanguo-text-secondary);
}

.v {
  color: var(--sanguo-text);
  font-weight: 500;
}

@media (max-width: 980px) {
  .side {
    position: static;
    margin-top: 24px;
  }
}

@media (max-width: 520px) {
  .cover-img {
    height: 220px;
  }
}
</style>
