<template>
  <el-container class="admin-layout">
    <el-aside width="248px" class="aside">
      <div class="aside-top">
        <div class="aside-badge">SANGUO</div>
        <div class="aside-title">管理后台</div>
        <div class="aside-sub">三国文化站点运营</div>
      </div>

      <div class="menu-wrap">
        <el-menu router :default-active="$route.path" class="menu" :collapse-transition="false" theme="dark">
          <el-menu-item v-if="isSuperAdmin || isAdmin" index="/admin">数据概览</el-menu-item>
          <el-menu-item v-if="isSuperAdmin || isAdmin" index="/admin/user">用户管理</el-menu-item>
          <el-menu-item v-if="canManageContent || isAdmin" index="/admin/person">人物管理</el-menu-item>
          <el-menu-item v-if="canManageContent || isAdmin" index="/admin/event">事件管理</el-menu-item>
          <el-menu-item v-if="canManageContent || isAdmin" index="/admin/literature">史料管理</el-menu-item>
          <el-menu-item v-if="canManageContent || isAdmin" index="/admin/allusion">典故管理</el-menu-item>
          <el-menu-item v-if="canManageReview || isAdmin" index="/admin/article">文章审核</el-menu-item>
          <el-menu-item v-if="canManageReview || isAdmin" index="/admin/comment">评论管理</el-menu-item>
          <el-menu-item v-if="canManageForum" index="/admin/forum">论坛管理</el-menu-item>
          <el-menu-item v-if="isSuperAdmin || isAdmin" index="/admin/config">系统设置</el-menu-item>
        </el-menu>
      </div>

      <div class="aside-bottom">
        <el-button class="back-btn" @click="$router.push('/')">返回前台</el-button>
      </div>
    </el-aside>
    <el-main class="admin-main">
      <div class="admin-topbar" v-if="canManageReview">
        <div class="topbar-title">{{ pageTitle }}</div>
        <div class="topbar-right">
          <el-badge :value="notifCount" :hidden="notifCount === 0" :max="99" class="notif-badge">
            <el-popover placement="bottom-end" :width="340" trigger="click" v-model:visible="notifVisible">
              <template #reference>
                <el-button class="notif-btn" circle>
                  <el-icon size="20"><Bell /></el-icon>
                </el-button>
              </template>
              <div class="notif-panel">
                <div class="notif-header">
                  <span>审核通知</span>
                  <el-button link type="primary" size="small" @click="markAllRead" v-if="notifCount > 0">全部已读</el-button>
                </div>
                <div class="notif-list" v-if="notifList.length">
                  <div v-for="n in notifList" :key="n.id" class="notif-item" @click="goNotif(n)">
                    <div class="notif-type">{{ notifTypeName(n.type) }}</div>
                    <div class="notif-content">{{ n.content }}</div>
                    <div class="notif-time">{{ formatTime(n.createTime) }}</div>
                  </div>
                </div>
                <div class="notif-empty" v-else>暂无新通知</div>
              </div>
            </el-popover>
          </el-badge>
          <el-button link @click="handleLogout">退出登录</el-button>
        </div>
      </div>
      <div class="admin-inner">
        <router-view :key="$route.fullPath" />
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useNotificationStore } from '../stores/notification'
import { Bell } from '@element-plus/icons-vue'
import { notificationUnread, notificationRead, notificationReadAll } from '../utils/notification'

const router = useRouter()
const store = useUserStore()
store.initFromStorage()
const notifStore = useNotificationStore()

const roles = computed(() => store.user?.roles || [])
const hasRole = (r) => roles.value.includes(r)
const isSuperAdmin = computed(() => hasRole('ROLE_SUPER_ADMIN'))
const isAdmin = computed(() => hasRole('ROLE_ADMIN'))
const isContentAdmin = computed(() => hasRole('ROLE_CONTENT_ADMIN'))
const isReviewAdmin = computed(() => hasRole('ROLE_REVIEW_ADMIN'))
const canManageUser = computed(() => isSuperAdmin.value)
const canManageContent = computed(() => isSuperAdmin.value || isContentAdmin.value)
const canManageReview = computed(() => isSuperAdmin.value || isReviewAdmin.value)
const canManageForum = computed(() => isSuperAdmin.value || isReviewAdmin.value || isAdmin.value)
const canAccessAdmin = computed(() => isSuperAdmin.value || isAdmin.value || isContentAdmin.value || isReviewAdmin.value)

const notifCount = computed(() => notifStore.count)
const notifList = ref([])
const notifVisible = ref(false)

