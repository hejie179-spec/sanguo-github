<template>
  <div class="home">
    <div class="hero">
      <div class="hero-inner">
        <div class="hero-title">三国文化主题网站</div>
        <div class="hero-desc">整合人物、事件、史料、典故，构建面向学习与研究的数字化三国文化知识门户。</div>
        <div class="hero-search">
          <div class="hero-search-row">
            <el-input v-model="heroKeyword" placeholder="请输入关键字!" class="hero-input" size="large" @keyup.enter="goSearch" />
            <el-button type="primary" class="hero-btn" size="large" @click="goSearch">确定</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 首页轮播图模块 -->
    <div class="carousel-section">
      <el-carousel :interval="4000" type="card" height="300px">
        <el-carousel-item v-for="(item, index) in carouselItems" :key="index">
          <div class="carousel-item-content">
            <img class="carousel-img" :src="item.imageUrl" :alt="item.title" />
            <div class="carousel-overlay">
              <h3>{{ item.title }}</h3>
              <p v-if="item.desc">{{ item.desc }}</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="quick-section">
      <div class="quick-head">快捷入口</div>
      <div class="quick-grid">
        <div class="quick-card" @click="$router.push({ path: '/resource', query: { tab: 'person' } })">人物</div>
        <div class="quick-card" @click="$router.push({ path: '/resource', query: { tab: 'event' } })">事件</div>
        <div class="quick-card" @click="$router.push({ path: '/resource', query: { tab: 'literature' } })">史料</div>
        <div class="quick-card" @click="$router.push({ path: '/resource', query: { tab: 'allusion' } })">典故</div>
        <div class="quick-card" @click="$router.push('/article')">文章</div>
        <div class="quick-card" @click="$router.push('/forum')">论坛</div>
      </div>
    </div>

    <div class="recommend-section" v-if="recommendations.length">
      <div class="section-header">
        <h2 class="section-title">为你推荐</h2>
      </div>
      <div class="recommend-grid">
        <div class="recommend-item" v-for="item in recommendations" :key="item.type + item.id" @click="goRecommend(item)">
          <div class="rec-cover">
            <img v-if="item.imageUrl" :src="item.imageUrl" />
            <div v-else class="rec-ph">{{ recTypeLabel(item.type) }}</div>
          </div>
          <div class="rec-info">
            <div class="rec-name">{{ item.name || item.title }}</div>
            <div class="rec-dynasty" v-if="item.dynasty">{{ item.dynasty }}</div>
            <el-tag size="small" effect="plain">{{ recTypeLabel(item.type) }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <div class="section-wrapper">
      <div class="section-header">
        <h2 class="section-title">人物专题</h2>
        <span class="section-more" @click="$router.push({ path: '/resource', query: { tab: 'person' } })">查看更多</span>
      </div>
      <div class="person-grid">
        <div class="person-item" v-for="item in persons.slice(0,4)" :key="'p'+item.id" @click="$router.push('/resource/person/'+item.id)">
          <div class="person-cover">
            <img v-if="item.imageUrl" :src="item.imageUrl" />
            <div v-else class="person-ph">暂无图片</div>
            <div class="person-dynasty" v-if="item.dynasty">{{ item.dynasty }}</div>
          </div>
          <div class="person-info">
            <div class="person-name">{{ item.name }}</div>
            <div class="person-alias">{{ item.alias || '查看详情' }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="news-community-wrapper" v-if="articles.length || topics.length">
      <div class="news-col block" v-if="articles.length">
        <div class="section-header">
          <h2 class="section-title">文章资讯</h2>
          <span class="section-more" @click="$router.push('/article')">更多</span>
        </div>
        <div class="text-list">
          <div class="text-item" v-for="item in articles" :key="item.id" @click="$router.push('/article/'+item.id)">
            <div class="text-title">{{ item.title }}</div>
            <div class="text-date">{{ item.createTime.split(' ')[0] }}</div>
          </div>
        </div>
      </div>

      <div class="community-col block" v-if="topics.length">
        <div class="section-header">
          <h2 class="section-title">论坛交流</h2>
          <span class="section-more" @click="$router.push('/forum')">更多</span>
        </div>
        <div class="text-list">
          <div class="text-item" v-for="item in topics" :key="item.id" @click="$router.push('/forum/'+item.id)">
            <div class="text-title">{{ item.title }}</div>
            <div class="text-date">{{ item.createTime.split(' ')[0] }}</div>
          </div>
        </div>
      </div>

      <div class="stats-col block">
        <div class="section-header">
          <h2 class="section-title">资源分类统计</h2>
          <span class="section-more" @click="$router.push('/resource')">进入资源中心</span>
        </div>
        <div class="stats-list">
          <div class="stats-item"><span>人物</span><b>{{ counts.person }}</b></div>
          <div class="stats-item"><span>事件</span><b>{{ counts.event }}</b></div>
          <div class="stats-item"><span>史料</span><b>{{ counts.literature }}</b></div>
          <div class="stats-item"><span>典故</span><b>{{ counts.allusion }}</b></div>
        </div>
      </div>
    </div>

    <div v-else class="empty-block">
      <div class="empty-title">暂无动态内容</div>
      <div class="empty-desc">当前文章与论坛数据为空，可先在后台审核通过文章或发布论坛主题后展示。</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { resolveResourceTab } from '../utils/smartSearch'
import { searchRecommend } from '../api/search'

// 首页（路由：/）
// - 顶部搜索：跳转到资源中心，并根据关键字智能选中人物/事件/史料/典故 tab
// - 首页内容：人物推荐 + 新闻动态（文章）+ 论坛主题 + 资源数量统计
// - 如果你想改首页展示哪些模块、模块顺序、请求哪些数据，就从这个文件入手
const router = useRouter()

const persons = ref([])
const articles = ref([])
const topics = ref([])
const heroKeyword = ref('')
const counts = ref({ person: 0, event: 0, literature: 0, allusion: 0 })
const recommendations = ref([])

const carouselImg1 = new URL('../assets/images/person/caocao.png', import.meta.url).href
const carouselImg2 = new URL('../assets/images/person/liubei.png', import.meta.url).href
const carouselImg3 = new URL('../assets/images/person/zhugeliang.png', import.meta.url).href
const carouselImg4 = new URL('../assets/images/person/sunquan.png', import.meta.url).href

const carouselItems = ref([
  { title: '曹操', desc: '治世之能臣，乱世之奸雄', imageUrl: carouselImg1 },
  { title: '刘备', desc: '以仁德得人心', imageUrl: carouselImg2 },
  { title: '诸葛亮', desc: '鞠躬尽瘁，死而后已', imageUrl: carouselImg3 },
  { title: '孙权', desc: '坐断东南，鼎立一方', imageUrl: carouselImg4 }
])

async function goSearch() {
  const kw = heroKeyword.value?.trim()
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

const typeRouteMap = {
  person: '/resource/person/',
  event: '/resource/event/',
  literature: '/resource/literature/',
  allusion: '/resource/allusion/',
  article: '/article/'
}

function recTypeLabel(type) {
  return { person: '人物', event: '事件', literature: '史料', allusion: '典故', article: '文章' }[type] || type
}

function goRecommend(item) {
  const base = typeRouteMap[item.type] || '/'
  router.push(base + item.id)
}

onMounted(async () => {
  try {
    const [p, a, t] = await Promise.all([
      request.get('/person/list', { params: { current: 1, size: 8 } }),
      request.get('/article/list', { params: { current: 1, size: 5 } }),
      request.get('/forum/topic/list', { params: { current: 1, size: 5 } })
    ])
    persons.value = p.records || []
    articles.value = (a.records || []).map(r => ({ ...r, createTime: formatTime(r.createTime) }))
    topics.value = (t.records || []).map(r => ({ ...r, createTime: formatTime(r.createTime) }))

    const [ep, el, ea] = await Promise.all([
      request.get('/event/list', { params: { current: 1, size: 1 } }),
      request.get('/literature/list', { params: { current: 1, size: 1 } }),
      request.get('/allusion/list', { params: { current: 1, size: 1 } })
    ])
    counts.value = {
      person: p.total || 0,
      event: ep.total || 0,
      literature: el.total || 0,
      allusion: ea.total || 0
    }
  } catch {}

  try {
    const rec = await searchRecommend(8)
    recommendations.value = rec || []
  } catch {}
})
function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleString('zh-CN')
}
</script>

<style lang="scss" scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.hero {
  background: linear-gradient(180deg, #9b9b9b 0%, #8f8f8f 100%);
  border-radius: 0;
  overflow: hidden;
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-inner {
  width: 100%;
  max-width: 980px;
  padding: 54px 24px;
  text-align: center;
}

.hero-title {
  color: rgba(255, 255, 255, 0.94);
  font-size: 44px;
  line-height: 1.2;
  font-weight: 700;
  letter-spacing: 3px;
  font-family: "Songti SC", "SimSun", "STSong", serif;
}

.hero-desc {
  margin-top: 14px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  line-height: 22px;
}

.hero-search {
  margin-top: 26px;
  display: flex;
  justify-content: center;
}

.hero-search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 720px;
  max-width: 100%;
}

.hero-input {
  flex: 1;
}

:deep(.hero-input .el-input__wrapper) {
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.hero-btn {
  height: 40px;
  min-width: 90px;
  border-radius: 10px;
  padding: 0 22px;
  font-weight: 700;
  color: #fff !important;
  background: var(--sanguo-primary) !important;
  border-color: var(--sanguo-primary) !important;
}

.carousel-section {
  width: 100%;
}

.carousel-item-content {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  position: relative;
}

.carousel-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f4f0e8;
  display: block;
}

.carousel-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18px 18px 14px;
  color: #fff;
  background: linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0.65) 100%);
  text-shadow: 1px 1px 4px rgba(0,0,0,0.5);
}

