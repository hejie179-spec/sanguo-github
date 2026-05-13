<template>
  <div class="admin-article">
    <div class="sg-page-title">
      <div>
        <h1>文章审核</h1>
        <div class="sg-subtitle">对用户投稿进行通过/驳回/删除处理。</div>
      </div>
    </div>

    <el-card class="sg-card">
      <el-table :data="list" v-loading="loading" class="table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="authorName" label="作者" width="110" />
        <el-table-column prop="category" label="分类" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.category" effect="plain">{{ row.category }}</el-tag>
            <span v-else class="sg-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" effect="plain" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.status === 1" effect="plain" type="success">已通过</el-tag>
            <el-tag v-else effect="plain" type="danger">已驳回</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="primary" @click="approve(row.id)">通过</el-button>
            <el-button v-if="row.status === 0" link type="warning" @click="openReject(row)">驳回</el-button>
            <el-button link type="danger" @click="del(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="current" :page-size="size" :total="total" layout="prev, pager, next" @current-change="load" />
      </div>
    </el-card>
    <el-dialog v-model="rejectVisible" title="驳回原因" width="400px">
      <el-input v-model="rejectReason" type="textarea" placeholder="请输入驳回原因" :rows="3" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="reject">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

// 管理后台 - 文章审核（路由：/admin/article）
// - 列表：GET /admin/article/list（通常包含待审核/已通过/已驳回）
// - 审核通过：PUT /admin/article/approve/:id
// - 驳回：PUT /admin/article/reject/:id（带 reason）
const list = ref([])
const loading = ref(false)
const current = ref(1)
const size = ref(10)
const total = ref(0)
const rejectVisible = ref(false)
const rejectId = ref(null)
const rejectReason = ref('')

async function load() {
  loading.value = true
  try {
    const res = await request.get('/admin/article/list', { params: { current: current.value, size: size.value } })
    list.value = (res.records || []).map(r => ({ ...r, createTime: r.createTime ? new Date(r.createTime).toLocaleString('zh-CN') : '' }))
    total.value = res.total || 0
  } finally { loading.value = false }
}
async function approve(id) {
  await request.put('/admin/article/approve/' + id)
  ElMessage.success('已通过')
  load()
}
function openReject(row) {
  rejectId.value = row.id
  rejectReason.value = ''
  rejectVisible.value = true
}
async function reject() {
  await request.put('/admin/article/reject/' + rejectId.value, { reason: rejectReason.value })
  ElMessage.success('已驳回')
  rejectVisible.value = false
  load()
}
async function del(id) {
  await ElMessageBox.confirm('确定删除？')
  await request.delete('/admin/article/' + id)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>

<style lang="scss" scoped>
.table {
  width: 100%;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}
</style>
