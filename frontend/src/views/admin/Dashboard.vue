<template>
  <div class="dashboard">
    <div class="sg-page-title">
      <div>
        <h1>数据概览</h1>
        <div class="sg-subtitle">快速查看站点关键指标与待处理事项。</div>
      </div>
    </div>
    <el-row :gutter="20">
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="sg-card is-hover stat">
          <div class="stat-num">{{ stats.userCount || 0 }}</div>
          <div class="stat-label">用户数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="sg-card is-hover stat">
          <div class="stat-num">{{ stats.articleCount || 0 }}</div>
          <div class="stat-label">文章数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="sg-card is-hover stat">
          <div class="stat-num">{{ stats.articlePendingCount || 0 }}</div>
          <div class="stat-label">待审核文章</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="sg-card is-hover stat">
          <div class="stat-num">{{ (stats.personCount||0)+(stats.eventCount||0)+(stats.literatureCount||0)+(stats.allusionCount||0) }}</div>
          <div class="stat-label">资源总数</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'

// 管理后台 - 数据概览（路由：/admin）
// - 数据来源：GET /admin/stats/overview
// - 该页只做展示，不做编辑操作
const stats = ref({})
onMounted(async () => {
  try {
    stats.value = await request.get('/admin/stats/overview')
  } catch {}
})
</script>

<style lang="scss" scoped>
.stat {
  cursor: default;
}

.stat-num { font-size: 28px; font-weight: 700; color: var(--sanguo-primary); }
.stat-label { font-size: 14px; color: var(--sanguo-text-secondary); margin-top: 4px; }
</style>
