import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

// 路由配置说明，就是告诉系统各个URL对应哪个页面
// /intro：开场页，没登录的用户访问首页时会先跳这里
// /login、/register：登录和注册页，这两个页面是给没登录的人用的
// / 下面的页面：网站的主要内容，比如首页、资源页、论坛等，都有顶部导航栏
// /admin 下面的页面：管理后台，只有管理员能进
const routes = [
  {
    path: '/intro',
    name: 'Intro',
    component: () => import('../views/Intro.vue'),
    meta: { guest: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'relationship', name: 'RelationshipGraph', component: () => import('../views/RelationshipGraph.vue') },
      { path: 'map', name: 'TimeSpaceMap', component: () => import('../views/TimeSpaceMap.vue') },
      { path: 'resource', name: 'Resource', component: () => import('../views/Resource.vue') },
      { path: 'resource/person/:id', name: 'PersonDetail', component: () => import('../views/ResourceDetail.vue'), props: { type: 'person' } },
      { path: 'resource/event/:id', name: 'EventDetail', component: () => import('../views/ResourceDetail.vue'), props: { type: 'event' } },
      { path: 'resource/literature/:id', name: 'LiteratureDetail', component: () => import('../views/ResourceDetail.vue'), props: { type: 'literature' } },
      { path: 'resource/allusion/:id', name: 'AllusionDetail', component: () => import('../views/ResourceDetail.vue'), props: { type: 'allusion' } },
      { path: 'article', name: 'ArticleList', component: () => import('../views/ArticleList.vue') },
      { path: 'article/:id', name: 'ArticleDetail', component: () => import('../views/ArticleDetail.vue') },
      { path: 'ai', name: 'Ai', component: () => import('../views/Ai.vue') },
      { path: 'forum', name: 'Forum', component: () => import('../views/Forum.vue') },
      { path: 'forum/:id', name: 'ForumDetail', component: () => import('../views/ForumDetail.vue') },
      { path: 'center', name: 'Center', component: () => import('../views/Center.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layout/AdminLayout.vue'),
    meta: { admin: true },
    children: [
      { path: '', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { roles: ['ROLE_SUPER_ADMIN', 'ROLE_ADMIN'] } },
      { path: 'user', name: 'AdminUser', component: () => import('../views/admin/User.vue'), meta: { roles: ['ROLE_SUPER_ADMIN', 'ROLE_ADMIN'] } },
      { path: 'person', name: 'AdminPerson', component: () => import('../views/admin/Resource.vue'), props: { type: 'person' }, meta: { roles: ['ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_CONTENT_ADMIN'] } },
      { path: 'event', name: 'AdminEvent', component: () => import('../views/admin/Resource.vue'), props: { type: 'event' }, meta: { roles: ['ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_CONTENT_ADMIN'] } },
      { path: 'literature', name: 'AdminLiterature', component: () => import('../views/admin/Resource.vue'), props: { type: 'literature' }, meta: { roles: ['ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_CONTENT_ADMIN'] } },
      { path: 'allusion', name: 'AdminAllusion', component: () => import('../views/admin/Resource.vue'), props: { type: 'allusion' }, meta: { roles: ['ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_CONTENT_ADMIN'] } },
      { path: 'article', name: 'AdminArticle', component: () => import('../views/admin/Article.vue'), meta: { roles: ['ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_REVIEW_ADMIN'] } },
      { path: 'comment', name: 'AdminComment', component: () => import('../views/admin/Comment.vue'), meta: { roles: ['ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_REVIEW_ADMIN'] } },
      { path: 'forum', name: 'AdminForum', component: () => import('../views/admin/Forum.vue'), meta: { roles: ['ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_REVIEW_ADMIN'] } },
      { path: 'config', name: 'AdminConfig', component: () => import('../views/admin/Config.vue'), meta: { roles: ['ROLE_SUPER_ADMIN', 'ROLE_ADMIN'] } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

// 全局前置守卫：所有页面跳转前执行权限检查
// 1. 首页未登录 → 跳转开场页
// 2. 已登录访问开场页 → 跳转首页
// 3. admin路由：未登录→登录页，登录但无任何管理员角色→首页
// 4. auth页面：已登录→首页
// 5. 个人中心：未登录→登录页
router.beforeEach((to, from, next) => {
  const store = useUserStore()
  store.initFromStorage()
  if (to.path === '/' && !store.token && String(to.query?.fromIntro || '') !== '1') {
    next('/intro')
    return
  }
  if (to.path === '/intro' && store.token) {
    next('/')
    return
  }
  if (to.meta.admin) {
    const roles = store.user?.roles || []
    const hasAnyAdminRole = roles.some(r =>
      r === 'ROLE_SUPER_ADMIN' || r === 'ROLE_CONTENT_ADMIN' || r === 'ROLE_REVIEW_ADMIN' || r === 'ROLE_ADMIN'
    )
    if (!store.token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
    if (!hasAnyAdminRole) {
      next('/')
      return
    }
    const required = to.meta.roles
    if (required && Array.isArray(required) && required.length) {
      const ok = roles.some(r => required.includes(r))
      if (!ok) {
        if (roles.includes('ROLE_CONTENT_ADMIN')) {
          next('/admin/person')
          return
        }
        if (roles.includes('ROLE_REVIEW_ADMIN')) {
          next('/admin/article')
          return
        }
        next('/admin')
        return
      }
    }
  }
  if (to.meta.guest && store.token && (to.path === '/login' || to.path === '/register' || to.path === '/intro')) {
    next('/')
    return
  }
  next()
})

export default router
