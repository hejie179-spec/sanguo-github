<template>
  <div class="relationship-graph-page">
    <div class="topbar">
      <div class="topbar-title">
        <h2>三国人物关系图谱</h2>
        <p>支持节点拖拽、鼠标滚轮缩放、点击查看详情；可按阵营/关系筛选。</p>
      </div>

      <div class="topbar-filters">
        <el-form :inline="true" class="filter-form">
          <el-form-item label="阵营">
            <el-select v-model="filterCamp" placeholder="全部" clearable @change="updateGraph" class="filter-select">
              <el-option v-for="camp in campOptions" :key="camp" :label="camp" :value="camp" />
            </el-select>
          </el-form-item>
          <el-form-item label="关系">
            <el-select v-model="filterRelation" placeholder="全部" clearable @change="updateGraph" class="filter-select">
              <el-option v-for="rel in relationOptions" :key="rel" :label="rel" :value="rel" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="content-grid">
      <div class="graph-card">
        <div class="graph-toolbar">
          <div class="toolbar-left">
            <span class="toolbar-hint">提示：滚轮缩放，拖拽节点，点击节点查看详情</span>
          </div>
          <div class="toolbar-right">
            <el-button size="small" @click="resetView">重置视图</el-button>
            <el-button size="small" @click="toggleLabel">{{ showLabel ? '隐藏名字' : '显示名字' }}</el-button>
          </div>
        </div>
        <div class="graph-container" ref="chartRef"></div>
      </div>

      <div class="detail-panel" v-if="selectedNode">
        <div class="panel-header">
          <div class="panel-title">
            <div class="panel-name">{{ selectedNode.name }}</div>
            <el-tag size="small" :type="getCampTagType(selectedNode.category)">{{ categories?.[selectedNode.category]?.name || '未知阵营' }}</el-tag>
          </div>
          <el-button type="primary" link @click="selectedNode = null">关闭</el-button>
        </div>
        <div class="panel-body">
          <div class="panel-section">
            <div class="section-label">简介</div>
            <div class="desc">{{ selectedNode.desc || '暂无详细介绍' }}</div>
          </div>

          <div class="panel-section">
            <div class="section-label">相关人物关系</div>
            <ul class="relation-list" v-if="getRelatedLinks(selectedNode.name).length">
              <li v-for="(link, idx) in getRelatedLinks(selectedNode.name)" :key="idx">
                <span class="rel-name">{{ link.source === selectedNode.name ? link.target : link.source }}</span>
                <span class="rel-type">{{ link.value }}</span>
              </li>
            </ul>
            <div class="relation-empty" v-else>暂无关系数据</div>
          </div>
        </div>
      </div>

      <div class="detail-panel is-empty" v-else>
        <div class="panel-empty">
          <div class="panel-empty-title">点击人物查看详情</div>
          <div class="panel-empty-sub">右侧会展示阵营、简介与相关关系</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onBeforeUnmount, onMounted, markRaw } from 'vue'
import * as echarts from 'echarts'
import relationData from '../data/sanguo-relation.json'

// 人物关系图谱（路由：/relationship）
// - 数据来源：src/data/sanguo-relation.json（本地静态数据）
// - ECharts graph：支持拖拽/缩放/点击节点查看详情
// - 右侧详情面板：展示阵营、简介、与该节点有关的关系边
const chartRef = ref(null)
let myChart = null

const filterCamp = ref('')
const filterRelation = ref('')
const selectedNode = ref(null)
const showLabel = ref(true)

const categories = relationData.categories || []

const campOptions = computed(() => categories.map(c => c.name))

const relationOptions = computed(() => {
  const set = new Set((relationData.links || []).map(l => l.relation).filter(Boolean))
  return Array.from(set)
})

const getCampTagType = (catIdx) => {
  const name = categories?.[catIdx]?.name
  if (name === '魏国') return 'primary'
  if (name === '蜀国') return 'success'
  if (name === '吴国') return 'danger'
  return 'info'
}

const categoryIndexByName = new Map(categories.map((c, idx) => [c.name, idx]))