.carousel-overlay h3 {
  font-size: 28px;
  margin: 0 0 8px;
  letter-spacing: 2px;
}

.carousel-overlay p {
  font-size: 14px;
  opacity: 0.8;
  margin: 0;
}

.quick-section {
  background: #fff;
  border: 1px solid var(--sanguo-border);
  border-radius: var(--sanguo-radius);
  box-shadow: var(--sanguo-shadow);
  padding: 22px 22px 26px;
}

.quick-head {
  font-size: 20px;
  font-weight: 800;
  color: var(--sanguo-primary);
  margin-bottom: 16px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.quick-card {
  background: #f6f2ec;
  border-radius: 8px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #1f2328;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.quick-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.10);
}

.recommend-section {
  background: #fff;
  border-radius: var(--sanguo-radius);
  padding: 18px;
  box-shadow: var(--sanguo-shadow);
  border: 1px solid var(--sanguo-border);
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.recommend-item {
  cursor: pointer;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--sanguo-border);
  transition: transform 0.18s, box-shadow 0.18s;
}
.recommend-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--sanguo-shadow-hover);
}

.rec-cover {
  height: 120px;
  background: #e8e0d5;
  display: flex;
  align-items: center;
  justify-content: center;
}
.rec-cover img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f4f0e8;
}
.rec-ph {
  font-size: 13px;
  color: #999;
}
.rec-info {
  padding: 10px;
  background: #fff;
}
.rec-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rec-dynasty {
  font-size: 12px;
  color: var(--sanguo-text-secondary);
  margin-bottom: 4px;
}

