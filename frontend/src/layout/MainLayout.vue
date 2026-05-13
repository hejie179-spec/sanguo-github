<template>
  <el-container class="main-layout">
    <el-header class="site-header">
      <div class="brand-row">
        <div class="sg-container brand-inner">
          <div class="brand-left" @click="$router.push('/')">
            <div class="brand-title">{{ siteName }}</div>
            
          </div>
          <div class="brand-right">
            <el-input
              v-model="headerKeyword"
              placeholder="请输入关键字！"
              clearable
              class="header-search"
              @keyup.enter="goSearch"
            />
            <el-button class="header-search-btn" @click="goSearch">搜索</el-button>
            <template v-if="userStore.token">
              <el-dropdown @command="handleCommand">
                <span class="user-name">{{ userStore.user?.username || '用户' }}</span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="center">个人中心</el-dropdown-item>
                    <el-dropdown-item command="admin" v-if="isAdmin">管理后台</el-dropdown-item>
                    <el-dropdown-item divided command="logout">退出</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <el-button type="primary" link @click="$router.push('/login')">登录</el-button>
              <el-button type="primary" @click="$router.push('/register')">注册</el-button>
            </template>
          </div>
        </div>
      </div>
      <div class="nav-row">
        <div class="nav-container">
          <el-menu mode="horizontal" :default-active="activeMenu" router class="nav-menu" :ellipsis="false">
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/resource">资源中心</el-menu-item>
            <el-menu-item index="/relationship">人物关系图谱</el-menu-item>
            <el-menu-item index="/map">事件时空地图</el-menu-item>
            <el-menu-item index="/article">文章</el-menu-item>
            <el-menu-item index="/ai">AI 助手</el-menu-item>
            <el-menu-item index="/forum">论坛</el-menu-item>
            <el-menu-item index="/center" v-if="userStore.token">个人中心</el-menu-item>
          </el-menu>
        </div>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <div class="sg-container sg-page">
            <component :is="Component" />
          </div>
        </transition>
      </router-view>
    </el-main>

    <el-footer class="footer">
      <div class="footer-bar">
        <div class="footer-text">{{ footerText }}</div>
      </div>
    </el-footer>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { resolveResourceTab } from '../utils/smartSearch'

// 前台主布局（几乎所有“前台页面”都挂在这里）
// - 顶部：网站标题 + 搜索框 + 登录态入口（个人中心/管理后台/退出）
// - 菜单：el-menu-item index=“/xxx” 决定点击后跳转到哪个路由
// - 内容：router-view 渲染当前子页面（Home/Resource/Map/Forum...）
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path.split('/').slice(0, 2).join('/') || '/')
const isAdmin = computed(() =>
  userStore.user?.roles?.some(r =>
    r === 'ROLE_SUPER_ADMIN' || r === 'ROLE_CONTENT_ADMIN' || r === 'ROLE_REVIEW_ADMIN' || r === 'ROLE_ADMIN'
  )
)

const headerKeyword = ref('')

const siteName = ref('三国文化主题网站')
const copyright = ref('')

const footerText = computed(() => {
  const year = new Date().getFullYear()
  const name = siteName.value || '三国文化主题网站'
  if (copyright.value && String(copyright.value).trim()) {
    return `${name} © ${year} ${copyright.value}`
  }
  return `${name} © ${year} 版权所有`
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/')
  } else if (cmd === 'center') router.push('/center')
  else if (cmd === 'admin') router.push('/admin')
}

async function goSearch() {
  const kw = headerKeyword.value?.trim()
  if (!kw) {
    router.push({ path: '/resource' })
    return
  }
  try {
    const tab = await resolveResourceTab(kw)
    if (!tab) {
      ElMessage.warning('不存在该资源')
      return
    }
    router.push({ path: '/resource', query: { tab, keyword: kw } })
  } catch {
    router.push({ path: '/resource', query: { keyword: kw } })
  }
}

onMounted(async () => {
  if (userStore.token && !userStore.user) await userStore.fetchInfo()
  try {
    const data = await request.get('/config/site')
    if (data.site_name) siteName.value = data.site_name
    if (data.copyright) copyright.value = data.copyright
  } catch {}
})
</script>

<style lang="scss" scoped>
.main-layout {
  min-height: 100vh;
  background: var(--sanguo-bg);
}
.site-header { height: auto; padding: 0; position: sticky; top: 0; z-index: 50; background: #fff; }
.brand-row { border-bottom: 1px solid var(--sanguo-border); background: #b9b8b8; }
.brand-inner { height: 84px; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 0; }
.brand-left { cursor: pointer; float: 1; }
.brand-title { font-size: 30px; font-weight: 700; color: #1f2328; }

.nav-row { background: #b9b8b8; height: 52px;  }

.nav-menu {
  height: 52px;
  border: none;
  background: transparent;
  padding: 0 5%;
  display: flex;
  justify-content: flex-start;
}

.nav-menu :deep(.el-menu-item) {
  color: rgba(254, 251, 251, 0.95);
  height: 52px; 
  line-height: 52px;
  font-size: 16px;
  font-family: "Microsoft Yahei", "思源黑体", sans-serif;
  padding: 0 58px;
  margin: 0;
}
.nav-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.18);
}
.nav-menu :deep(.el-menu-item.is-active) {
  color: #fe0303 !important;
  background: rgba(255, 255, 255, 0.24) !important;
}

.brand-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-search { width: 320px; }
.header-search :deep(.el-input__wrapper) {
  border-radius: 0;
  box-shadow: none;
  border: 1px solid #ddd;
}
.header-search-btn {
  border-radius: 0;
  height: 32px;
  background: #900;
  color: #fff;
  border: none;
}
.header-search-btn:hover{
  background: #b30000;
}

.user-name {
  cursor: pointer;
  color: #333;
  font-weight: 600;
  font-size: 16px;
}
.main-content {
  padding: 0;
}
.footer {
  height: auto;
  padding: 0;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  background: transparent;
}

.footer-bar {
  height: 76px;
  background: linear-gradient(180deg, #2b241f 0%, #1f1a16 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.footer-text {
  color: rgba(255, 255, 255, 0.86);
  font-size: 14px;
  letter-spacing: 0.8px;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 980px) {
  .brand-sub { display: none; }
  .brand-inner { height: 66px; padding: 0 4%; }
  .brand-title { font-size: 20px; }
  .header-search {
    display: none;
  }
  .header-search-btn {
    display: none;
  }
  .nav-menu{
    padding: 0 4%;
  }
  .nav-menu:deep(.el-menu-item){
    padding: 0 12px;
    font-size: 14px;
  }
}
</style>