const encodePath = (p) => String(p || '').split('/').map(encodeURIComponent).join('/')

const getAvatarUrl = (node) => {
  const raw = node?.avatar
  if (raw && /^https?:\/\//i.test(raw)) return raw
  if (raw && String(raw).startsWith('/')) return raw
  if (raw && String(raw).includes('/')) return `/assets/images/person/${encodePath(raw)}`
  if (raw) return `/assets/images/person/${encodeURIComponent(raw)}`
  return `/assets/images/person/${encodeURIComponent(node?.name || '')}.png`
}

const rawNodes = (relationData.nodes || []).map(n => {
  const idx = categoryIndexByName.get(n.category)
  const avatarUrl = getAvatarUrl(n)
  return {
    id: n.name,
    name: n.name,
    category: typeof idx === 'number' ? idx : 0,
    symbol: avatarUrl ? `image://${avatarUrl}` : undefined,
    symbolSize: 52,
    itemStyle: avatarUrl
      ? { borderColor: 'rgba(255, 255, 255, 0.95)', borderWidth: 2 }
      : undefined,
    desc: n.desc
  }
})

const rawLinks = (relationData.links || []).map(l => ({
  source: l.source,
  target: l.target,
  value: l.relation
}))

const getRelatedLinks = (name) => {
  return rawLinks.filter(l => l.source === name || l.target === name)
}

const resetView = () => {
  if (!myChart) return
  myChart.setOption({
    series: [{
      center: ['50%', '50%'],
      zoom: 1
    }]
  })
}

const toggleLabel = () => {
  showLabel.value = !showLabel.value
  updateGraph()
}

const updateGraph = () => {
  if (!myChart) return

  // 筛选节点和连线
  let filteredNodes = [...rawNodes]
  let filteredLinks = [...rawLinks]

  if (filterCamp.value) {
    const campIdx = categories.findIndex(c => c.name === filterCamp.value)
    if (campIdx !== -1) {
      filteredNodes = rawNodes.filter(n => n.category === campIdx)
      const nodeNames = new Set(filteredNodes.map(n => n.name))
      filteredLinks = rawLinks.filter(l => nodeNames.has(l.source) && nodeNames.has(l.target))
    }
  }

  if (filterRelation.value) {
    filteredLinks = filteredLinks.filter(l => l.value === filterRelation.value)
    // 仅保留有关系的节点（若选择了关系类型）
    const relNodes = new Set()
    filteredLinks.forEach(l => {
      relNodes.add(l.source)
      relNodes.add(l.target)
    })
    filteredNodes = filteredNodes.filter(n => relNodes.has(n.name))
  }

  const baseRepulsion = Math.max(900, Math.min(1800, filteredNodes.length * 45))
  const option = {
    tooltip: {
      formatter: function (params) {
        if (params.dataType === 'node') {
          const campName = categories?.[params.data.category]?.name || '未知'
          return `${params.data.name}<br/>阵营: ${campName}`
        } else {
          return `${params.data.source} - ${params.data.target}<br/>关系: ${params.data.value}`
        }
      }
    },
    legend: [{
      data: categories.map(a => a.name),
      top: 10,
      left: 12
    }],
    color: ['#1f77b4', '#2ca02c', '#d62728', '#9467bd', '#8c564b'],
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: filteredNodes,
        links: filteredLinks,
        categories: categories,
        roam: true,
        draggable: true,
        center: ['50%', '52%'],
        zoom: 1,
        scaleLimit: { min: 0.4, max: 2.5 },
        nodeScaleRatio: 0.6,
        label: {
          show: showLabel.value,
          position: 'right',
          formatter: '{b}',
          fontSize: 12,
          color: '#1f2328'
        },
        labelLayout: {
          hideOverlap: true
        },
        force: {
          repulsion: baseRepulsion,
          edgeLength: [90, 160],
          gravity: 0.12
        },
        lineStyle: {
          color: 'source',
          curveness: 0.18,
          width: 1.3,
          opacity: 0.85
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: {
            width: 4,
            opacity: 1
          },
          label: {
            fontWeight: 700
          }
        },
        animationDurationUpdate: 300
      }
    ]
  }

  myChart.setOption(option)
}