const pageTitle = computed(() => {
  const map = {
    '/admin': '数据概览', '/admin/user': '用户管理', '/admin/person': '人物管理',
    '/admin/event': '事件管理', '/admin/literature': '史料管理', '/admin/allusion': '典故管理',
    '/admin/article': '文章审核', '/admin/comment': '评论管理', '/admin/forum': '论坛管理',
    '/admin/config': '系统设置'
  }
  return map[router.currentRoute.value.path] || '管理后台'
})

const notifTypeName = (type) => {
  const map = { article_submitted: '文章提交', forum_topic_submitted: '论坛发帖' }
  return map[type] || '通知'
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN')
}

async function loadNotif() {
  if (!canManageReview.value) return
  try {
    const res = await notificationUnread()
    notifList.value = res || []
  } catch {}
}

function goNotif(n) {
  if (n.link) router.push(n.link)
  if (n.id) notificationRead(n.id)
  notifVisible.value = false
}

async function markAllRead() {
  await notificationReadAll()
  notifList.value = []
  notifStore.refreshCount()
}

const notifListener = ({ type, content, link }) => {
  loadNotif()
  notifStore.refreshCount()
}

function handleLogout() {
  store.logout()
  router.push('/login')
}

onMounted(async () => {
  await loadNotif()
  await notifStore.refreshCount()
  notifStore.connectSse()
  notifStore.addListener(notifListener)
})

onUnmounted(() => {
  notifStore.removeListener(notifListener)
  notifStore.disconnectSse()
})
</script>

<style lang="scss" scoped>
.admin-layout { min-height: 100vh; background: var(--sanguo-bg); }
.aside {
  padding: 14px 12px 12px;
  background: linear-gradient(180deg, #8a0000 0%, #b30000 30%, #6f0000 100%);
  border-right: 1px solid rgba(0, 0, 0, 0.14);
  display: flex;
  flex-direction: column;
}

.aside-top {
  padding: 10px 10px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.10);
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.18);
}

.aside-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.86);
  font-weight: 900;
  letter-spacing: 1px;
  font-size: 12px;
}

.aside-title {
  color: rgba(255, 255, 255, 0.96);
  font-weight: 900;
  font-size: 18px;
  letter-spacing: 1px;
  margin-top: 10px;
}

.aside-sub {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
}

.menu-wrap {
  flex: 1;
  margin-top: 12px;
  border-radius: 14px;
  background: rgba(0, 0, 0, 0.10);
  border: 1px solid rgba(255, 255, 255, 0.10);
  overflow: hidden;
}

.menu {
  border: none;
  background: transparent;
  padding: 8px 6px;
}

.menu :deep(.el-menu-item) {
  border-radius: 12px;
  margin: 5px 6px;
  height: 46px;
  line-height: 46px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 700;
  position: relative;
  transition: transform 0.16s ease, background 0.16s ease, color 0.16s ease;
}

.menu :deep(.el-menu-item::before) {
  content: '';
  position: absolute;
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0);
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.10);
  transform: translateX(2px);
}

.menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.26);
  color: #ffffff !important;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.35);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.18);
}

.menu :deep(.el-menu-item.is-active::before) {
  background: rgba(255, 229, 170, 0.95);
}

.aside-bottom {
  padding: 12px 2px 0;
}

.back-btn {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.92);
  background: rgba(0, 0, 0, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.back-btn:hover {
  background: rgba(0, 0, 0, 0.22);
}

.admin-main { padding: 0; }

.admin-inner {
  padding: 22px;
}

.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 22px;
  background: #fff;
  border-bottom: 1px solid var(--sanguo-border);
  position: sticky;
  top: 0;
  z-index: 10;
}

.topbar-title {
  font-weight: 700;
  font-size: 16px;
  color: var(--sanguo-text);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notif-badge :deep(.el-badge__content) {
  background: #d32f2f;
}

.notif-btn {
  border: 1px solid var(--sanguo-border);
}

.notif-panel {
  max-height: 400px;
  overflow-y: auto;
}

.notif-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 4px 10px;
  border-bottom: 1px solid var(--sanguo-border);
  font-weight: 700;
  font-size: 14px;
}

.notif-list {
  padding: 4px 0;
}

.notif-item {
  padding: 10px 8px;
  cursor: pointer;
  border-bottom: 1px solid #f0ede8;
  transition: background 0.15s;
}

.notif-item:hover {
  background: #faf8f5;
}

.notif-item:last-child {
  border-bottom: none;
}

.notif-type {
  font-size: 11px;
  color: #d32f2f;
  font-weight: 700;
  margin-bottom: 3px;
}

.notif-content {
  font-size: 13px;
  color: var(--sanguo-text);
  line-height: 1.4;
  margin-bottom: 4px;
}

.notif-time {
  font-size: 11px;
  color: var(--sanguo-text-secondary);
}

.notif-empty {
  text-align: center;
  padding: 30px 0;
  color: var(--sanguo-text-secondary);
  font-size: 13px;
}
</style>
