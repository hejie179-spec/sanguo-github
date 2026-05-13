<template>
  <div class="admin-forum">
    <div class="sg-page-title">
      <div>
        <h1>论坛管理</h1>
        <div class="sg-subtitle">对主题进行置顶/关闭/删除，对回复进行清理。</div>
      </div>
    </div>

    <el-card class="sg-card">
    <el-tabs v-model="activeTab" class="tabs">
      <el-tab-pane label="主题" name="topic">
        <el-table :data="topics" v-loading="topicLoading" class="table">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">{{ { 0: '关闭', 1: '正常', 2: '隐藏' }[row.status] }}</template>
          </el-table-column>
          <el-table-column prop="replyCount" label="回复" width="80" />
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button link @click="setTop(row.id, row.topStatus === 1 ? 0 : 1)">{{ row.topStatus === 1 ? '取消置顶' : '置顶' }}</el-button>
              <el-button link @click="closeTopic(row.id)">关闭</el-button>
              <el-button link type="danger" @click="delTopic(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination v-model:current-page="topicPage" :page-size="10" :total="topicTotal" layout="prev, pager, next" @current-change="loadTopics" />
        </div>
      </el-tab-pane>
      <el-tab-pane label="回复" name="reply">
        <el-table :data="replies" v-loading="replyLoading" class="table">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="topicId" label="主题ID" width="80" />
          <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="danger" @click="delReply(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination v-model:current-page="replyPage" :page-size="10" :total="replyTotal" layout="prev, pager, next" @current-change="loadReplies" />
        </div>
      </el-tab-pane>
    </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

// 管理后台 - 论坛管理（路由：/admin/forum）
// - 主题列表：GET /admin/forum/topic/list
// - 回复列表：GET /admin/forum/reply/list
// - 删除主题/回复：DELETE 对应资源（见本文件的 request 调用）
const activeTab = ref('topic')
const topics = ref([])
const topicLoading = ref(false)
const topicPage = ref(1)
const topicTotal = ref(0)
const replies = ref([])
const replyLoading = ref(false)
const replyPage = ref(1)
const replyTotal = ref(0)

async function loadTopics() {
  topicLoading.value = true
  try {
    const res = await request.get('/admin/forum/topic/list', { params: { current: topicPage.value, size: 10 } })
    topics.value = (res.records || []).map(r => ({ ...r, createTime: r.createTime ? new Date(r.createTime).toLocaleString('zh-CN') : '' }))
    topicTotal.value = res.total || 0
  } finally { topicLoading.value = false }
}
async function loadReplies() {
  replyLoading.value = true
  try {
    const res = await request.get('/admin/forum/reply/list', { params: { current: replyPage.value, size: 10 } })
    replies.value = (res.records || []).map(r => ({ ...r, createTime: r.createTime ? new Date(r.createTime).toLocaleString('zh-CN') : '' }))
    replyTotal.value = res.total || 0
  } finally { replyLoading.value = false }
}
async function setTop(id, topStatus) {
  await request.put('/admin/forum/topic/top/' + id, null, { params: { topStatus } })
  ElMessage.success('操作成功')
  loadTopics()
}
async function closeTopic(id) {
  await request.put('/admin/forum/topic/close/' + id)
  ElMessage.success('已关闭')
  loadTopics()
}
async function delTopic(id) {
  await ElMessageBox.confirm('确定删除主题？')
  await request.delete('/admin/forum/topic/' + id)
  ElMessage.success('已删除')
  loadTopics()
}
async function delReply(id) {
  await ElMessageBox.confirm('确定删除回复？')
  await request.delete('/admin/forum/reply/' + id)
  ElMessage.success('已删除')
  loadReplies()
}
onMounted(() => { loadTopics(); loadReplies() })
</script>

<style lang="scss" scoped>
.tabs :deep(.el-tabs__header) {
  margin: 0 0 12px;
}

.table {
  width: 100%;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}
</style>
