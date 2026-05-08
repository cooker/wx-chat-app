<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    required: true
  }
})

const palette = ['#30c48d', '#67d7aa', '#9ae5c5', '#5f92ff', '#8e7dff', '#f6b74d']

const chartData = computed(() => {
  const total = props.items.reduce((sum, item) => sum + Number(item.pv || 0), 0)
  if (!total) return []
  let current = 0
  return props.items.map((item, index) => {
    const value = Number(item.pv || 0)
    const ratio = value / total
    const start = current
    const end = current + ratio * 360
    current = end
    return {
      device: item.device || 'unknown',
      pv: value,
      percent: Math.round(ratio * 1000) / 10,
      color: palette[index % palette.length],
      start,
      end
    }
  })
})

const pieStyle = computed(() => {
  if (chartData.value.length === 0) {
    return { background: '#f1f5f9' }
  }
  const slices = chartData.value
    .map((item) => `${item.color} ${item.start}deg ${item.end}deg`)
    .join(', ')
  return { background: `conic-gradient(${slices})` }
})
</script>

<template>
  <article class="panel">
    <h2>今日设备埋点占比（PV）</h2>
    <p v-if="chartData.length === 0">暂无今日设备埋点数据</p>
    <div v-else class="device-pie-wrap">
      <div class="device-pie" :style="pieStyle"></div>
      <ul class="device-legend">
        <li v-for="item in chartData" :key="item.device">
          <span class="legend-dot" :style="{ background: item.color }"></span>
          <span class="legend-name">{{ item.device }}</span>
          <span class="legend-meta">PV {{ item.pv }} · {{ item.percent }}%</span>
        </li>
      </ul>
    </div>
  </article>
</template>
