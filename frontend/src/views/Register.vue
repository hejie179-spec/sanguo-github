<template>
  <div class="register-page">
    <div class="register-card sg-card">
      <div class="brand">
        <div class="brand-mark">创建账号</div>
        <div class="brand-sub">注册后可收藏、评论、发布文章与参与论坛讨论</div>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" :disabled="loading" autocomplete="username" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" size="large" show-password :disabled="loading" autocomplete="new-password" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" size="large" show-password :disabled="loading" autocomplete="new-password" @keyup.enter="onSubmit" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号（选填）" size="large" :disabled="loading" autocomplete="tel" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱（选填）" size="large" :disabled="loading" autocomplete="email" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onSubmit">注册</el-button>
        </el-form-item>
      </el-form>
      <div class="links"><router-link :to="{ path: '/login', query: route.query }">已有账号？去登录</router-link></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useUserStore } from '../stores/user'

// 注册页（路由：/register）
// - 提交接口：POST /auth/register
// - 注册成功后：保存 token，拉取 /auth/info，再根据 ?redirect 回跳
// - confirmPassword 只用于前端校验，不会提交给后端
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '', confirmPassword: '', phone: '', email: '' })

const phonePattern = /^1\d{10}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '长度 2-50', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度 6-20', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (String(value || '') !== String(form.password || '')) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  phone: [
    {
      validator: (rule, value, callback) => {
        const v = String(value || '').trim()
        if (!v) callback()
        else if (!phonePattern.test(v)) callback(new Error('手机号格式不正确'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  email: [
    {
      validator: (rule, value, callback) => {
        const v = String(value || '').trim()
        if (!v) callback()
        else if (!emailPattern.test(v)) callback(new Error('邮箱格式不正确'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function onSubmit() {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const payload = {
    username: String(form.username || '').trim(),
    password: String(form.password || '').trim(),
    phone: String(form.phone || '').trim(),
    email: String(form.email || '').trim()
  }
  form.username = payload.username
  form.password = payload.password
  form.phone = payload.phone
  form.email = payload.email
  loading.value = true
  try {
    const data = await request.post('/auth/register', payload)
    userStore.setToken(data.token)
    await userStore.fetchInfo()
    ElMessage.success('注册成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    router.push(redirect || '/')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.register-page {
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
.register-card {
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
