<script setup>
defineProps({
  bars: {
    type: Array,
    required: true
  }
})
</script>

<template>
  <article class="panel">
    <h2>Revenue Stream</h2>
    <div class="chart">
      <div v-for="(height, index) in bars" :key="index" class="bar" :style="{ height: `${height}%` }"></div>
    </div>
  </article>
</template>
