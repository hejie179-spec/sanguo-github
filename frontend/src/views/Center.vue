<template>
  <div class="center-page">
    <div class="sg-page-title">
      <div>
        <h1>个人中心</h1>
        
      </div>
    </div>

    <el-card class="sg-card">
    <div v-if="!isLogin" class="guest-tip">
      <el-alert title="游客模式：可浏览页面，登录后可管理个人资料、收藏、评论、发帖、提问等功能" type="info" show-icon :closable="false" />
      <div class="guest-actions">
        <el-button type="primary" @click="goLogin">去登录</el-button>
        <el-button @click="goRegister">去注册</el-button>
      </div>
    </div>
    <el-tabs v-model="activeTab" class="tabs">
      <el-tab-pane label="个人信息" name="profile">
        <el-card class="sg-card inner">
          <el-form :model="profile" label-width="80px" style="max-width:400px">
            <el-form-item label="用户名"><el-input v-model="profile.username" disabled /></el-form-item>
            <el-form-item label="真实姓名"><el-input v-model="profile.realName" /></el-form-item>
            <el-form-item label="手机"><el-input v-model="profile.phone" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="profile.email" /></el-form-item>
            <el-form-item><el-button type="primary" :disabled="!isLogin" @click="saveProfile">保存</el-button></el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="我的文章" name="article">
        <el-table :data="myArticles" v-loading="articleLoading">
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">{{ { 0: '待审核', 1: '已通过', 2: '已驳回' }[row.status] }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="180" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="我的收藏" name="collection">
        <div class="bar">
          <el-select v-model="collectType" placeholder="全部类型" style="width:140px" @change="loadCollections">
            <el-option label="全部" :value="null" />
            <el-option label="文章" :value="1" />
            <el-option label="人物" :value="2" />
            <el-option label="事件" :value="3" />
            <el-option label="史料" :value="4" />
            <el-option label="典故" :value="5" />
            <el-option label="答疑" :value="6" />
            <el-option label="论坛" :value="7" />
          </el-select>
          <el-button @click="loadCollections">刷新</el-button>
        </div>
        <el-table :data="collections" v-loading="collectLoading" @row-click="goCollection">
          <el-table-column prop="targetType" label="类型" width="90">
            <template #default="{ row }">{{ { 1: '文章', 2: '人物', 3: '事件', 4: '史料', 5: '典故', 6: '答疑', 7: '论坛' }[row.targetType] }}</template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="createTime" label="收藏时间" width="180" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="danger" @click.stop="cancelCollect(row)">取消收藏</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="我的评论" name="comment">
        <div class="bar">
          <el-button @click="loadMyComments">刷新</el-button>
        </div>
        <el-table :data="myComments" v-loading="commentLoading">
          <el-table-column prop="targetType" label="类型" width="90">
            <template #default="{ row }">
              {{ row.kind === 'forumReply' ? '论坛' : ({ 1: '文章', 2: '人物', 3: '事件', 4: '史料', 5: '典故' }[row.targetType]) }}
            </template>
          </el-table-column>
          <el-table-column prop="targetTitle" label="目标" min-width="180" show-overflow-tooltip />
          <el-table-column prop="content" label="我的评论" min-width="220" show-overflow-tooltip />
          <el-table-column prop="parentUsername" label="回复对象" width="110">
            <template #default="{ row }">{{ row.parentId && row.parentId !== 0 ? (row.parentUsername || '-') : '-' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button link type="primary" @click="goCommentTarget(row)">查看</el-button>
              <el-button
                v-if="row.parentId && row.parentId !== 0"
                link
                type="primary"
                @click="openReply(row)"
              >
                回复他
              </el-button>
              <el-button link type="danger" @click="deleteMyComment(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="commentCurrent"
          :page-size="commentSize"
          :total="commentTotal"
          layout="prev, pager, next"
          @current-change="loadMyComments"
          style="margin-top:16px"
        />
      </el-tab-pane>
      <el-tab-pane label="我的提问" name="ai">
        <el-table :data="aiHistory">
          <el-table-column prop="questionContent" label="提问" />
          <el-table-column prop="createTime" label="时间" width="180" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
    </el-card>
    <el-dialog v-model="replyVisible" title="回复评论" width="520px">
      <div v-if="replyRow" style="margin-bottom:10px; color: var(--sanguo-text-secondary)">
        回复：{{ replyRow.parentUsername }}
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
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { useUserStore } from '../stores/user'
import { ElMessageBox, ElMessage } from 'element-plus'

// 个人中心（路由：/center，支持游客浏览，登录后可使用全部功能）
// - 个人信息：GET/PUT /user/profile
// - 我的文章：GET /article/my
// - 我的收藏：GET /collection/my-detail + 取消收藏
// - 我的评论：GET /comment/my-all + 删除/回复
// - 我的提问：GET /ai/history
const userStore = useUserStore()
const router = useRouter()
const isLogin = computed(() => !!userStore.token)
const activeTab = ref('profile')
const profile = reactive({ username: '', realName: '', phone: '', email: '' })
const myArticles = ref([])
const articleLoading = ref(false)
const collections = ref([])
const collectLoading = ref(false)
const collectType = ref(null)
const aiHistory = ref([])
const myComments = ref([])
const commentLoading = ref(false)
const commentCurrent = ref(1)
const commentSize = ref(10)
const commentTotal = ref(0)
const replyVisible = ref(false)
const replyRow = ref(null)
const replyContent = ref('')
const replying = ref(false)

