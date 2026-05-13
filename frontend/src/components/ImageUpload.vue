<template>
  <div class="image-upload">
    <el-upload
      :action="action"
      :headers="headers"
      :show-file-list="false"
      :limit="1"
      accept="image/*"
      :on-success="onSuccess"
      :before-upload="beforeUpload"
    >
      <el-button type="primary">{{ modelValue ? '重新上传' : '上传图片' }}</el-button>
    </el-upload>
    <div v-if="modelValue" class="preview">
      <el-image :src="modelValue" style="width: 160px; height: 90px" fit="contain" />
      <el-button link type="danger" @click="$emit('update:modelValue', '')">移除</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

// 通用图片上传组件（v-model 绑定图片 URL）
// - 默认上传接口：POST /api/file/upload-image（ElementPlus upload 会直接发 multipart）
// - 自动带 Authorization 头：需要登录才可上传
// - 上传成功后把后端返回的 url 回填到 v-model
defineProps({
  modelValue: { type: String, default: '' },
  action: { type: String, default: '/api/file/upload-image' }
})

const emit = defineEmits(['update:modelValue'])
const userStore = useUserStore()

const headers = computed(() => {
  const token = userStore.token
  if (!token) return {}
  return { Authorization: 'Bearer ' + token }
})

function beforeUpload(file) {
  const okType = file.type?.startsWith('image/')
  const okSize = file.size <= 10 * 1024 * 1024
  if (!okType) ElMessage.error('请选择图片文件')
  if (!okSize) ElMessage.error('图片大小不能超过 10MB')
  return okType && okSize
}

function onSuccess(resp) {
  if (resp?.code !== 200) {
    ElMessage.error(resp?.msg || '上传失败')
    return
  }
  const url = resp?.data?.url
  if (!url) {
    ElMessage.error('上传失败')
    return
  }
  emit('update:modelValue', url)
  ElMessage.success('上传成功')
}
</script>

<style scoped>
.image-upload {
  display: flex;
  gap: 12px;
  align-items: center;
}
.preview {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
