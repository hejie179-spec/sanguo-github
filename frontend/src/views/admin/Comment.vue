<template>
  <div class="admin-comment">
    <div class="sg-page-title">
      <div>
        <h1>评论管理</h1>
        <div class="sg-subtitle">支持按类型与状态筛选，对不当内容进行隐藏或永久删除。</div>
      </div>
    </div>

    <el-card class="sg-card">
    <div class="bar">
      <el-select v-model="filterStatus" placeholder="全部状态" class="sel" @change="load">
        <el-option label="全部状态" :value="null" />
        <el-option label="正常" :value="1" />
        <el-option label="隐藏" :value="0" />
      </el-select>
      <el-select v-model="filterType" placeholder="全部类型" class="sel" @change="load">
        <el-option label="全部类型" :value="null" />
        <el-option label="文章" :value="1" />
        <el-option label="人物" :value="2" />
        <el-option label="事件" :value="3" />
        <el-option label="史料" :value="4" />
        <el-option label="典故" :value="5" />
      </el-select>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="list" v-loading="loading" class="table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="targetType" label="类型" width="80">
        <template #default="{ row }">{{ { 1: '文章', 2: '人物', 3: '事件', 4: '史料', 5: '典故' }[row.targetType] }}</template>
      </el-table-column>
      <el-table-column prop="targetId" label="目标ID" width="80" />
      <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">{{ row.status === 0 ? '隐藏' : '正常' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="180" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button v-if="row.status !== 0" link type="danger" @click="del(row.id)">隐藏</el-button>
          <el-button v-if="row.status === 0" link type="danger" @click="purge(row.id)">永久删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination v-model:current-page="current" :page-size="size" :total="total" layout="prev, pager, next" @current-change="load" />
    </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

// 管理后台 - 评论管理（路由：/admin/comment）
// - 列表：GET /admin/comment/list（支持按状态/目标类型筛选）
// - 删除：DELETE /admin/comment/:id
// - 屏蔽/恢复：PUT /admin/comment/status/:id（或类似接口，见本文件 request 调用）
const list = ref([])
const loading = ref(false)
const current = ref(1)
const size = ref(10)
const total = ref(0)
const filterStatus = ref(null)
const filterType = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await request.get('/admin/comment/list', { params: { current: current.value, size: size.value, status: filterStatus.value, targetType: filterType.value } })
    list.value = (res.records || []).map(r => ({ ...r, createTime: r.createTime ? new Date(r.createTime).toLocaleString('zh-CN') : '' }))
    total.value = res.total || 0
  } finally { loading.value = false }
}
async function del(id) {
  await ElMessageBox.confirm('确定隐藏该评论及其所有回复？')
  await request.delete('/admin/comment/' + id)
  ElMessage.success('已隐藏')
  load()
}

async function purge(id) {
  await ElMessageBox.confirm('确定永久删除该评论及其所有回复？此操作不可恢复。')
  await request.delete('/admin/comment/purge/' + id)
  ElMessage.success('已永久删除')
  load()
}
onMounted(load)
</script>

<style lang="scss" scoped>
.bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.sel {
  width: 140px;
}

.table {
  width: 100%;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}

@media (max-width: 520px) {
  .sel {
    width: 100%;
  }
}
</style>
