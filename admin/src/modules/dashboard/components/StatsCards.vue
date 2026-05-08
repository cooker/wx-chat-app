<script setup>
defineProps({
  stats: {
    type: Array,
    required: true
  }
})
</script>

<template>
  <section class="stats-grid">
    <article v-for="stat in stats" :key="stat.label" class="stat-card">
      <p>{{ stat.label }}</p>
      <h3>{{ stat.value }}</h3>
    </article>
  </section>
</template>