.block {
  background: #fff;
  border-radius: var(--sanguo-radius);
  border: 1px solid var(--sanguo-border);
  padding: 18px 18px;
  box-shadow: var(--sanguo-shadow);
}

.section-wrapper {
  background: #fff;
  border-radius: var(--sanguo-radius);
  padding: 18px;
  box-shadow: var(--sanguo-shadow);
  border: 1px solid var(--sanguo-border);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--sanguo-primary);
}

.section-title {
  font-size: 20px;
  font-weight: 800;
  color: #1f2328;
  margin: 0;
}

.section-more {
  font-size: 13px;
  color: var(--sanguo-text-secondary);
  cursor: pointer;
}
.section-more:hover {
  color: var(--sanguo-primary);
}

/* 热门人物网格 */
.person-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }

.person-item {
  cursor: pointer;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  border: 1px solid var(--sanguo-border);
}

.person-item:hover {
  box-shadow: var(--sanguo-shadow-hover);
  transform: translateY(-2px);
  border-color: rgba(179, 0, 0, 0.35);
}

.person-cover {
  position: relative;
  height: 160px;
  background: #e5e5e5;
}

.person-cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0,0,0,0.02), rgba(0,0,0,0.35));
  pointer-events: none;
}

.person-cover img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f4f0e8;
}

.person-ph {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
}

.person-dynasty {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(179, 0, 0, 0.88);
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.person-info {
  padding: 12px;
  text-align: center;
}

.person-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--sanguo-text);
  margin-bottom: 4px;
}

.person-alias {
  font-size: 13px;
  color: var(--sanguo-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 资讯与社区双栏 */
.news-community-wrapper {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 18px;
}

.text-list {
  display: flex;
  flex-direction: column;
}

.text-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed var(--sanguo-border);
  cursor: pointer;
  transition: background 0.18s ease, transform 0.18s ease;
}

.text-item:last-child {
  border-bottom: none;
}

.text-item:hover .text-title {
  color: var(--sanguo-primary);
}

.text-item:hover {
  background: rgba(179, 0, 0, 0.03);
  transform: translateX(2px);
}

.text-title {
  font-size: 15px;
  color: var(--sanguo-text);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 16px;
}

.text-title::before {
  content: "•";
  color: var(--sanguo-primary);
  margin-right: 8px;
}

.text-date {
  font-size: 13px;
  color: var(--sanguo-text-secondary);
  flex-shrink: 0;
}

.empty-text {
  text-align: center;
  color: #999;
  padding: 20px 0;
  font-size: 14px;
}
.stats-list { display: flex; flex-direction: column; gap: 8px; }
.stats-item {
  height: 44px;
  border: 1px solid var(--sanguo-border);
  border-radius: 10px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}
.stats-item:hover {
  border-color: rgba(179, 0, 0, 0.35);
  box-shadow: 0 10px 22px rgba(0,0,0,0.06);
  transform: translateY(-1px);
}
.stats-item b { color: var(--sanguo-primary); font-size: 18px; }
.empty-block {
  border: 1px solid var(--sanguo-border);
  border-radius: 4px;
  background: #fff;
  padding: 28px 20px;
  text-align: center;
}
.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #444;
}
.empty-desc {
  margin-top: 8px;
  color: #888;
  font-size: 14px;
}

@media (max-width: 980px) {
  .person-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .news-community-wrapper {
    grid-template-columns: 1fr 1fr;
  }
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 520px) {
  .news-community-wrapper { grid-template-columns: 1fr; }
  .person-grid {
    grid-template-columns: 1fr;
  }
}
</style>
