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
    <div class="card-body">
      <h3 class="card-title">{{ item.title }}</h3>
      <div class="card-meta">
        <span class="card-author">{{ item.authorText }}</span>
        <span class="card-like">♡ {{ item.likeText }}</span>
      </div>
    </div>
  </article>
</template>
