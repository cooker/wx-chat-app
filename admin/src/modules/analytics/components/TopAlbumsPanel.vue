<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    required: true
  }
})

const chartItems = computed(() => {
  const maxPv = Math.max(1, ...props.items.map((item) => Number(item.pv || 0)))
  return props.items.map((item, index) => ({
    ...item,
    rank: index + 1,
    width: Math.max(6, Math.round((Number(item.pv || 0) / maxPv) * 100))
  }))
})
</script>

<template>
  <article class="panel">
    <h2>访问 TOP 10 相册</h2>
    <p v-if="items.length === 0">暂无访问数据</p>
    <div v-else class="top-bars">
      <div v-for="item in chartItems" :key="item.albumId" class="top-row">
        <span class="top-rank">#{{ item.rank }}</span>
        <p class="top-title">{{ item.title }}</p>
        <div class="top-track">
          <div class="top-fill" :style="{ width: `${item.width}%` }"></div>
          <div class="top-meta">
            <p class="top-pv">PV {{ item.pv }} / UV {{ item.uv }}</p>
          </div>
        </div>
      </div>
    </div>
  </article>
</template>
