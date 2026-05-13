<template>
  <div class="forum-detail" v-loading="loading">
    <template v-if="topic">
      <div class="sg-page-title">
        <div>
          <h1>{{ topic.title }}</h1>
          <div class="sg-subtitle">作者：{{ authorName }} · 浏览 {{ topic.viewCount }} · 回复 {{ topic.replyCount }} · 点赞 {{ topic.likeCount }}</div>
        </div>
        <div class="actions" v-if="userStore.token">
          <el-button :type="liked ? 'warning' : 'primary'" @click="toggleLike">{{ liked ? '已赞' : '点赞' }}</el-button>
          <el-button :type="collected ? 'warning' : 'primary'" @click="toggleCollect">{{ collected ? '已收藏' : '收藏' }}</el-button>
        </div>
      </div>

      <el-row :gutter="16">
        <el-col :xs="24" :lg="16">
          <el-card class="sg-card">
            <div v-if="topic.coverUrl" class="cover">
              <el-image :src="topic.coverUrl" fit="contain" class="cover-img" />
            </div>
            <div class="sg-prose content" v-html="topic.content"></div>
          </el-card>

          <el-card class="sg-card comment-card">
            <template #header>
              <div class="card-head">回复</div>
            </template>
            <div v-if="userStore.token" class="comment-editor">
              <el-input v-model="replyContent" type="textarea" placeholder="发表回复" :rows="2" />
              <div class="comment-actions">
                <el-button type="primary" size="small" @click="submitReply">提交</el-button>
              </div>
            </div>
            <div v-else class="sg-muted">登录后可回复</div>
            <CommentTree :items="replies" :can-reply="!!userStore.token" @reply="openReply" />
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="8">
          <el-card class="sg-card side">
            <template #header>
              <div class="card-head">讨论提示</div>
            </template>
            <div class="sg-muted tip">欢迎引用史料、标注出处、给出不同观点。请避免人身攻击与无意义灌水。</div>
            <div class="sg-divider" />
            <div class="kv">
              <div class="k">点赞</div>
              <div class="v">表达支持与认可</div>
            </div>
            <div class="kv">
              <div class="k">收藏</div>
              <div class="v">用于个人中心快速回看</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
    <el-dialog v-model="replyVisible" title="回复评论" width="520px">
      <div v-if="replyTarget" style="margin-bottom:10px; color: var(--sanguo-text-secondary)">
        回复：{{ replyTarget.username }}
      </div>
      <el-input v-model="replyText" type="textarea" :rows="3" placeholder="请输入回复内容" />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="submitChildReply">提交</el-button>
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

// 论坛主题详情（路由：/forum/:id）
// - 主题详情：GET /forum/topic/detail/:id
// - 回复列表：GET /forum/reply/list/:topicId（或类似接口，见本文件 request 调用）
// - 点赞/收藏：登录后可操作
// - 回复/楼中楼：CommentTree 树形展示，submitReply/submitChildReply 提交
const route = useRoute()
const userStore = useUserStore()
const id = computed(() => route.params.id)
const loading = ref(true)
const topic = ref(null)
const authorName = ref('')
const replies = ref([])
const replyContent = ref('')
const liked = ref(false)
const collected = ref(false)
const replyVisible = ref(false)
const replyTarget = ref(null)
const replyText = ref('')
const replying = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await request.get('/forum/topic/detail/' + id.value)
    topic.value = res.topic
    authorName.value = res.authorName || ''
    liked.value = !!res.liked
    await reloadReplies()
    if (userStore.token) {
      const check = await request.get('/collection/check', { params: { targetType: 7, targetId: id.value } })
      collected.value = !!check
    }
  } finally { loading.value = false }
})

function formatTree(list) {
  return (list || []).map(r => ({
    ...r,
    createTime: r.createTime ? new Date(r.createTime).toLocaleString('zh-CN') : '',
    children: formatTree(r.children || [])
  }))
}

async function reloadReplies() {
  const list = await request.get('/forum/reply/list/' + id.value)
  replies.value = formatTree(list || [])
}

async function toggleLike() {
  const res = await request.post('/forum/topic/like/' + id.value)
  liked.value = res
  if (topic.value) topic.value.likeCount = (topic.value.likeCount || 0) + (res ? 1 : -1)
}

async function toggleCollect() {
  const res = await request.post('/collection/toggle', null, { params: { targetType: 7, targetId: id.value } })
  collected.value = res
}

async function submitReply() {
  if (!replyContent.value.trim()) return
  await request.post('/forum/reply/save', { topicId: parseInt(id.value), content: replyContent.value.trim() })
  replyContent.value = ''
  await reloadReplies()
  if (topic.value) topic.value.replyCount = (topic.value.replyCount || 0) + 1
}

function openReply(r) {
  replyTarget.value = r
  replyText.value = ''
  replyVisible.value = true
}

async function submitChildReply() {
  if (!replyTarget.value) return
  if (!replyText.value.trim()) return
  replying.value = true
  try {
    await request.post('/forum/reply/save', {
      topicId: parseInt(id.value),
      parentId: replyTarget.value.id,
      content: replyText.value.trim()
    })
    replyVisible.value = false
    await reloadReplies()
    if (topic.value) topic.value.replyCount = (topic.value.replyCount || 0) + 1
  } finally {
    replying.value = false
  }
}
</script>

<style lang="scss" scoped>
.forum-detail {
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
