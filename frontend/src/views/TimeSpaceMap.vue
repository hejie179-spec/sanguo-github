<template>
  <div class="time-space-map-page">
    <div class="header-section">
      <h2>事件时空地图</h2>
      <p>结合时间轴与地理位置，动态展示三国历史事件。点击左侧时间轴事件，右侧地图将高亮对应发生地。</p>
    </div>

    <div class="main-content">
      <!-- 左侧时间轴 -->
      <div class="timeline-panel">
        <div class="timeline-toolbar">
          <div class="toolbar-left">
            <span class="toolbar-title">时间轴</span>
            <el-tag v-if="selectedCityKey" type="warning" effect="light" class="city-tag">
              {{ selectedCityKey }}
            </el-tag>
          </div>
          <el-button v-if="selectedCityKey" size="small" @click="clearCityFilter">清除筛选</el-button>
        </div>
        <el-timeline>
          <el-timeline-item
            v-for="(event, index) in filteredEvents"
            :key="index"
            :timestamp="event.year"
            :type="activeEvent?.id === event.id ? 'primary' : 'info'"
            :hollow="activeEvent?.id !== event.id"
            :size="activeEvent?.id === event.id ? 'large' : 'normal'"
            @click="selectEvent(event)"
            class="timeline-item"
          >
            <div class="event-card" :class="{ active: activeEvent?.id === event.id }">
              <h4>{{ event.name }}</h4>
              <p class="location"><el-icon><Location /></el-icon> {{ event.locationText }}</p>
              <p class="desc">{{ event.desc }}</p>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-if="!filteredEvents.length" description="暂无该地点相关事件" />
      </div>

      <!-- 右侧地图区 (使用散点图模拟坐标展示) -->
      <div class="map-container">
        <div ref="mapChartRef" class="map-chart"></div>
        <div class="map-hint">点击地名可筛选时间轴</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, markRaw, computed, nextTick } from 'vue'
import { Location } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import timelineData from '../data/sanguo-timeline.json'

// 事件时空地图（路由：/map）
// - 左侧：时间轴（timelineData 来自本地 JSON，可替换为后端接口）
// - 右侧：ECharts 散点图“示意坐标系”（非真实地图），点击地名可筛选事件
// - cityCoords：地名→坐标（示意坐标系，非真实经纬度）
const mapChartRef = ref(null)
let myChart = null
let resizeObserver = null
const activeEvent = ref(null)
const selectedCityKey = ref('')

// 说明：这里用的是“示意坐标系”（非真实经纬度），目的是让点位更分散、更清晰。
// 坐标范围大致为 x:[0,100], y:[0,100]，数值越大不代表真实地理位置，只用于页面展示布局。
const cityCoords = {
  '全国': [8, 92],
  '关东': [78, 80],

  '洛阳': [55, 68],
  '洛阳高平陵': [58, 72],
  '高平陵': [58, 72],

  '长安': [34, 70],
  '许都': [63, 60],
  '许昌': [65, 58],
  '官渡': [71, 66],
  '宛城': [58, 54],
  '寿春': [76, 52],

  '隆中': [54, 46],
  '祁山': [41, 62],
  '街亭': [44, 56],
  '五丈原': [46, 66],

  '徐州': [86, 60],
  '建业': [90, 42],
  '赤壁': [78, 34],
  '夷陵': [70, 28],
  '白帝城': [57, 30],

  '成都': [18, 36],
  '南中': [10, 18],

  '乌桓': [92, 92]
}

const normalizeLocationKey = (locationText) => {
  const text = String(locationText || '').trim()
  if (!text) return '全国'
  const beforeParen = text.split('（')[0]
  const firstPart = beforeParen.split(/[、,，\s]/).filter(Boolean)[0]
  if (!firstPart) return '全国'
  if (firstPart.includes('全国')) return '全国'
  return firstPart
}

const historyEvents = (timelineData || []).map((e, idx) => {
  const locationKey = normalizeLocationKey(e.location)
  return {
    id: idx + 1,
    year: `公元${e.year}年`,
    rawYear: e.year,
    name: e.eventName,
    locationText: e.location,
    locationKey,
    desc: e.desc
  }
})

const filteredEvents = computed(() => {
  const key = String(selectedCityKey.value || '').trim()
  if (!key || key === '全国') return historyEvents
  return historyEvents.filter(e => e.locationKey === key)
})

const initMap = () => {
  myChart = markRaw(echarts.init(mapChartRef.value))
  myChart.off('click')
  myChart.on('click', (params) => {
    const name = params?.data?.name
    if (name) selectCity(name)
  })
  updateMap()
}

