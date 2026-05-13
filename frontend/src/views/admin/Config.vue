<template>
  <div class="admin-config">
    <div class="sg-page-title">
      <div>
        <h1>系统设置</h1>
        <div class="sg-subtitle">站点名称、版权等配置项维护。</div>
      </div>
    </div>

    <el-card class="sg-card">
      <el-form label-width="120px" class="form">
        <el-form-item v-for="c in configList" :key="c.configKey" :label="c.configKey">
          <el-input v-model="c.configValue" :placeholder="c.description" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

// 管理后台 - 系统设置（路由：/admin/config）
// - 读取配置：GET /admin/config/list
// - 保存配置：PUT /admin/config/save
// 前台 MainLayout 会读取 /config/site 来展示站点名称、版权等
const configList = ref([])

async function load() {
  configList.value = await request.get('/admin/config/list') || []
}
async function save() {
  await request.put('/admin/config/save', configList.value)
  ElMessage.success('保存成功')
}
onMounted(load)
</script>

<style lang="scss" scoped>
.form {
  max-width: 560px;
}
</style>
