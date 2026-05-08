<script setup>
defineProps({
  item: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['select'])
</script>

<template>
  <article class="card" @click="emit('select', item.id)">
    <img v-if="item.coverDisplay" class="cover" :src="item.coverDisplay" :alt="item.title" />
    <span class="cover-id">#{{ item.id }}</span>
    <span class="cover-count">{{ item.photoText }}</span>
    <div class="card-overlay">
      <h3>{{ item.title }}</h3>
    </div>
  </article>
</template>