const updateMap = () => {
  if (!myChart) return

  const coordKeys = new Set(Object.keys(cityCoords).filter(k => k !== '全国'))
  if (activeEvent.value?.locationKey) coordKeys.add(activeEvent.value.locationKey)
  if (activeEvent.value?.locationKey === '全国') coordKeys.add('全国')

  const cityData = Array.from(coordKeys).map(name => {
    const isTarget = activeEvent.value && activeEvent.value.locationKey === name
    return {
      name,
      value: cityCoords[name] || cityCoords['全国'],
      symbolSize: isTarget ? 25 : 12,
      itemStyle: {
        color: isTarget ? '#e6a23c' : '#409EFF',
        shadowBlur: isTarget ? 10 : 0,
        shadowColor: '#e6a23c'
      },
      label: {
        show: true,
        formatter: '{b}',
        position: 'right',
        fontSize: isTarget ? 16 : 12,
        fontWeight: isTarget ? 'bold' : 'normal',
        color: isTarget ? '#e6a23c' : '#333'
      }
    }
  })

  const option = {
    backgroundColor: '#f8f9fa',
    title: {
      text: '三国历史地理示意系图',
      subtext: '点击左侧事件查看发生地',
      left: 'center',
      top: 20
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}'
    },
    xAxis: {
      type: 'value',
      scale: true,
      show: false // 隐藏坐标轴
    },
    yAxis: {
      type: 'value',
      scale: true,
      show: false // 隐藏坐标轴
    },
    series: [
      {
        type: 'scatter',
        data: cityData,
        animationDelay: 300,
        labelLayout: { hideOverlap: true }
      },
      // 涟漪特效动画 (仅对选中的地点生效)
      {
        type: 'effectScatter',
        data: cityData.filter(item => activeEvent.value && activeEvent.value.locationKey === item.name),
        symbolSize: 25,
        labelLayout: { hideOverlap: true },
        showEffectOn: 'render',
        rippleEffect: {
          brushType: 'stroke',
          scale: 4
        },
        itemStyle: {
          color: '#e6a23c'
        },
        zlevel: 1
      }
    ]
  }

  myChart.setOption(option)
}

const selectEvent = (event) => {
  activeEvent.value = event
  updateMap()
}

const selectCity = (cityName) => {
  const name = String(cityName || '').trim()
  if (!name || name === '全国') {
    selectedCityKey.value = ''
    if (historyEvents.length) activeEvent.value = historyEvents[0]
    updateMap()
    return
  }

  selectedCityKey.value = name
  const events = historyEvents
    .filter(e => e.locationKey === name)
    .slice()
    .sort((a, b) => (a.rawYear ?? 0) - (b.rawYear ?? 0))
  if (events.length) activeEvent.value = events[0]
  updateMap()
}

const clearCityFilter = () => {
  selectedCityKey.value = ''
  if (historyEvents.length) activeEvent.value = historyEvents[0]
  updateMap()
}

onMounted(() => {
  nextTick(() => {
    initMap()
    if (historyEvents.length) selectEvent(historyEvents[0])
    if (mapChartRef.value && typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(() => handleResize())
      resizeObserver.observe(mapChartRef.value)
    }
  })
  window.addEventListener('resize', handleResize)
})

const handleResize = () => {
  if (myChart) {
    myChart.resize()
    updateMap()
  }
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (myChart) {
    myChart.dispose()
  }
})
</script>

<style scoped>
.time-space-map-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header-section {
  margin-bottom: 20px;
}

.header-section h2 {
  margin: 0 0 10px 0;
  color: #333;
}

.header-section p {
  color: #666;
  margin: 0;
}

.main-content {
  flex: 1;
  display: flex;
  gap: 20px;
  min-height: 600px;
}

.timeline-panel {
  width: 350px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  padding: 20px;
  overflow-y: auto;
  max-height: 600px;
}

.timeline-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.toolbar-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.city-tag {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timeline-item {
  cursor: pointer;
}

.event-card {
  padding: 12px;
  border-radius: 6px;
  background-color: #f4f4f5;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.event-card:hover {
  background-color: #ecf5ff;
}

.event-card.active {
  background-color: #ecf5ff;
  border-color: #b3d8ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.event-card h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
}

.event-card .location {
  margin: 0 0 8px 0;
  color: #e6a23c;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.event-card .desc {
  margin: 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.map-container {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  min-height: 600px;
}

.map-chart {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 520px;
}

.map-hint {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
  text-align: right;
}
</style>
