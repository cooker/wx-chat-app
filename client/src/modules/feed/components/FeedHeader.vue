<script setup>
defineProps({
  title: {
    type: String,
    default: '首页'
  },
  loading: {
    type: Boolean,
    default: false
  },
  refreshing: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['refresh'])
</script>

<template>
  <header class="feed-header">
    <h1>{{ title }}</h1>
    <button @click="emit('refresh')" :disabled="loading || refreshing">
      {{ loading ? '加载中…' : refreshing ? '刷新中…' : '刷新' }}
    </button>
  </header>
</template>
