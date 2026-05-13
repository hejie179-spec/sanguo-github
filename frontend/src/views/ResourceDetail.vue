<template>
  <div class="detail-page" v-loading="loading">
    <template v-if="data">
      <div class="sg-page-title">
        <div>
          <h1>{{ (main && (main.name || main.title)) }}</h1>
          <div class="sg-subtitle" v-if="main?.dynasty">归属：{{ main.dynasty }}</div>
        </div>
        <div class="title-actions" v-if="userStore.token">
          <el-button :type="collected ? 'warning' : 'primary'" @click="toggleCollect">{{ collected ? '已收藏' : '收藏' }}</el-button>
        </div>
      </div>

      <el-row :gutter="16" class="detail-grid">
        <el-col :xs="24" :lg="16">
          <el-card class="sg-card main-card">
            <div v-if="main?.imageUrl" class="cover">
              <el-image :src="main.imageUrl" fit="contain" class="cover-img" />
            </div>
            <div class="sg-prose content" v-html="main?.content || main?.introduction || main?.source || ''"></div>
          </el-card>

          <el-card class="sg-card comment-card">
            <template #header>
              <div class="card-head">评论</div>
            </template>
            <div v-if="userStore.token" class="comment-editor">
              <el-input v-model="commentContent" type="textarea" placeholder="发表评论" :rows="2" />
              <div class="comment-actions">
                <el-button type="primary" size="small" @click="submitComment">提交</el-button>
              </div>
            </div>
            <div v-else class="sg-muted">登录后可评论</div>
            <div class="comment-list">
              <CommentTree :items="comments" :can-reply="!!userStore.token" @reply="openReply" />
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="8">
          <el-card class="sg-card side-card">
            <template #header>
              <div class="card-head">条目信息</div>
            </template>
            <div class="kv" v-if="props.type === 'person' && main?.alias">
              <div class="k">别名</div>
              <div class="v">{{ main.alias }}</div>
            </div>
            <div class="kv" v-if="main?.dynasty">
              <div class="k">归属</div>
              <div class="v">{{ main.dynasty }}</div>
            </div>
            <div class="kv" v-if="props.type === 'literature' && main?.author">
              <div class="k">作者</div>
              <div class="v">{{ main.author }}</div>
            </div>
            <div class="kv" v-if="props.type === 'allusion' && main?.source">
              <div class="k">来源</div>
              <div class="v">{{ main.source }}</div>
            </div>
          </el-card>

          <el-card v-if="relatedList.length" class="sg-card related-card">
            <template #header>
              <div class="card-head">相关{{ relatedLabel }}</div>
            </template>
            <div class="related-tags">
              <el-tag v-for="r in relatedList" :key="r.id" class="rel" effect="plain" @click="goRelated(r)">
                {{ r.name || r.title }}
              </el-tag>
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
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { useUserStore } from '../stores/user'
import CommentTree from '../components/CommentTree.vue'

// 资源详情页（复用同一个组件展示人物/事件/史料/典故）
// 路由：/resource/{type}/:id（type 通过 router/index.js 的 props 传入）
// 功能包含：
// - 详情展示：main（人物/事件/史料/典故的主信息）
// - 收藏：登录后可收藏/取消收藏
// - 评论：登录后可发评论/回复评论（CommentTree 负责树形渲染）
const props = defineProps({ type: String })
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const id = computed(() => route.params.id)
const loading = ref(true)
const data = ref(null)
const comments = ref([])
const commentContent = ref('')
const collected = ref(false)
const replyVisible = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')
const replying = ref(false)

const titleKey = computed(() => props.type === 'person' ? 'name' : 'title')
const main = computed(() => {
  if (!data.value) return null
  if (data.value.person) return data.value.person
  if (data.value.event) return data.value.event
  return data.value[props.type]
})

const relatedList = computed(() => {
  if (!data.value) return []
  if (data.value.events) return data.value.events
  if (data.value.persons) return data.value.persons
  if (data.value.literatures) return data.value.literatures
  if (data.value.allusions) return data.value.allusions
  return []
})
const relatedLabel = computed(() => {
  if (data.value?.events) return '事件'
  if (data.value?.persons) return '人物'
  if (data.value?.literatures) return '史料'
  if (data.value?.allusions) return '典故'
  return ''
})

const apiMap = {
  person: () => request.get('/person/detail/' + id.value),
  event: () => request.get('/event/detail/' + id.value),
  literature: () => request.get('/literature/detail/' + id.value),
  allusion: () => request.get('/allusion/detail/' + id.value)
}

const resourceIdMap = { person: 2, event: 3, literature: 4, allusion: 5 }
const targetTypeComment = computed(() => resourceIdMap[props.type])

onMounted(async () => {
  loading.value = true
  try {
    if (props.type === 'literature' || props.type === 'allusion') {
      data.value = { [props.type]: await apiMap[props.type]() }
    } else {
      data.value = await apiMap[props.type]()
    }
    await reloadComments()
    if (userStore.token) {
      const check = await request.get('/collection/check', { params: { targetType: resourceIdMap[props.type], targetId: id.value } })
      collected.value = !!check
    }
  } finally {
    loading.value = false
  }
})

function formatTree(list) {
  return (list || []).map(c => ({
    ...c,
    createTime: c.createTime ? new Date(c.createTime).toLocaleString('zh-CN') : '',
    children: formatTree(c.children || [])
  }))
}

async function reloadComments() {
  const res = await request.get('/comment/list/' + targetTypeComment.value + '/' + id.value)
  comments.value = formatTree(res || [])
}

async function toggleCollect() {
  const res = await request.post('/collection/toggle', null, { params: { targetType: resourceIdMap[props.type], targetId: id.value } })
  collected.value = res
}

function goRelated(r) {
  const typeMap = { '事件': 'event', '人物': 'person', '史料': 'literature', '典故': 'allusion' }
  const t = typeMap[relatedLabel.value] || 'person'
  router.push('/resource/' + t + '/' + r.id)
}

async function submitComment() {
  if (!commentContent.value.trim()) return
  await request.post('/comment/add', {
    targetType: targetTypeComment.value,
    targetId: id.value,
    content: commentContent.value.trim()
  })
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
      targetType: targetTypeComment.value,
      targetId: id.value,
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
.resource-detail {
  max-width: 1200px;
  margin: 0 auto;
}

.title-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.detail-grid {
  margin-top: 8px;
}

.main-card {
  margin-bottom: 24px;
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

.side-card {
  margin-bottom: 24px;
}

.card-head {
  font-weight: 600;
  font-size: 18px;
  color: var(--sanguo-primary);
  border-bottom: 2px solid var(--sanguo-primary);
  padding-bottom: 12px;
  display: inline-block;
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

.related-card {
  margin-bottom: 24px;
}

.related-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.rel {
  cursor: pointer;
}

.comment-card {
  margin-top: 24px;
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

@media (max-width: 980px) {
  .side-card {
    margin-top: 24px;
  }
}

@media (max-width: 520px) {
  .cover-img {
    height: 220px;
  }
}
</style>