const resetFilter = () => {
  filterCamp.value = ''
  filterRelation.value = ''
  selectedNode.value = null
  updateGraph()
}

onMounted(() => {
  myChart = markRaw(echarts.init(chartRef.value))
  
  // 监听节点点击事件
  myChart.on('click', (params) => {
    if (params.dataType === 'node') {
      selectedNode.value = params.data
    }
  })

  updateGraph()
  resetView()

  window.addEventListener('resize', handleResize)
})

const handleResize = () => {
  if (myChart) {
    myChart.resize()
  }
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (myChart) {
    myChart.dispose()
  }
})
</script>

<style scoped>
.relationship-graph-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
}


.topbar {
  background: #fff;
  border: 1px solid var(--sanguo-border);
  border-radius: 10px;
  box-shadow: var(--sanguo-shadow);
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.topbar-title h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--sanguo-text);
}

.topbar-title p {
  margin: 6px 0 0;
  color: var(--sanguo-text-secondary);
  font-size: 13px;
}

.topbar-filters {
  flex-shrink: 0;
}

.filter-form {
  margin-bottom: -18px;
}

.filter-select {
  width: 150px;
}

.content-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 16px;
  align-items: start;
  min-height: 620px;
}

.graph-card {
  background: #fff;
  border-radius: 10px;
  border: 1px solid var(--sanguo-border);
  box-shadow: var(--sanguo-shadow);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.graph-toolbar {
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 12px;
  border-bottom: 1px solid var(--sanguo-border);
  background: rgba(244, 240, 232, 0.55);
}

.toolbar-hint {
  color: var(--sanguo-text-secondary);
  font-size: 12px;
}

.graph-container {
  flex: 1;
  min-height: 560px;
}

.detail-panel {
  background: #fff;
  border-radius: 10px;
  border: 1px solid var(--sanguo-border);
  box-shadow: var(--sanguo-shadow);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 86px;
  max-height: calc(100vh - 110px);
}

.detail-panel.is-empty {
  display: flex;
}

.panel-header {
  padding: 12px 14px;
  border-bottom: 1px solid var(--sanguo-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.panel-name {
  font-size: 18px;
  font-weight: 800;
  color: var(--sanguo-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.panel-body {
  padding: 14px;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.panel-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--sanguo-text);
}

.desc {
  background: #f8f9fa;
  background: rgba(244, 240, 232, 0.55);
  border: 1px solid var(--sanguo-border);
  padding: 12px;
  border-radius: 10px;
  color: var(--sanguo-text-secondary);
  line-height: 1.7;
  font-size: 13px;
}

.relation-list {
  list-style: none;
  padding: 0;
  margin: 0;
  border: 1px solid var(--sanguo-border);
  border-radius: 10px;
  overflow: hidden;
}

.relation-list li {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(229, 229, 229, 0.9);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.relation-list li:last-child {
  border-bottom: none;
}
.rel-name {
  color: #303133;
  font-weight: 500;
}

.rel-type {
  color: #909399;
  color: var(--sanguo-text-secondary);
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(179, 0, 0, 0.06);
  border: 1px solid rgba(179, 0, 0, 0.12);
}

.relation-empty {
  color: var(--sanguo-text-secondary);
  font-size: 13px;
  padding: 10px 0;
}


.panel-empty {
  padding: 18px 16px;
}

.panel-empty-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--sanguo-text);
}

.panel-empty-sub {
  margin-top: 8px;
  color: var(--sanguo-text-secondary);
  font-size: 13px;
  line-height: 20px;
}

@media (max-width: 1100px) {
  .topbar {
    flex-direction: column;
    align-items: stretch;
  }
  .topbar-filters {
    width: 100%;
  }
  .content-grid {
    grid-template-columns: 1fr;
  }
  .detail-panel {
    position: static;
    max-height: none;
  }
  .filter-select {
    width: 160px;
  }
}
</style>