async function loadProfile() {
  const data = await request.get('/user/profile')
  Object.assign(profile, data)
}
async function saveProfile() {
  await request.put('/user/profile', profile)
  const data = await request.get('/auth/info')
  userStore.setUser(data)
}

async function loadArticles() {
  articleLoading.value = true
  try {
    const res = await request.get('/article/my', { params: { current: 1, size: 50 } })
    myArticles.value = (res.records || []).map(r => ({ ...r, createTime: r.createTime ? new Date(r.createTime).toLocaleString('zh-CN') : '' }))
  } finally { articleLoading.value = false }
}
async function loadCollections() {
  collectLoading.value = true
  try {
    const res = await request.get('/collection/my-detail', { params: { current: 1, size: 50, targetType: collectType.value } })
    collections.value = (res.records || []).map(r => ({ ...r, createTime: r.createTime ? new Date(r.createTime).toLocaleString('zh-CN') : '' }))
  } finally { collectLoading.value = false }
}
async function loadAi() {
  const res = await request.get('/ai/history', { params: { current: 1, size: 50 } })
  aiHistory.value = (res || []).map(r => ({ ...r, createTime: r.createTime ? new Date(r.createTime).toLocaleString('zh-CN') : '' }))
}

async function loadMyComments() {
  commentLoading.value = true
  try {
    const res = await request.get('/comment/my-all', { params: { current: commentCurrent.value, size: commentSize.value } })
    myComments.value = (res.records || []).map(r => ({ ...r, createTime: r.createTime ? new Date(r.createTime).toLocaleString('zh-CN') : '' }))
    commentTotal.value = res.total || 0
  } finally { commentLoading.value = false }
}

watch(activeTab, v => {
  if (!isLogin.value) return
  if (v === 'article') loadArticles()
  else if (v === 'collection') loadCollections()
  else if (v === 'comment') loadMyComments()
  else if (v === 'ai') loadAi()
})

function goCollection(row) {
  const t = row.targetType
  const id = row.targetId
  if (t === 1) {
    router.push('/article/' + id)
    return
  }
  if (t === 7) {
    router.push('/forum/' + id)
    return
  }
  if (t === 2) router.push('/resource/person/' + id)
  else if (t === 3) router.push('/resource/event/' + id)
  else if (t === 4) router.push('/resource/literature/' + id)
  else if (t === 5) router.push('/resource/allusion/' + id)
}

async function cancelCollect(row) {
  const res = await request.post('/collection/toggle', null, { params: { targetType: row.targetType, targetId: row.targetId } })
  if (!res) loadCollections()
}

function goCommentTarget(row) {
  if (row.kind === 'forumReply') {
    router.push('/forum/' + row.topicId)
    return
  }
  const t = row.targetType
  const id = row.targetId
  if (t === 1) {
    router.push('/article/' + id)
    return
  }
  if (t === 2) router.push('/resource/person/' + id)
  else if (t === 3) router.push('/resource/event/' + id)
  else if (t === 4) router.push('/resource/literature/' + id)
  else if (t === 5) router.push('/resource/allusion/' + id)
}

function openReply(row) {
  replyRow.value = row
  replyContent.value = ''
  replyVisible.value = true
}

async function submitReply() {
  if (!replyRow.value) return
  if (!replyContent.value.trim()) return
  replying.value = true
  try {
    if (replyRow.value.kind === 'forumReply') {
      await request.post('/forum/reply/save', {
        topicId: replyRow.value.topicId,
        parentId: replyRow.value.parentId,
        content: replyContent.value.trim()
      })
    } else {
      await request.post('/comment/add', {
        targetType: replyRow.value.targetType,
        targetId: replyRow.value.targetId,
        parentId: replyRow.value.parentId,
        content: replyContent.value.trim()
      })
    }
    replyVisible.value = false
    loadMyComments()
  } finally { replying.value = false }
}

async function deleteMyComment(row) {
  await ElMessageBox.confirm('确定删除该评论及其所有回复？')
  if (row.kind === 'forumReply') {
    await request.delete('/forum/reply/' + row.id)
  } else {
    await request.delete('/comment/' + row.id)
  }
  ElMessage.success('已删除')
  loadMyComments()
}

onMounted(() => {
  if (!isLogin.value) return
  loadProfile()
  if (activeTab.value === 'article') loadArticles()
})

function goLogin() {
  router.push('/login')
}
function goRegister() {
  router.push('/register')
}
</script>

<style lang="scss" scoped>
.tabs :deep(.el-tabs__header) {
  margin: 0 0 12px;
}

.inner {
  box-shadow: none;
}

.bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.guest-tip {
  margin-bottom: 14px;
}
.guest-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}
</style>
