<template>
  <div class="login-page">
    <div class="login-card sg-card">
      <div class="brand">
        <div class="brand-mark">三国文化</div>
        <div class="brand-sub">登录后可收藏、评论并参与社区讨论</div>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名 / 手机号 / 邮箱" size="large" :disabled="loading" autocomplete="username" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password :disabled="loading" autocomplete="current-password" @keyup.enter="onSubmit" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onSubmit">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="links">
        <router-link :to="{ path: '/register', query: route.query }">没有账号？去注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useUserStore } from '../stores/user'

// 登录页（路由：/login）
// - 提交接口：POST /auth/login（支持：用户名/手机号/邮箱）
// - 登录成功后：保存 token，拉取 /auth/info，再根据 ?redirect 回跳
// - request.js 会统一处理后端 Result 格式与 401 场景
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const payload = {
    username: String(form.username || '').trim(),
    password: String(form.password || '').trim()
  }
  form.username = payload.username
  form.password = payload.password
  loading.value = true
  try {
    const data = await request.post('/auth/login', payload)
    userStore.setToken(data.token)
    await userStore.fetchInfo()
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    router.push(redirect || '/')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(900px 500px at 15% 0%, rgba(122, 31, 31, 0.12), transparent 55%),
    radial-gradient(900px 500px at 85% 10%, rgba(183, 121, 31, 0.10), transparent 55%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.55), rgba(255, 255, 255, 0));
}
.login-card {
  width: 400px;
  padding: 28px;
  background: rgba(255, 255, 255, 0.9);
}

.brand {
  text-align: center;
  margin-bottom: 18px;
}

.brand-mark {
  font-size: 22px;
  font-weight: 900;
  color: var(--sanguo-primary);
  letter-spacing: 1px;
}

.brand-sub {
  margin-top: 8px;
  color: var(--sanguo-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.links {
  text-align: center;
  margin-top: 14px;
}
</style>
