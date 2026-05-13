<template>
  <div class="resource-page">
    <div class="sg-page-title">
      <div>
        <h1>资源中心</h1>
        
      </div>
    </div>

    <el-card class="sg-card filter-card">
      <el-tabs v-model="activeTab" class="resource-tabs" @tab-change="loadList">
        <el-tab-pane label="人物" name="person" />
        <el-tab-pane label="事件" name="event" />
        <el-tab-pane label="史料" name="literature" />
        <el-tab-pane label="典故" name="allusion" />
      </el-tabs>
      <div class="sg-toolbar" style="margin-top: 10px">
        <el-input v-model="keyword" placeholder="关键词" clearable class="kw" @keyup.enter="loadList" />
        <el-select v-if="activeTab==='person'||activeTab==='event'" v-model="dynasty" placeholder="朝代" clearable class="dyn">
          <el-option label="魏国" value="魏国" />
          <el-option label="蜀国" value="蜀国" />
          <el-option label="吴国" value="吴国" />
        </el-select>
        <el-button type="primary" @click="loadList">搜索</el-button>
        <div class="sg-muted meta">共 {{ total }} 条</div>
      </div>
    </el-card>

    <div class="list-wrap" v-loading="loading">
      <el-row :gutter="16">
        <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="item in list" :key="item.id">
          <el-card shadow="never" class="sg-card is-hover item-card" @click="goDetail(item.id)">
            <div class="item-media">
              <img v-if="item.imageUrl" :src="item.imageUrl" class="item-img" />
              <div v-else class="item-placeholder">{{ tabLabel }}</div>
              <div class="item-gradient" />
              <div class="item-badge" v-if="item.dynasty">{{ item.dynasty }}</div>
            </div>
            <div class="item-body">
              <div class="item-title">{{ item.name || item.title }}</div>
              <div class="item-meta">{{ item.author || item.source || (item.alias ? ('别名：' + item.alias) : '') }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div class="pager">
        <el-pagination
          v-model:current-page="current"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPersonList, getEventList, getLiteratureList, getAllusionList } from '../api/resource'

// 资源中心（路由：/resource）
// - 用 el-tabs 切换：人物/事件/史料/典故
// - 搜索条件：keyword +（人物/事件才有）dynasty
// - 数据来源：统一封装在 api/resource.js（getPersonList/getEventList/...）
// - 跳转详情：/resource/{type}/:id（见 router/index.js）
const router = useRouter()
const route = useRoute()

const activeTab = ref('person')
const keyword = ref('')
const dynasty = ref('')
const list = ref([])
const loading = ref(false)
const current = ref(1)
const size = ref(12)
const total = ref(0)

const tabLabel = computed(() => ({ person: '人物', event: '事件', literature: '史料', allusion: '典故' }[activeTab.value]))

const apis = {
  person: getPersonList,
  event: getEventList,
  literature: getLiteratureList,
  allusion: getAllusionList
}

async function loadList() {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value, keyword: keyword.value || undefined }
    if (dynasty.value && (activeTab.value === 'person' || activeTab.value === 'event')) params.dynasty = dynasty.value
    const res = await apis[activeTab.value](params)
    list.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/resource/${activeTab.value}/${id}`)
}

watch(activeTab, () => { current.value = 1; loadList() })

watch(
  () => route.query.keyword,
  v => {
    if (typeof v === 'string') {
      keyword.value = v
      current.value = 1
      loadList()
    }
  },
  { immediate: true }
)

watch(
  () => route.query.tab,
  v => {
    if (typeof v === 'string' && ['person', 'event', 'literature', 'allusion'].includes(v)) {
      activeTab.value = v
    }
  },
  { immediate: true }
)

loadList()
</script>

<style lang="scss" scoped>
.filter-card {
  margin-bottom: 16px;
}

.kw {
  width: 280px;
}

.dyn {
  width: 160px;
}

.meta {
  margin-left: auto;
}

.item-card {
  cursor: pointer;
  overflow: hidden;
}

.item-media {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
  background: #f4f0e8;
}

.item-img,
.item-placeholder {
  width: 100%;
  height: 100px;
  display: block;
}

.item-img {
  object-fit: contain;
  background: #f4f0e8;
}

.item-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--sanguo-text-secondary);
  font-weight: 700;
  letter-spacing: 1px;
}

.item-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, transparent 40%, rgba(31, 35, 40, 0.5));
}

.item-badge {
  position: absolute;
  left: 10px;
  bottom: 10px;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(230, 224, 214, 0.9);
  border-radius: 999px;
  font-size: 12px;
  line-height: 12px;
  color: var(--sanguo-text);
}

.item-body {
  padding: 12px 2px 4px;
}

.item-title {
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  margin-top: 6px;
  font-size: 12px;
  line-height: 18px;
  color: var(--sanguo-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pager {
  margin-top: 18px;
  display: flex;
  justify-content: center;
}

@media (max-width: 520px) {
  .kw {
    width: 100%;
  }
  .dyn {
    width: 100%;
  }
  .meta {
    width: 100%;
    margin-left: 0;
  }
}
</style>
